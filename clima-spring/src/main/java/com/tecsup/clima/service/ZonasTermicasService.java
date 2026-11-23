package com.tecsup.clima.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import com.tecsup.clima.domain.NivelRiesgo;
import com.tecsup.clima.domain.PoligonoUtils;
import com.tecsup.clima.domain.ZonaTermica;
import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.EstacionMonitoreoEntity;
import com.tecsup.clima.persistence.EstacionMonitoreoRepository;
import com.tecsup.clima.persistence.EventoClimaticoEntity;
import com.tecsup.clima.persistence.EventoClimaticoRepository;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.NivelRiesgoRepository;
import com.tecsup.clima.persistence.RegistroTsmEntity;
import com.tecsup.clima.persistence.RegistroTsmRepository;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

/**
 * Consulta la TSM en tiempo real de las zonas vulnerables (DER) y persiste el
 * registro en {@code registro_tsm}, clasificando la zona por nivel de riesgo.
 */
@Service
public class ZonasTermicasService {

    /** Temperatura baseclimatica de la costa peruana usada para la anomalia. */
    private static final double TEMPERATURA_BASE = 24.0;

    private static final String API_CLIMA = "https://api.open-meteo.com/v1/forecast";

    private final RestTemplate restTemplate = new RestTemplate();

    private final ZonaVulnerableRepository zonaRepository;
    private final EstacionMonitoreoRepository estacionRepository;
    private final RegistroTsmRepository registroRepository;
    private final NivelRiesgoRepository nivelRepository;
    private final EventoClimaticoRepository eventoRepository;
    private final String apiMarinaUrl;

    public ZonasTermicasService(ZonaVulnerableRepository zonaRepository,
                                EstacionMonitoreoRepository estacionRepository,
                                RegistroTsmRepository registroRepository,
                                NivelRiesgoRepository nivelRepository,
                                EventoClimaticoRepository eventoRepository,
                                @Value("${weather.marine.url:https://marine-api.open-meteo.com/v1/marine}") String apiMarinaUrl) {
        this.zonaRepository = zonaRepository;
        this.estacionRepository = estacionRepository;
        this.registroRepository = registroRepository;
        this.nivelRepository = nivelRepository;
        this.eventoRepository = eventoRepository;
        this.apiMarinaUrl = apiMarinaUrl;
    }

    /** Zonas vulnerables con su TSM en tiempo real. */
    @Transactional
    public synchronized List<ZonaTermica> consultarTsmPorZonas() {
        if (cacheZonas != null && Instant.now().isBefore(cacheZonasHasta)) {
            return cacheZonas;
        }
        List<ZonaTermica> resultado = consultar(sinDuplicadosPorNombre(zonaRepository.findAllByOrderByIdAsc()));
        cacheZonas = resultado;
        cacheZonasHasta = Instant.now().plus(Duration.ofMinutes(10));
        return resultado;
    }

    private List<ZonaTermica> cacheZonas;
    private Instant cacheZonasHasta = Instant.EPOCH;

    /** Consulta y registra la lectura TSM de una sola zona. */
    @Transactional
    public ZonaTermica consultarTsmZona(int zonaId) {
        ZonaVulnerableEntity zona = zonaRepository.findById(zonaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("La zona " + zonaId + " no existe"));
        return consultarZona(zona);
    }

    /** Filtro por zona geografica: solo las zonas que contienen el punto. */
    @Transactional
    public List<ZonaTermica> consultarTsmPorZonas(double latitud, double longitud) {
        List<ZonaVulnerableEntity> filtradas = new ArrayList<>();

        for (ZonaVulnerableEntity zona : zonaRepository.findAllByOrderByIdAsc()) {
            if (PoligonoUtils.contiene(PoligonoUtils.desdeWkt(zona.getPoligono()), latitud, longitud)) {
                filtradas.add(zona);
            }
        }

        return consultar(filtradas);
    }

    /** Zona que ya cubre la ubicacion o que tiene el mismo nombre: evita duplicados. */
    public Optional<ZonaVulnerableEntity> buscarZonaExistente(String nombre, String departamento,
                                                             double latitud, double longitud) {
        return zonaRepository.findAllByOrderByIdAsc().stream()
            .filter(zona -> (zona.getNombre() != null && zona.getNombre().equals(nombre)
                    && Objects.equals(zona.getDepartamento(), departamento))
                || PoligonoUtils.contiene(PoligonoUtils.desdeWkt(zona.getPoligono()), latitud, longitud))
            .findFirst();
    }

    /** Oculta zonas repetidas (mismo nombre y departamento) en la vista. */
    private List<ZonaVulnerableEntity> sinDuplicadosPorNombre(List<ZonaVulnerableEntity> zonas) {
        Set<String> vistos = new HashSet<>();
        List<ZonaVulnerableEntity> resultado = new ArrayList<>();
        for (ZonaVulnerableEntity zona : zonas) {
            String clave = zona.getNombre() != null
                ? zona.getNombre() + "|" + zona.getDepartamento()
                : "id:" + zona.getId();
            if (vistos.add(clave)) {
                resultado.add(zona);
            }
        }
        return resultado;
    }

