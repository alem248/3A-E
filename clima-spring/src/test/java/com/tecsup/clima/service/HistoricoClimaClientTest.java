package com.tecsup.clima.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.clima.domain.RegistroClimaDiario;

class HistoricoClimaClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parseaCadaDiaConSusValores() throws Exception {
        String json = """
            {"daily": {
                "time": ["2023-01-01", "2023-01-02"],
                "temperature_2m_max": [30.5, 29.0],
                "temperature_2m_min": [22.1, 21.4],
                "precipitation_sum": [0.0, 12.3]
            }}
            """;

        List<RegistroClimaDiario> registros = HistoricoClimaClient.parsear(mapper.readTree(json));

        assertEquals(2, registros.size());
        assertEquals(LocalDate.of(2023, 1, 2), registros.get(1).fecha());
        assertEquals(29.0, registros.get(1).temperaturaMaxC(), 1e-9);
        assertEquals(21.4, registros.get(1).temperaturaMinC(), 1e-9);
        assertEquals(12.3, registros.get(1).precipitacionMm(), 1e-9);
    }

    @Test
    void valoresNullDeLaFuenteQuedanNull() throws Exception {
        String json = """
            {"daily": {
                "time": ["2023-01-01"],
                "temperature_2m_max": [null],
                "temperature_2m_min": [21.0],
                "precipitation_sum": [null]
            }}
            """;

        RegistroClimaDiario registro = HistoricoClimaClient.parsear(mapper.readTree(json)).get(0);

        assertNull(registro.temperaturaMaxC());
        assertEquals(21.0, registro.temperaturaMinC(), 1e-9);
        assertNull(registro.precipitacionMm());
    }

    @Test
    void arreglosMasCortosQueLasFechasDejanNull() throws Exception {
        String json = """
            {"daily": {
                "time": ["2023-01-01", "2023-01-02"],
                "temperature_2m_max": [30.0]
            }}
            """;

        List<RegistroClimaDiario> registros = HistoricoClimaClient.parsear(mapper.readTree(json));

        assertEquals(2, registros.size());
        assertNull(registros.get(1).temperaturaMaxC());
        assertNull(registros.get(1).precipitacionMm());
    }

    @Test
    void respuestaSinDailyDevuelveListaVacia() throws Exception {
        assertTrue(HistoricoClimaClient.parsear(mapper.readTree("{}")).isEmpty());
    }

    @Test
    void respuestaNulaDevuelveListaVacia() {
        assertTrue(HistoricoClimaClient.parsear(null).isEmpty());
    }
}
