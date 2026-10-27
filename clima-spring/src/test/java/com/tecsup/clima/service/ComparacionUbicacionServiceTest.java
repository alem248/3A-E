package com.tecsup.clima.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tecsup.clima.persistence.ComparacionZonaProyeccion;
import com.tecsup.clima.persistence.ComparacionZonaRepository;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

class ComparacionUbicacionServiceTest {

    private final UbicacionService ubicacionService = mock(UbicacionService.class);
    private final ComparacionZonaRepository comparacionRepository = mock(ComparacionZonaRepository.class);
    private final ZonaVulnerableRepository zonaRepository = mock(ZonaVulnerableRepository.class);
    private final ComparacionUbicacionService servicio =
            new ComparacionUbicacionService(ubicacionService, comparacionRepository, zonaRepository, 50.0);

    @Test
    void clasificaDentroDeZonaComoEnZona() {
        assertEquals("EN_ZONA", ComparacionUbicacionService.clasificar(true, 0, 50_000));
    }

    @Test
    void clasificaDentroDelUmbralComoCerca() {
        assertEquals("CERCA", ComparacionUbicacionService.clasificar(false, 26_600, 50_000));
        assertEquals("CERCA", ComparacionUbicacionService.clasificar(false, 50_000, 50_000));
    }

    @Test
    void clasificaLejosComoFuera() {
        assertEquals("FUERA", ComparacionUbicacionService.clasificar(false, 50_001, 50_000));
    }

    @Test
    void riesgoSoloEnNaranjaORojo() {
        assertTrue(ComparacionUbicacionService.esRiesgoAlto("ROJO"));
        assertTrue(ComparacionUbicacionService.esRiesgoAlto("NARANJA"));
        assertFalse(ComparacionUbicacionService.esRiesgoAlto("AMARILLO"));
        assertFalse(ComparacionUbicacionService.esRiesgoAlto(null));
    }

    @Test
    void compara_usuarioCercaDeZonaNaranja() {
        when(ubicacionService.ultima(7)).thenReturn(ubicacion(-5.19, -80.63));
        ComparacionZonaProyeccion p = proyeccion(2, false, 26_600.0);
        when(comparacionRepository.buscarZonaMasCercana(-5.19, -80.63))
                .thenReturn(Optional.of(p));
        when(zonaRepository.findById(2)).thenReturn(Optional.of(zona(2, "NARANJA", "#FFA500")));

        ComparacionUbicacion r = servicio.comparar(7);

        assertEquals("CERCA", r.estado());
        assertEquals(2, r.zonaId());
        assertEquals(26.6, r.distanciaKm());
        assertEquals("NARANJA", r.nivelRiesgo());
        assertTrue(r.riesgo());
    }

    @Test
    void compara_puntoDentroDeZonaAmarilla() {
        when(ubicacionService.ultima(8)).thenReturn(ubicacion(-5.0, -81.0));
        ComparacionZonaProyeccion p = proyeccion(3, true, 0.0);
        when(comparacionRepository.buscarZonaMasCercana(-5.0, -81.0))
                .thenReturn(Optional.of(p));
        when(zonaRepository.findById(3)).thenReturn(Optional.of(zona(3, "AMARILLO", "#FFFF00")));

        ComparacionUbicacion r = servicio.comparar(8);

        assertEquals("EN_ZONA", r.estado());
        assertEquals(0.0, r.distanciaKm());
        assertFalse(r.riesgo());
    }

    @Test
    void compara_sinZonasConGeometriaEsFuera() {
        when(ubicacionService.ultima(9)).thenReturn(ubicacion(-12.0, -77.0));
        when(comparacionRepository.buscarZonaMasCercana(-12.0, -77.0)).thenReturn(Optional.empty());

        ComparacionUbicacion r = servicio.comparar(9);

        assertEquals("FUERA", r.estado());
        assertNull(r.zonaId());
        assertFalse(r.riesgo());
    }

    private static UbicacionRegistrada ubicacion(double lat, double lng) {
        return new UbicacionRegistrada(1, 7, BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), 10.0,
                OffsetDateTime.now(), false, null, List.of());
    }

    private static ComparacionZonaProyeccion proyeccion(int zonaId, boolean dentro, double distanciaM) {
        ComparacionZonaProyeccion p = mock(ComparacionZonaProyeccion.class);
        when(p.getZonaId()).thenReturn(zonaId);
        when(p.getDentro()).thenReturn(dentro);
        when(p.getDistanciaM()).thenReturn(distanciaM);
        return p;
    }

    private static ZonaVulnerableEntity zona(int id, String nivel, String color) {
        NivelRiesgoEntity n = new NivelRiesgoEntity();
        n.setNombre(nivel);
        n.setColorHex(color);
        ZonaVulnerableEntity z = new ZonaVulnerableEntity();
        z.setId(id);
        z.setNivelRiesgo(n);
        return z;
    }
}