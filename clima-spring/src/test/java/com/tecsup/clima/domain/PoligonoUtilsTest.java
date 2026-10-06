package com.tecsup.clima.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PoligonoUtilsTest {

    // Poligono de ejemplo (Piura Norte), con vertices [latitud, longitud].
    private final List<List<Double>> poligono = List.of(
        List.of(-4.0, -81.1),
        List.of(-4.0, -81.6),
        List.of(-4.7, -81.6),
        List.of(-4.7, -81.1)
    );

    @Test
    void generaWktConAnilloCerrado() {
        String wkt = PoligonoUtils.aWkt(poligono);

        assertTrue(wkt.startsWith("POLYGON(("));
        assertTrue(wkt.endsWith("))"));
        // WKT usa X=longitud, Y=latitud y cierra el anillo.
        assertTrue(wkt.contains("-81.1 -4.0, -81.6 -4.0"));
        assertTrue(wkt.endsWith("-81.1 -4.0))"));
    }

    @Test
    void convierteWktAParesLatitudLongitud() {
        List<List<Double>> resultado = PoligonoUtils.desdeWkt(
            PoligonoUtils.aWkt(poligono));

        assertEquals(4, resultado.size());
        assertEquals(-4.0, resultado.get(0).get(0));
        assertEquals(-81.1, resultado.get(0).get(1));
    }

    @Test
    void detectaPuntoInternoYExterno() {
        assertTrue(PoligonoUtils.contiene(poligono, -4.35, -81.35));
        org.junit.jupiter.api.Assertions.assertFalse(PoligonoUtils.contiene(poligono, -5.5, -81.35));
        org.junit.jupiter.api.Assertions.assertFalse(PoligonoUtils.contiene(poligono, -4.35, -80.0));
    }

    @Test
    void calculaElCentroide() {
        double[] centroide = PoligonoUtils.centroide(poligono);

        assertEquals(-4.35, centroide[0], 0.0001);
        assertEquals(-81.35, centroide[1], 0.0001);
    }
}