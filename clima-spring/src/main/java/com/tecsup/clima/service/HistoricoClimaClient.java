package com.tecsup.clima.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.tecsup.clima.config.WeatherProperties;
import com.tecsup.clima.domain.RegistroClimaDiario;

/**
 * Consulta el archivo historico de Open-Meteo y devuelve un registro por dia
 * con temperatura maxima, minima y precipitacion acumulada.
 */
@Component
public class HistoricoClimaClient {

    private static final String VARIABLES_DIARIAS = "temperature_2m_max,temperature_2m_min,precipitation_sum";

    private final RestTemplate restTemplate;
    private final WeatherProperties properties;

    public HistoricoClimaClient(WeatherProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    public List<RegistroClimaDiario> consultar(double latitud, double longitud, LocalDate desde, LocalDate hasta) {
        String url = UriComponentsBuilder.fromUriString(properties.getArchiveUrl())
            .queryParam("latitude", latitud)
            .queryParam("longitude", longitud)
            .queryParam("start_date", desde)
            .queryParam("end_date", hasta)
            .queryParam("daily", VARIABLES_DIARIAS)
            .queryParam("timezone", "America/Lima")
            .build()
            .toUriString();

        JsonNode respuesta = restTemplate.getForObject(url, JsonNode.class);
        return parsear(respuesta);
    }

    /** Convierte el JSON de Open-Meteo en registros. Los valores ausentes quedan null. */
    static List<RegistroClimaDiario> parsear(JsonNode respuesta) {
        List<RegistroClimaDiario> registros = new ArrayList<>();
        if (respuesta == null) {
            return registros;
        }

        JsonNode diario = respuesta.path("daily");
        JsonNode fechas = diario.path("time");
        if (!fechas.isArray()) {
            return registros;
        }

        JsonNode maximas = diario.path("temperature_2m_max");
        JsonNode minimas = diario.path("temperature_2m_min");
        JsonNode lluvias = diario.path("precipitation_sum");

        for (int i = 0; i < fechas.size(); i++) {
            registros.add(new RegistroClimaDiario(
                LocalDate.parse(fechas.get(i).asText()),
                valorOpcional(maximas, i),
                valorOpcional(minimas, i),
                valorOpcional(lluvias, i)
            ));
        }
        return registros;
    }

    private static Double valorOpcional(JsonNode arreglo, int indice) {
        if (!arreglo.isArray() || indice >= arreglo.size() || arreglo.get(indice).isNull()) {
            return null;
        }
        return arreglo.get(indice).asDouble();
    }
}
