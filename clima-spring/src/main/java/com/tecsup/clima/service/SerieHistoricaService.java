package com.tecsup.clima.service;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.tecsup.clima.config.WeatherProperties;
import com.tecsup.clima.domain.historico.RegistroHistoricoDto;
import com.tecsup.clima.domain.historico.SerieHistoricaResponse;

/** Consulta series diarias históricas y el promedio diario estimado actual. */
@Service
public class SerieHistoricaService {

    private static final int MAX_DIAS = 366;
    private static final String VARIABLES_DIARIAS =
        "temperature_2m_mean,temperature_2m_max,temperature_2m_min,precipitation_sum";

    private final WeatherProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public SerieHistoricaService(WeatherProperties properties) {
        this(properties, new RestTemplate());
    }

    public SerieHistoricaService(WeatherProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public SerieHistoricaResponse consultarSerie(LocalDate desde, LocalDate hasta,
                                                  Double latitud, Double longitud) {
        validarRango(desde, hasta);
        double lat = latitud == null ? properties.getLatitude() : latitud;
        double lon = longitud == null ? properties.getLongitude() : longitud;
        validarCoordenadas(lat, lon);

        try {
            JsonNode root = consultar(properties.getArchiveUrl(), desde, hasta, lat, lon);
            List<RegistroHistoricoDto> registros = mapear(root, desde, hasta);
            String mensaje = registros.isEmpty()
                ? "La fuente no devolvió registros para el rango seleccionado."
                : "Se cargaron " + registros.size() + " días.";
            return new SerieHistoricaResponse(desde, hasta, lat, lon, registros,
                !registros.isEmpty(), mensaje);
        } catch (RestClientException | IllegalStateException | DateTimeException error) {
            return new SerieHistoricaResponse(desde, hasta, lat, lon, List.of(), false,
                "No se pudieron cargar los datos históricos. Intenta nuevamente más tarde.");
        }
    }

    public Optional<RegistroHistoricoDto> consultarDia(LocalDate fecha, double latitud, double longitud) {
        SerieHistoricaResponse serie = consultarSerie(fecha, fecha, latitud, longitud);
        return serie.registros().stream().findFirst();
    }

    /** Media diaria estimada para hoy, consultada en la API de pronóstico. */
    public Optional<Double> consultarMediaActual(double latitud, double longitud) {
        validarCoordenadas(latitud, longitud);
        try {
            String url = UriComponentsBuilder.fromUriString(properties.getApiUrl())
                .queryParam("latitude", latitud)
                .queryParam("longitude", longitud)
                .queryParam("daily", "temperature_2m_mean")
                .queryParam("forecast_days", 1)
                .queryParam("timezone", "auto")
                .build().encode().toUriString();
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            JsonNode valores = root == null ? null : root.path("daily").path("temperature_2m_mean");
            return valor(valores, 0);
        } catch (RestClientException error) {
            return Optional.empty();
        }
    }

    private JsonNode consultar(String baseUrl, LocalDate desde, LocalDate hasta,
                               double latitud, double longitud) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("latitude", latitud)
            .queryParam("longitude", longitud)
            .queryParam("start_date", desde)
            .queryParam("end_date", hasta)
            .queryParam("daily", VARIABLES_DIARIAS)
            .queryParam("timezone", "auto")
            .build().encode().toUriString();
        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        if (root == null || !root.has("daily")) {
            throw new IllegalStateException("La fuente no devolvió una serie diaria.");
        }
        return root;
    }

    private List<RegistroHistoricoDto> mapear(JsonNode root, LocalDate desde, LocalDate hasta) {
        JsonNode daily = root.path("daily");
        JsonNode fechas = daily.path("time");
        List<RegistroHistoricoDto> registros = new ArrayList<>();
        for (int i = 0; i < fechas.size(); i++) {
            LocalDate fecha = LocalDate.parse(fechas.get(i).asText());
            if (fecha.isBefore(desde) || fecha.isAfter(hasta)) {
                continue;
            }
            registros.add(new RegistroHistoricoDto(
                fecha,
                valor(daily.path("temperature_2m_mean"), i).orElse(null),
                valor(daily.path("temperature_2m_max"), i).orElse(null),
                valor(daily.path("temperature_2m_min"), i).orElse(null),
                valor(daily.path("precipitation_sum"), i).orElse(null)));
        }
        return registros;
    }

    private Optional<Double> valor(JsonNode valores, int indice) {
        if (valores == null || !valores.isArray() || indice >= valores.size()
                || valores.get(indice).isNull() || !valores.get(indice).isNumber()) {
            return Optional.empty();
        }
        double valor = valores.get(indice).asDouble();
        return Double.isFinite(valor) ? Optional.of(valor) : Optional.empty();
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango de fechas no es válido.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > MAX_DIAS) {
            throw new IllegalArgumentException("El rango máximo de consulta es de 366 días.");
        }
        if (hasta.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha final no puede ser futura.");
        }
    }

    private void validarCoordenadas(double latitud, double longitud) {
        if (!Double.isFinite(latitud) || latitud < -90 || latitud > 90
                || !Double.isFinite(longitud) || longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException("Las coordenadas no son válidas.");
        }
    }
}
