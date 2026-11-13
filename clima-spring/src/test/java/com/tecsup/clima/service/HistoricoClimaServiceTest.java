package com.tecsup.clima.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tecsup.clima.domain.HistoricoClima;
import com.tecsup.clima.domain.RegistroClimaDiario;
import com.tecsup.clima.domain.ResumenClima;

class HistoricoClimaServiceTest {

    private static final double LAT = -4.35;
    private static final double LON = -81.35;
    private static final LocalDate DESDE = LocalDate.of(2023, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2023, 12, 31);

    private final HistoricoClimaClient cliente = mock(HistoricoClimaClient.class);
    private final HistoricoClimaService servicio = new HistoricoClimaService(cliente);

    private static List<RegistroClimaDiario> registros() {
        return List.of(
            new RegistroClimaDiario(LocalDate.of(2023, 1, 5), 30.0, 22.0, 10.0),
            new RegistroClimaDiario(LocalDate.of(2023, 2, 5), 28.0, 20.0, null),
            new RegistroClimaDiario(LocalDate.of(2023, 7, 5), 20.0, 15.0, 0.0),
            new RegistroClimaDiario(LocalDate.of(2023, 3, 5), null, null, 5.0));
    }

    @Test
    void filtraSoloLosMesesPedidos() {
        List<RegistroClimaDiario> filtrados = HistoricoClimaService.filtrarPorMeses(registros(), Set.of(1, 2, 3));

        assertEquals(3, filtrados.size());
        assertTrue(filtrados.stream().allMatch(r -> r.fecha().getMonthValue() <= 3));
    }

    @Test
    void sinMesesNoFiltra() {
        assertEquals(4, HistoricoClimaService.filtrarPorMeses(registros(), null).size());
        assertEquals(4, HistoricoClimaService.filtrarPorMeses(registros(), Set.of()).size());
    }

    @Test
    void resumenPromediaIgnorandoNulos() {
        ResumenClima resumen = HistoricoClimaService.resumir(registros());

        assertEquals(4, resumen.diasConsiderados());
        assertEquals(26.0, resumen.temperaturaMaxPromedioC(), 1e-9);
        assertEquals(19.0, resumen.temperaturaMinPromedioC(), 1e-9);
        assertEquals(15.0, resumen.precipitacionTotalMm(), 1e-9);
    }

    @Test
    void resumenSinDatosDevuelveNulos() {
        ResumenClima resumen = HistoricoClimaService.resumir(
            List.of(new RegistroClimaDiario(LocalDate.of(2023, 1, 1), null, null, null)));

        assertNull(resumen.temperaturaMaxPromedioC());
        assertNull(resumen.precipitacionTotalMm());
    }

    @Test
    void resumenDeListaVaciaNoFalla() {
        assertEquals(0, HistoricoClimaService.resumir(List.of()).diasConsiderados());
    }

    @Test
    void consultarDevuelveRegistrosFiltradosYResumen() {
        when(cliente.consultar(LAT, LON, DESDE, HASTA)).thenReturn(registros());

        HistoricoClima historico = servicio.consultar(LAT, LON, DESDE, HASTA, Set.of(1, 2, 3));

        assertEquals(3, historico.registros().size());
        assertEquals(3, historico.resumen().diasConsiderados());
    }

    @Test
    void rechazaDesdePosteriorAHasta() {
        assertThrows(DatoInvalidoException.class,
            () -> servicio.consultar(LAT, LON, HASTA, DESDE, null));
        verifyNoInteractions(cliente);
    }

    @Test
    void rechazaRangoMayorAlMaximo() {
        LocalDate hastaLejano = DESDE.plusDays(HistoricoClimaService.MAX_DIAS);

        assertThrows(DatoInvalidoException.class,
            () -> servicio.consultar(LAT, LON, DESDE, hastaLejano, null));
        verifyNoInteractions(cliente);
    }

    @Test
    void rechazaMesFueraDeRango() {
        assertThrows(DatoInvalidoException.class,
            () -> servicio.consultar(LAT, LON, DESDE, HASTA, Set.of(13)));
        assertThrows(DatoInvalidoException.class,
            () -> servicio.consultar(LAT, LON, DESDE, HASTA, Set.of(0)));
        verifyNoInteractions(cliente);
    }

    @Test
    void rechazaCoordenadasFueraDeRango() {
        assertThrows(DatoInvalidoException.class,
            () -> servicio.consultar(95.0, LON, DESDE, HASTA, null));
        assertThrows(DatoInvalidoException.class,
            () -> servicio.consultar(LAT, -181.0, DESDE, HASTA, null));
        verifyNoInteractions(cliente);
    }
}
