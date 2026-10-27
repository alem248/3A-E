package com.tecsup.clima.web;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Coordenadas GPS enviadas por el dispositivo (p. ej. desde {@code navigator.geolocation}).
 *
 * @param latitud         grados decimales WGS84
 * @param longitud        grados decimales WGS84
 * @param precisionMetros radio de incertidumbre reportado por el dispositivo (opcional)
 * @param fechaCaptura    momento de la captura en ISO-8601 (opcional; por defecto la hora del servidor)
 */
public record UbicacionRequest(
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud,
    @PositiveOrZero Double precisionMetros,
    OffsetDateTime fechaCaptura) {
}