    /**
     * Crea una zona que cubre la ubicacion y la costa con mar mas cercana, con su
     * estacion marina para que el sistema la monitoree como las demas zonas.
     */
    @Transactional
    public ZonaVulnerableEntity crearZonaParaUbicacion(String nombre, String departamento,
                                                      double latitud, double longitud) {
        boolean continental = ZonasTermicas.esContinental(departamento);
        double[] costa = continental ? new double[] {latitud, longitud} : buscarCostaConMar(latitud, longitud);
        if (costa == null) {
            throw new IllegalStateException("No se encontró mar cercano a esta ubicación.");
        }

        int nuevoId = zonaRepository.findAllByOrderByIdAsc().stream()
            .mapToInt(ZonaVulnerableEntity::getId).max().orElse(0) + 1;

        EstacionMonitoreoEntity referencia = estacionRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No hay fuente oficial de datos configurada."));
        EstacionMonitoreoEntity estacion = new EstacionMonitoreoEntity();
        estacion.setId(nuevoId);
        estacion.setFuente(referencia.getFuente());
        estacion.setTipo(continental ? "CONTINENTAL" : "MARINA");
        estacion.setLatitud(BigDecimal.valueOf(costa[0]));
        estacion.setLongitud(BigDecimal.valueOf(costa[1]));
        estacionRepository.save(estacion);

        double margen = 0.05;
        double latMin = Math.min(latitud, costa[0]) - margen;
        double latMax = Math.max(latitud, costa[0]) + margen;
        double lonMin = Math.min(longitud, costa[1]) - margen;
        double lonMax = Math.max(longitud, costa[1]) + margen;
        List<List<Double>> poligono = List.of(
            List.of(latMin, lonMin), List.of(latMin, lonMax),
            List.of(latMax, lonMax), List.of(latMax, lonMin));

        ZonaVulnerableEntity zona = new ZonaVulnerableEntity();
        zona.setId(nuevoId);
        zona.setNombre(nombre);
        zona.setDepartamento(departamento);
        zona.setPoligono(PoligonoUtils.aWkt(poligono));
        zona.setNivelRiesgo(nivelRepository.findByNombre(NivelRiesgo.SIN_DATOS.getId()).orElse(null));
        return zonaRepository.save(zona);
    }

    /** Avanza hacia el oeste hasta encontrar un punto donde la API marina entrega temperatura del mar. */
    private double[] buscarCostaConMar(double latitud, double longitud) {
        for (int paso = 0; paso <= 60; paso++) {
            double lon = longitud - paso * 0.05;
            if (temperaturaMarina(latitud, lon) != null) {
                return new double[] {latitud, lon};
            }
        }
        return null;
    }

    private Double temperaturaMarina(double latitud, double longitud) {
        try {
            String url = String.format("%s?latitude=%s&longitude=%s&current=sea_surface_temperature",
                apiMarinaUrl, String.valueOf(latitud), String.valueOf(longitud));
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            double valor = root.path("current").path("sea_surface_temperature").asDouble(Double.NaN);
            return Double.isNaN(valor) ? null : valor;
        } catch (Exception error) {
            return null;
        }
    }

    private List<ZonaTermica> consultar(List<ZonaVulnerableEntity> zonas) {
        List<ZonaTermica> resultado = new ArrayList<>();

        for (ZonaVulnerableEntity zona : zonas) {
            resultado.add(consultarZona(zona));
        }

        return resultado;
    }

    private ZonaTermica consultarZona(ZonaVulnerableEntity zona) {
        List<List<Double>> poligono = PoligonoUtils.desdeWkt(zona.getPoligono());
        double[] centroide = PoligonoUtils.centroide(poligono);

        if (centroide == null) {
            return sinDatos(zona, poligono);
        }

        ZonasTermicas.Zona catalogo = ZonasTermicas.getZona(zona.getId());
        String nombre = catalogo != null ? catalogo.getNombre()
            : (zona.getNombre() != null ? zona.getNombre() : String.valueOf(zona.getId()));
        String departamento = catalogo != null ? catalogo.getDepartamento()
            : (zona.getDepartamento() != null ? zona.getDepartamento() : "Costa norte");

        try {
            EstacionMonitoreoEntity estacion = estacionRepository
                .findById(zona.getId())
                .orElse(null);

            double latitudConsulta = estacion != null ? estacion.getLatitud().doubleValue() : centroide[0];
            double longitudConsulta = estacion != null ? estacion.getLongitud().doubleValue() : centroide[1];

            if (estacion != null && "CONTINENTAL".equals(estacion.getTipo())) {
                return consultarZonaContinental(zona, poligono, centroide, nombre, departamento,
                    latitudConsulta, longitudConsulta);
            }

            String url = String.format(
                "%s?latitude=%s&longitude=%s&current=sea_surface_temperature",
                apiMarinaUrl, latitudConsulta, longitudConsulta);

            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            JsonNode current = root.path("current");

            Double tsm = current.path("sea_surface_temperature").asDouble(Double.NaN);
            String actualizadoEn = current.path("time").asText(null);

            if (tsm.isNaN()) {
                return sinDatos(zona, poligono, nombre, departamento, centroide, actualizadoEn);
            }

            NivelRiesgo nivel = NivelRiesgo.clasificarTemperatura(tsm);

            clasificar(zona, nivel);
            if (actualizadoEn != null) {
                registrarLectura(zona.getId(), centroide, tsm, LocalDateTime.parse(actualizadoEn));
            }

            return new ZonaTermica(String.valueOf(zona.getId()), nombre, departamento,
                centroide[0], centroide[1], poligono,
                tsm, actualizadoEn, nivel, true);
        } catch (Exception error) {
            return sinDatos(zona, poligono, nombre, departamento, centroide, null);
        }
    }

