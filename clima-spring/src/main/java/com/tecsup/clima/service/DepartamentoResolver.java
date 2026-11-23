package com.tecsup.clima.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Convierte una coordenada GPS en el departamento del Peru usando Nominatim (OpenStreetMap).
 * Nominatim pide un User-Agent identificable y no mas de una consulta por segundo.
 */
@Component
public class DepartamentoResolver {

    private static final Logger log = LoggerFactory.getLogger(DepartamentoResolver.class);

    private static final String URL_REVERSO = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT =
        "ClimaZonaCostera/1.0 (proyecto academico; eder.yaycate@tecsup.edu.pe)";

    private final RestTemplate restTemplate;

    public DepartamentoResolver() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(8));
        this.restTemplate = new RestTemplate(fabrica);
    }

    /** Devuelve el departamento, o vacio si el servicio no responde o no lo reconoce. */
    public Optional<String> resolver(double latitud, double longitud) {
        return direccion(latitud, longitud, 5, "state", "region");
    }

    /** Devuelve el distrito (por ejemplo Santa Anita o Lurigancho), o vacio si no se reconoce. */
    public Optional<String> resolverDistrito(double latitud, double longitud) {
        return direccion(latitud, longitud, 14, "city_district", "district", "suburb", "town", "village", "municipality");
    }

    private Optional<String> direccion(double latitud, double longitud, int zoom, String... claves) {
        try {
            String url = UriComponentsBuilder.fromUriString(URL_REVERSO)
                .queryParam("format", "json")
                .queryParam("lat", latitud)
                .queryParam("lon", longitud)
                .queryParam("zoom", zoom)
                .queryParam("accept-language", "es")
                .build()
                .toUriString();

            HttpHeaders cabeceras = new HttpHeaders();
            cabeceras.set(HttpHeaders.USER_AGENT, USER_AGENT);
            ResponseEntity<Map> respuesta = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(cabeceras), Map.class);

            Map<?, ?> cuerpo = respuesta.getBody();
            if (cuerpo == null || !(cuerpo.get("address") instanceof Map<?, ?> direccion)) {
                log.warn("Nominatim no devolvio 'address' para {}, {}: {}", latitud, longitud, cuerpo);
                return Optional.empty();
            }
            log.info("Nominatim address para {}, {}: {}", latitud, longitud, direccion);
            for (String clave : claves) {
                if (direccion.get(clave) instanceof String texto && !texto.isBlank()) {
                    return Optional.of(texto.trim());
                }
            }
            return Optional.empty();
        } catch (RestClientException | ClassCastException error) {
            log.warn("No se pudo resolver la direccion para {}, {}: {}", latitud, longitud, error.toString());
            return Optional.empty();
        }
    }
}
