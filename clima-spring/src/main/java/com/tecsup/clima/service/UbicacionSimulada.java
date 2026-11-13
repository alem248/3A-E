package com.tecsup.clima.service;

/**
 * Ubicacion de un usuario dentro de un escenario de simulacion.
 *
 * @param etiqueta nombre visible de la ubicacion
 * @param latitud  grados decimales WGS84
 * @param longitud grados decimales WGS84
 */
public record UbicacionSimulada(String etiqueta, double latitud, double longitud) {

    public UbicacionSimulada {
        if (!Double.isFinite(latitud) || latitud < -90.0 || latitud > 90.0) {
            throw new IllegalArgumentException("latitud fuera de rango");
        }
        if (!Double.isFinite(longitud) || longitud < -180.0 || longitud > 180.0) {
            throw new IllegalArgumentException("longitud fuera de rango");
        }
    }
}
