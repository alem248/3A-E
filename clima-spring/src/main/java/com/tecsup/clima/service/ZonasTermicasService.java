package com.tecsup.clima.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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

    private static final String API_MARINA_URL = "https://marine-api.open-meteo.com/v1/marine";

    /** Temperatura baseclimatica de la costa peruana usada para la anomalia. */
    private static final double TEMPERATURA_BASE = 24.0;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ZonaVulnerableRepository zonaRepository;
    private final EstacionMonitoreoRepository estacionRepository;
    private final RegistroTsmRepository registroRepository;
    private final NivelRiesgoRepository nivelRepository;
    private final EventoClimaticoRepository eventoRepository;

    public ZonasTermicasService(ZonaVulnerableRepository zonaRepository,
                                EstacionMonitoreoRepository estacionRepository,
                                RegistroTsmRepository registroRepository,
                                NivelRiesgoRepository nivelRepository,
                                EventoClimaticoRepository eventoRepository) {
        this.zonaRepository = zonaRepository;
        this.estacionRepository = estacionRepository;
        this.registroRepository = registroRepository;
        this.nivelRepository = nivelRepository;
        this.eventoRepository = eventoRepository;
    }

    /** Zonas vulnerables con su TSM en tiempo real. */
    @Transactional
    public List<ZonaTermica> consultarTsmPorZonas() {
        return consultar(zonaRepository.findAllByOrderByIdAsc());
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
        String nombre = catalogo != null ? catalogo.getNombre() : String.valueOf(zona.getId());
        String departamento = catalogo != null ? catalogo.getDepartamento() : "Costa norte";

        try {
            EstacionMonitoreoEntity estacion = estacionRepository
                .findById(zona.getId())
                .orElse(null);

            double latitudConsulta = estacion != null ? estacion.getLatitud().doubleValue() : centroide[0];
            double longitudConsulta = estacion != null ? estacion.getLongitud().doubleValue() : centroide[1];

            String url = String.format(
                "%s?latitude=%s&longitude=%s&current=sea_surface_temperature",
                API_MARINA_URL, latitudConsulta, longitudConsulta);

            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            JsonNode current = root.path("current");

            Double tsm = current.path("sea_surface_temperature").asDouble(Double.NaN);
            String actualizadoEn = current.path("time").asText(null);

            if (tsm.isNaN()) {
                return sinDatos(zona, poligono, nombre, departamento, centroide, actualizadoEn);
            }

            NivelRiesgo nivel = NivelRiesgo.clasificarTemperatura(tsm);

            clasificar(zona, nivel);
            registrarLectura(zona.getId(), centroide, tsm);

            return new ZonaTermica(String.valueOf(zona.getId()), nombre, departamento,
                centroide[0], centroide[1], poligono,
                tsm, actualizadoEn, nivel, true);
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
    private void registrarLectura(int zonaId, double[] centroide, double tsm) {
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
        registroRepository.save(registro);
    }

    private ZonaTermica sinDatos(ZonaVulnerableEntity zona, List<List<Double>> poligono) {
        double[] centroide = PoligonoUtils.centroide(poligono);
        ZonasTermicas.Zona catalogo = ZonasTermicas.getZona(zona.getId());

        String nombre = catalogo != null ? catalogo.getNombre() : String.valueOf(zona.getId());
        String departamento = catalogo != null ? catalogo.getDepartamento() : "Costa norte";

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