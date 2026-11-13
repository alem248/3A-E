package com.tecsup.clima.service;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.tecsup.clima.config.WeatherProperties;
import com.tecsup.clima.domain.historico.RegistroHistoricoDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparacionHistoricaServiceTest {

    @Test
    void calculaDesviacionPorcentualConSigno() {
        WeatherProperties properties = properties();
        LocalDate fecha = LocalDate.now().minusDays(10);
        SerieHistoricaService historico = new StubHistoricoService(properties,
            Optional.of(20.0), Optional.of(new RegistroHistoricoDto(fecha, 25.0, 29.0, 20.0, 0.0)));

        var resultado = new ComparacionHistoricaService(historico, properties).comparar(fecha, null, null);

        assertTrue(resultado.disponible());
        assertEquals(-5.0, resultado.diferenciaAbsolutaC());
        assertEquals(-20.0, resultado.desviacionPorcentual());
    }

    @Test
    void informaDatosInsuficientesSinExcepcion() {
        WeatherProperties properties = properties();
        SerieHistoricaService historico = new StubHistoricoService(properties, Optional.empty(), Optional.empty());
        LocalDate fecha = LocalDate.now().minusDays(10);

        var resultado = new ComparacionHistoricaService(historico, properties).comparar(fecha, null, null);

        assertFalse(resultado.disponible());
        assertNull(resultado.desviacionPorcentual());
        assertTrue(resultado.mensaje().contains("No hay datos suficientes"));
    }

    @Test
    void noCalculaPorcentajeConMediaHistoricaCero() {
        WeatherProperties properties = properties();
        LocalDate fecha = LocalDate.now().minusDays(10);
        SerieHistoricaService historico = new StubHistoricoService(properties, Optional.of(2.0),
            Optional.of(new RegistroHistoricoDto(fecha, 0.0, 1.0, -1.0, 4.0)));

        var resultado = new ComparacionHistoricaService(historico, properties).comparar(fecha, null, null);

        assertTrue(resultado.disponible());
        assertNull(resultado.desviacionPorcentual());
    }

    private WeatherProperties properties() {
        WeatherProperties properties = new WeatherProperties();
        ReflectionTestUtils.setField(properties, "latitude", -4.35);
        ReflectionTestUtils.setField(properties, "longitude", -81.35);
        return properties;
    }

    private static class StubHistoricoService extends SerieHistoricaService {
        private final Optional<Double> actual;
        private final Optional<RegistroHistoricoDto> registro;

        StubHistoricoService(WeatherProperties properties, Optional<Double> actual,
                             Optional<RegistroHistoricoDto> registro) {
            super(properties, new org.springframework.web.client.RestTemplate());
            this.actual = actual;
            this.registro = registro;
        }

        @Override
        public Optional<Double> consultarMediaActual(double latitud, double longitud) {
            return actual;
        }

        @Override
        public Optional<RegistroHistoricoDto> consultarDia(LocalDate fecha, double latitud, double longitud) {
            return registro;
        }
    }
}
