package com.tecsup.clima.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tecsup.clima.config.ReglasAlertaProperties;
import com.tecsup.clima.domain.LecturaZona;
import com.tecsup.clima.domain.NivelRiesgo;
import com.tecsup.clima.domain.PoligonoUtils;
import com.tecsup.clima.domain.ReglaAlerta;
import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

class SimulacionEscenarioServiceTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 3, 12, 0, 0, 0, ZoneOffset.UTC);

    private final ZonaVulnerableRepository zonaRepository = mock(ZonaVulnerableRepository.class);

    private final SimulacionEscenarioService service = new SimulacionEscenarioService(
        new ReglasAlertaProperties(false, List.of(
            new ReglaAlerta("Tumbes rojo", 1, 28.0, NivelRiesgo.ROJO, "Evacue", 24),
            new ReglaAlerta("Piura rojo", 2, 28.0, NivelRiesgo.ROJO, "Evacue", 24))),
        zonaRepository, Clock.fixed(Instant.from(AHORA), ZoneOffset.UTC));

    @BeforeEach
    void configurarZonas() {
        when(zonaRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
            zona(1, NivelRiesgo.AMARILLO), zona(2, NivelRiesgo.AMARILLO), zona(3, NivelRiesgo.VERDE)));
    }

    @Test
    void disparaLaAlertaSoloEnUbicacionesDentroDeZonasCriticas() {
        ResultadoSimulacion resultado = service.simular("Nino fuerte",
            List.of(new LecturaZona(1, 29.5), new LecturaZona(2, 29.0)),
            List.of(new UbicacionSimulada("Tumbes", -3.62, -81.0),
                new UbicacionSimulada("Piura", -4.35, -81.35),
                new UbicacionSimulada("Lima", -12.05, -77.04)));

        assertEquals(2, resultado.alertas().size());
        assertEquals(AHORA, resultado.fecha());

        assertTrue(resultado.ubicaciones().get(0).alertaDisparada());
        assertEquals("ROJO", resultado.ubicaciones().get(0).nivelAlerta());
        assertTrue(resultado.ubicaciones().get(1).alertaDisparada());

        assertFalse(resultado.ubicaciones().get(2).alertaDisparada());
        assertNull(resultado.ubicaciones().get(2).zonaId());
        assertEquals("Ubicacion fuera de toda zona vulnerable: no se genera alerta",
            resultado.ubicaciones().get(2).mensaje());
    }

    @Test
    void noDisparaLaAlertaSiElEscenarioNoSuperaElUmbral() {
        ResultadoSimulacion resultado = service.simular("Normal",
            List.of(new LecturaZona(1, 23.0), new LecturaZona(2, 23.0)),
            List.of(new UbicacionSimulada("Tumbes", -3.62, -81.0)));

        assertTrue(resultado.alertas().isEmpty());
        assertFalse(resultado.ubicaciones().get(0).alertaDisparada());
        assertFalse(resultado.ubicaciones().get(0).zonaCritica());
        assertEquals(1, resultado.ubicaciones().get(0).zonaId());
    }

    @Test
    void unaZonaNoAfectadaNoRecibeAlertaAunqueLaUbicacionEsteDentro() {
        ResultadoSimulacion resultado = service.simular("Solo Tumbes",
            List.of(new LecturaZona(1, 29.5)),
            List.of(new UbicacionSimulada("Piura", -4.35, -81.35)));

        ResultadoSimulacion.EvaluacionUbicacion piura = resultado.ubicaciones().get(0);
        assertEquals(2, piura.zonaId());
        assertFalse(piura.zonaCritica());
        assertFalse(piura.alertaDisparada());
        assertNull(piura.nivelAlerta());
        assertEquals("Zona vulnerable no afectada por el escenario: no se genera alerta", piura.mensaje());
    }

    @Test
    void laAlertaUsaElNombreDeLaZonaDelCatalogo() {
        ResultadoSimulacion resultado = service.simular("Nino fuerte",
            List.of(new LecturaZona(1, 29.5)),
            List.of(new UbicacionSimulada("Tumbes", -3.62, -81.0)));

        assertEquals("Tumbes", resultado.alertas().get(0).zona());
        assertEquals("Tumbes rojo", resultado.alertas().get(0).regla());
        assertEquals(29.5, resultado.alertas().get(0).temperatura());
    }

    @Test
    void laSimulacionNoGuardaNada() {
        service.simular("Nino fuerte", List.of(new LecturaZona(1, 29.5)),
            List.of(new UbicacionSimulada("Tumbes", -3.62, -81.0)));

        verify(zonaRepository, never()).save(any());
    }

    private static ZonaVulnerableEntity zona(int id, NivelRiesgo nivel) {
        ZonaVulnerableEntity zona = new ZonaVulnerableEntity();
        zona.setId(id);
        zona.setPoligono(PoligonoUtils.aWkt(ZonasTermicas.getZona(id).getPoligono()));

        NivelRiesgoEntity entidad = new NivelRiesgoEntity();
        entidad.setId(id);
        entidad.setNombre(nivel.getNombre());
        entidad.setColorHex(nivel.getColor());
        zona.setNivelRiesgo(entidad);
        return zona;
    }
}
