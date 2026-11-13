package com.tecsup.clima.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ubicacion a verificar dentro de un escenario de simulacion.
 *
 * @param etiqueta nombre visible de la ubicacion (opcional)
 * @param latitud  grados decimales WGS84
 * @param longitud grados decimales WGS84
 */
public record UbicacionSimuladaRequest(
    @Size(max = 120) String etiqueta,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud) {
}
