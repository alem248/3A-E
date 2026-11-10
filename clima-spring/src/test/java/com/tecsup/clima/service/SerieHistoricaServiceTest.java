package com.tecsup.clima.service;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.tecsup.clima.config.WeatherProperties;
import com.tecsup.clima.domain.historico.SerieHistoricaResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerieHistoricaServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapeaRegistrosDiariosYValoresAusentes() throws Exception {
        WeatherProperties properties = properties();
        JsonNode respuesta = mapper.readTree("""
            {"daily":{"time":["2026-10-01","2026-10-02"],
             "temperature_2m_mean":[21.5,null],"temperature_2m_max":[25.0,24.0],
             "temperature_2m_min":[18.0,17.0],"precipitation_sum":[0.0,1.2]}}
            """);
        RestTemplate restTemplate = new StubRestTemplate(respuesta);

        SerieHistoricaResponse resultado = new SerieHistoricaService(properties, restTemplate)
            .consultarSerie(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-02"), null, null);

        assertTrue(resultado.disponible());
        assertEquals(2, resultado.registros().size());
        assertEquals(21.5, resultado.registros().get(0).temperaturaMediaC());
        assertNull(resultado.registros().get(1).temperaturaMediaC());
    }

    @Test
    void conservaRespuestaVaciaSinFallar() throws Exception {
        RestTemplate restTemplate = new StubRestTemplate(mapper.readTree("{\"daily\":{\"time\":[]}}"));

        SerieHistoricaResponse resultado = new SerieHistoricaService(properties(), restTemplate)
            .consultarSerie(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-02"), null, null);

        assertFalse(resultado.disponible());
        assertTrue(resultado.registros().isEmpty());
        assertTrue(resultado.mensaje().contains("no devolvió"));
    }

    @Test
    void rechazaRangoInvertido() {
        SerieHistoricaService servicio = new SerieHistoricaService(properties(), new StubRestTemplate(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarSerie(
            LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-02"), null, null));
    }

    private WeatherProperties properties() {
        WeatherProperties properties = new WeatherProperties();
        ReflectionTestUtils.setField(properties, "archiveUrl", "https://example.test/archive");
        ReflectionTestUtils.setField(properties, "apiUrl", "https://example.test/forecast");
        ReflectionTestUtils.setField(properties, "latitude", -4.35);
        ReflectionTestUtils.setField(properties, "longitude", -81.35);
        return properties;
    }

    private static class StubRestTemplate extends RestTemplate {
        private final JsonNode respuesta;

        StubRestTemplate(JsonNode respuesta) {
            this.respuesta = respuesta;
        }

        @Override
        public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
            return respuesta == null ? null : responseType.cast(respuesta);
        }
    }
}
