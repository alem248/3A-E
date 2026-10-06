package com.tecsup.clima.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tecsup.clima.domain.NivelRiesgo;
import com.tecsup.clima.domain.ZonaTermica;
import com.tecsup.clima.persistence.LecturaTsmEntity;
import com.tecsup.clima.persistence.LecturaTsmRepository;
import com.tecsup.clima.persistence.ZonaTermicaEntity;
import com.tecsup.clima.persistence.ZonaTermicaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ZonasTermicasService {

    private static final String API_MARINA_URL = "https://marine-api.open-meteo.com/v1/marine";

    private final RestTemplate restTemplate = new RestTemplate();

    private final ZonaTermicaRepository zonaRepository;
    private final LecturaTsmRepository lecturaRepository;

    public ZonasTermicasService(ZonaTermicaRepository zonaRepository, LecturaTsmRepository lecturaRepository) {
        this.zonaRepository = zonaRepository;
        this.lecturaRepository = lecturaRepository;
    }

    /**
     * Consulta la TSM de todas las zonas persistidas en la base de datos.
     */
    public List<ZonaTermica> consultarTsmPorZonas() {
        List<ZonaTermica> resultado = new ArrayList<>();

        for (ZonaTermicaEntity zona : zonaRepository.findAllByOrderByOrdenAsc()) {
            resultado.add(consultarZona(aBase(zona)));
        }

        return resultado;
    }

    /**
     * Consulta la TSM solo de las zonas cuyo poligono contiene el punto dado.
     * Permite filtrar por zona geografica (HU-1: filtrar por zona geografica).
     */
    public List<ZonaTermica> consultarTsmPorZonas(double latitud, double longitud) {
        List<ZonaTermica> resultado = new ArrayList<>();

        for (ZonaTermicaEntity zona : zonaRepository.findAllByOrderByOrdenAsc()) {
            ZonaTermica base = aBase(zona);

            if (base.contiene(latitud, longitud)) {
                resultado.add(consultarZona(base));
            }
        }

        return resultado;
    }

    private ZonaTermica consultarZona(ZonaTermica zona) {
        try {
            String url = String.format(
                "%s?latitude=%s&longitude=%s&current=sea_surface_temperature",
                API_MARINA_URL,
                zona.getLatitude(),
                zona.getLongitude()
            );

            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            JsonNode current = root.path("current");

            Double tsm = current.path("sea_surface_temperature").asDouble(Double.NaN);
            String actualizadoEn = current.path("time").asText(null);

            if (tsm.isNaN()) {
                return sinDatos(zona, actualizadoEn);
            }

            ZonaTermica lectura = new ZonaTermica(
                zona.getId(), zona.getNombre(), zona.getDepartamento(),
                zona.getLatitude(), zona.getLongitude(), zona.getPoligono(),
                tsm, actualizadoEn, NivelRiesgo.clasificarTemperatura(tsm), true
            );

            registrarLectura(zona.getId(), lectura);

            return lectura;
        } catch (Exception error) {
            return sinDatos(zona, null);
        }
    }

    private void registrarLectura(String zonaId, ZonaTermica lectura) {
        zonaRepository.findById(zonaId).ifPresent(zona -> {
            LecturaTsmEntity registro = new LecturaTsmEntity();
            registro.setZona(zona);
            registro.setTsm(lectura.getTsm());
            registro.setNivelRiesgo(lectura.getNivel().getId());
            registro.setActualizadoEn(lectura.getActualizadoEn());
            registro.setRegistradoEn(LocalDateTime.now());
            lecturaRepository.save(registro);
        });
    }

    private ZonaTermica sinDatos(ZonaTermica zona, String actualizadoEn) {
        return new ZonaTermica(
            zona.getId(), zona.getNombre(), zona.getDepartamento(),
            zona.getLatitude(), zona.getLongitude(), zona.getPoligono(),
            null, actualizadoEn, NivelRiesgo.SIN_DATOS, false
        );
    }

    /**
     * Convierte una entidad persistida en el modelo de dominio, reconstruyendo
     * el poligono a partir de los vertices almacenados en la base de datos.
     */
    private ZonaTermica aBase(ZonaTermicaEntity entidad) {
        List<List<Double>> poligono = new ArrayList<>();

        entidad.getPoligono().forEach(vertice ->
            poligono.add(List.of(vertice.getLatitud(), vertice.getLongitud()))
        );

        return new ZonaTermica(
            entidad.getId(), entidad.getNombre(), entidad.getDepartamento(),
            entidad.getLatitud(), entidad.getLongitud(), poligono,
            null, null, NivelRiesgo.SIN_DATOS, false
        );
    }
}
