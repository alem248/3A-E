package com.tecsup.clima.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tecsup.clima.config.ReglasAlertaProperties;
import com.tecsup.clima.domain.LecturaZona;
import com.tecsup.clima.domain.NivelRiesgo;
import com.tecsup.clima.domain.ReglaAlerta;
import com.tecsup.clima.persistence.AlertaPreventivaEntity;
import com.tecsup.clima.persistence.AlertaPreventivaRepository;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.NivelRiesgoRepository;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

class EvaluacionAlertasServiceTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 10, 27, 15, 0, 0, 0, ZoneOffset.UTC);

    private final ZonasTermicasService zonasService = mock(ZonasTermicasService.class);
    private final ZonaVulnerableRepository zonaRepository = mock(ZonaVulnerableRepository.class);
    private final NivelRiesgoRepository nivelRepository = mock(NivelRiesgoRepository.class);
    private final AlertaPreventivaRepository alertaRepository = mock(AlertaPreventivaRepository.class);

    private final EvaluacionAlertasService service = new EvaluacionAlertasService(
        new ReglasAlertaProperties(false, List.of(
            new ReglaAlerta("Piura rojo", 2, 28.0, NivelRiesgo.ROJO, "Evacue", 24),
            new ReglaAlerta("Tumbes naranja", 1, 27.0, NivelRiesgo.NARANJA, "Atento", 12),
            new ReglaAlerta("Zona fantasma", 99, 20.0, NivelRiesgo.AMARILLO, "x", 1))),
        zonasService, zonaRepository, nivelRepository, alertaRepository,
        Clock.fixed(Instant.from(AHORA), ZoneOffset.UTC));

    private final ZonaVulnerableEntity piura = zona(2);
    private final NivelRiesgoEntity rojo = nivel(4, "ROJO");

    @BeforeEach
    void configurarZonas() {
        when(zonaRepository.existsById(1)).thenReturn(true);
        when(zonaRepository.existsById(2)).thenReturn(true);
        when(zonaRepository.existsById(99)).thenReturn(false);
        when(zonaRepository.findById(2)).thenReturn(Optional.of(piura));
        when(nivelRepository.findByNombre("ROJO")).thenReturn(Optional.of(rojo));
    }

    @Test
    void creaLaAlertaCuandoSeCumpleLaReglaYNoHayUnaVigente() {
        when(alertaRepository.findFirstByZonaIdAndNivelRiesgoIdAndVigenciaHastaAfterOrderByFechaEmisionDesc(2, 4, AHORA))
            .thenReturn(Optional.empty());
        when(alertaRepository.save(any())).thenAnswer(invocacion -> {
            AlertaPreventivaEntity alerta = invocacion.getArgument(0);
            alerta.setId(10);
            return alerta;
        });

        ResultadoEvaluacion resultado = service.evaluar(List.of(new LecturaZona(2, 28.4), new LecturaZona(1, 26.0)));

        assertEquals(1, resultado.alertas().size());
        ResultadoEvaluacion.Alerta alerta = resultado.alertas().get(0);
        assertEquals(10, alerta.alertaId());
        assertEquals("ROJO", alerta.nivel());
        assertEquals(ResultadoEvaluacion.Estado.CREADA, alerta.estado());

        ArgumentCaptor<AlertaPreventivaEntity> guardada = ArgumentCaptor.forClass(AlertaPreventivaEntity.class);
        verify(alertaRepository).save(guardada.capture());
        assertEquals(piura, guardada.getValue().getZona());
        assertEquals(AHORA.plusHours(24), guardada.getValue().getVigenciaHasta());
        assertEquals("Evacue (TSM 28.4 C, umbral 28.0 C)", guardada.getValue().getMensaje());

        assertEquals(List.of("La zona 99 usada en las reglas no existe"), resultado.advertencias());
    }

    @Test
    void noDuplicaUnaAlertaVigente() {
        AlertaPreventivaEntity existente = new AlertaPreventivaEntity();
        existente.setId(7);
        when(alertaRepository.findFirstByZonaIdAndNivelRiesgoIdAndVigenciaHastaAfterOrderByFechaEmisionDesc(2, 4, AHORA))
            .thenReturn(Optional.of(existente));

        ResultadoEvaluacion resultado = service.evaluar(List.of(new LecturaZona(2, 29.0)));

        assertEquals(ResultadoEvaluacion.Estado.YA_VIGENTE, resultado.alertas().get(0).estado());
        assertEquals(7, resultado.alertas().get(0).alertaId());
        assertTrue(resultado.advertencias().contains("Sin lectura de TSM para la zona 1"));
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void fallaSiElNivelNoExisteEnElCatalogo() {
        when(nivelRepository.findByNombre("ROJO")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> service.evaluar(List.of(new LecturaZona(2, 29.0))));
    }

    private static ZonaVulnerableEntity zona(int id) {
        ZonaVulnerableEntity zona = new ZonaVulnerableEntity();
        zona.setId(id);
        return zona;
    }

    private static NivelRiesgoEntity nivel(int id, String nombre) {
        NivelRiesgoEntity nivel = new NivelRiesgoEntity();
        nivel.setId(id);
        nivel.setNombre(nombre);
        return nivel;
    }
}
