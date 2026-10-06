package com.tecsup.clima.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tecsup.clima.domain.NivelRiesgo;
import com.tecsup.clima.domain.ZonaTermica;
import com.tecsup.clima.domain.ZonasTermicas;

import java.util.ArrayList;
import java.util.List;

@Service
public class ZonasTermicasService {

    private static final String API_MARINA_URL = "https://marine-api.open-meteo.com/v1/marine";

    private final RestTemplate restTemplate;

    public ZonasTermicasService() {
        this.restTemplate = new RestTemplate();
    }

    public List<ZonaTermica> consultarTsmPorZonas() {
        List<ZonaTermica> zonas = ZonasTermicas.getZonas();
        List<ZonaTermica> resultado = new ArrayList<>();

        for (ZonaTermica zona : zonas) {
            resultado.add(consultarZona(zona));
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

            return new ZonaTermica(
                zona.getId(), zona.getNombre(), zona.getDepartamento(),
                zona.getLatitude(), zona.getLongitude(), zona.getPoligono(),
                tsm, actualizadoEn, NivelRiesgo.clasificarTemperatura(tsm), true
            );
        } catch (Exception error) {
            return sinDatos(zona, null);
        }
    }

    private ZonaTermica sinDatos(ZonaTermica zona, String actualizadoEn) {
        return new ZonaTermica(
            zona.getId(), zona.getNombre(), zona.getDepartamento(),
            zona.getLatitude(), zona.getLongitude(), zona.getPoligono(),
            null, actualizadoEn, NivelRiesgo.SIN_DATOS, false
        );
    }
}