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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComparacionHistoricaServiceTest {

    @Test
    void calculaDesviacionPorcentualConSigno() {
        HistoricoClimaService historico = mock(HistoricoClimaService.class);
        WeatherProperties properties = properties();
        LocalDate fecha = LocalDate.now().minusDays(10);
        when(historico.consultarMediaActual(-4.35, -81.35)).thenReturn(Optional.of(20.0));
        when(historico.consultarDia(fecha, -4.35, -81.35)).thenReturn(Optional.of(
            new RegistroHistoricoDto(fecha, 25.0, 29.0, 20.0, 0.0)));

        var resultado = new ComparacionHistoricaService(historico, properties).comparar(fecha, null, null);

        assertTrue(resultado.disponible());
        assertEquals(-5.0, resultado.diferenciaAbsolutaC());
        assertEquals(-20.0, resultado.desviacionPorcentual());
    }

    @Test
    void informaDatosInsuficientesSinExcepcion() {
        HistoricoClimaService historico = mock(HistoricoClimaService.class);
        when(historico.consultarMediaActual(-4.35, -81.35)).thenReturn(Optional.empty());
        LocalDate fecha = LocalDate.now().minusDays(10);

        var resultado = new ComparacionHistoricaService(historico, properties()).comparar(fecha, null, null);

        assertFalse(resultado.disponible());
        assertNull(resultado.desviacionPorcentual());
        assertTrue(resultado.mensaje().contains("No hay datos suficientes"));
    }

    @Test
    void noCalculaPorcentajeConMediaHistoricaCero() {
        HistoricoClimaService historico = mock(HistoricoClimaService.class);
        LocalDate fecha = LocalDate.now().minusDays(10);
        when(historico.consultarMediaActual(-4.35, -81.35)).thenReturn(Optional.of(2.0));
        when(historico.consultarDia(fecha, -4.35, -81.35)).thenReturn(Optional.of(
            new RegistroHistoricoDto(fecha, 0.0, 1.0, -1.0, 4.0)));

        var resultado = new ComparacionHistoricaService(historico, properties()).comparar(fecha, null, null);

        assertTrue(resultado.disponible());
        assertNull(resultado.desviacionPorcentual());
    }

    private WeatherProperties properties() {
        WeatherProperties properties = new WeatherProperties();
        ReflectionTestUtils.setField(properties, "latitude", -4.35);
        ReflectionTestUtils.setField(properties, "longitude", -81.35);
        return properties;
    }
}
