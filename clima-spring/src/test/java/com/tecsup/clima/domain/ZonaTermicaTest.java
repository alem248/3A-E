package com.tecsup.clima.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ZonaTermicaTest {

    // Poligono de ejemplo (Piura Norte), con vertices [latitud, longitud].
    private final ZonaTermica zona = new ZonaTermica(
        "piura-norte", "Piura Norte", "Piura", -4.35, -81.35,
        List.of(
            List.of(-4.0, -81.1),
            List.of(-4.0, -81.6),
            List.of(-4.7, -81.6),
            List.of(-4.7, -81.1)
        ),
        null, null, NivelRiesgo.SIN_DATOS, false
    );

    @Test
    void contieneElPuntoInterno() {
        assertTrue(zona.contiene(-4.35, -81.35));
    }

    @Test
    void noContieneElPuntoExterno() {
        assertFalse(zona.contiene(-5.5, -81.35));
        assertFalse(zona.contiene(-4.35, -80.0));
    }

    @Test
    void generaWktConAnilloCerrado() {
        String wkt = zona.poligonoWkt();

        assertTrue(wkt.startsWith("POLYGON(("));
        assertTrue(wkt.endsWith("))"));
        // El anillo debe cerrarse repitiendo el primer vertice.
        assertTrue(wkt.contains("-81.1 -4.0, -81.6 -4.0"));
    }
}