    /** Zona del interior: nivel por calor (temperatura del aire) y lluvia. */
    private ZonaTermica consultarZonaContinental(ZonaVulnerableEntity zona, List<List<Double>> poligono,
                                                 double[] centroide, String nombre, String departamento,
                                                 double latitud, double longitud) {
        try {
            String url = String.format("%s?latitude=%s&longitude=%s&current=temperature_2m,precipitation",
                API_CLIMA, String.valueOf(latitud), String.valueOf(longitud));
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            JsonNode current = root.path("current");

            Double temperatura = current.path("temperature_2m").asDouble(Double.NaN) * 1.0;
            Double lluvia = current.path("precipitation").asDouble(Double.NaN) * 1.0;
            String actualizadoEn = current.path("time").asText(null);

            if (temperatura.isNaN()) {
                return sinDatos(zona, poligono, nombre, departamento, centroide, actualizadoEn);
            }

            NivelRiesgo nivel = NivelRiesgo.maximo(
                NivelRiesgo.clasificarCalor(temperatura), NivelRiesgo.clasificarLluvia(lluvia.isNaN() ? null : lluvia));

            clasificar(zona, nivel);
            if (actualizadoEn != null) {
                registrarLectura(zona.getId(), centroide, temperatura, LocalDateTime.parse(actualizadoEn));
            }

            return new ZonaTermica(String.valueOf(zona.getId()), nombre, departamento,
                centroide[0], centroide[1], poligono,
                temperatura, actualizadoEn, nivel, true);
        } catch (Exception error) {
            return sinDatos(zona, poligono, nombre, departamento, centroide, null);
        }
    }

    /** DER: NIVEL_RIESGO clasifica ZONA_VULNERABLE. */
    private void clasificar(ZonaVulnerableEntity zona, NivelRiesgo nivel) {
        Optional<NivelRiesgoEntity> entidad = nivelRepository.findByNombre(nivel.getId());

        entidad.ifPresent(valor -> {
            zona.setNivelRiesgo(valor);
            zonaRepository.save(zona);
        });
    }

    /** DER: REGISTRO_TSM contiene la lectura medida por la estacion. */
    private void registrarLectura(int zonaId, double[] centroide, double tsm, LocalDateTime observadaEn) {
        Optional<EventoClimaticoEntity> evento = eventoRepository.findFirstByEstadoIgnoreCase("ACTIVO");
        Optional<EstacionMonitoreoEntity> estacion = estacionRepository.findById(zonaId);

        if (evento.isEmpty() || estacion.isEmpty()) {
            return;
        }

        RegistroTsmEntity registro = new RegistroTsmEntity();
        registro.setEvento(evento.get());
        registro.setEstacion(estacion.get());
        registro.setLatitud(BigDecimal.valueOf(centroide[0]));
        registro.setLongitud(BigDecimal.valueOf(centroide[1]));
        registro.setAnomalia(BigDecimal.valueOf(tsm - TEMPERATURA_BASE).setScale(2, java.math.RoundingMode.HALF_UP));
        registro.setFechaObservacion(observadaEn);
        registroRepository.save(registro);
    }

    private ZonaTermica sinDatos(ZonaVulnerableEntity zona, List<List<Double>> poligono) {
        double[] centroide = PoligonoUtils.centroide(poligono);
        ZonasTermicas.Zona catalogo = ZonasTermicas.getZona(zona.getId());

        String nombre = catalogo != null ? catalogo.getNombre()
            : (zona.getNombre() != null ? zona.getNombre() : String.valueOf(zona.getId()));
        String departamento = catalogo != null ? catalogo.getDepartamento()
            : (zona.getDepartamento() != null ? zona.getDepartamento() : "Costa norte");

        return sinDatos(zona, poligono, nombre, departamento, centroide, null);
    }

    private ZonaTermica sinDatos(ZonaVulnerableEntity zona, List<List<Double>> poligono,
                                 String nombre, String departamento, double[] centroide,
                                 String actualizadoEn) {
        return new ZonaTermica(String.valueOf(zona.getId()), nombre, departamento,
            centroide != null ? centroide[0] : null,
            centroide != null ? centroide[1] : null,
            poligono, null, actualizadoEn, NivelRiesgo.SIN_DATOS, false);
    }
}
