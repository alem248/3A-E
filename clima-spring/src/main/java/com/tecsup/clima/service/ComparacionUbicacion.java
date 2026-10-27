package com.tecsup.clima.service;

/**
 * Resultado de comparar la ultima ubicacion del usuario con las zonas del fenomeno.
 *
 * @param estado    EN_ZONA (dentro de una zona), CERCA (a {@code umbral} km o menos) o FUERA
 * @param zonaId    zona mas cercana; null si no hay zonas con geometria
 * @param distanciaKm distancia a la zona mas cercana en km (0 si esta dentro); null si FUERA sin zonas
 * @param riesgo    true si la zona mas cercana esta en nivel NARANJA o ROJO
 */
public record ComparacionUbicacion(
    int usuarioId,
    String estado,
    Integer zonaId,
    String zonaNombre,
    String nivelRiesgo,
    String colorHex,
    Double distanciaKm,
    boolean riesgo) {
}
