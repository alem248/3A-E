package com.tecsup.clima.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Lectura hipotetica para simular las reglas: {@code {"zonaId": 2, "temperatura": 28.5}}.
 *
 * @param zonaId      id de la zona vulnerable
 * @param temperatura TSM en grados C
 */
public record LecturaRequest(
    @NotNull @Positive Integer zonaId,
    @NotNull @DecimalMin("-5.0") @DecimalMax("40.0") Double temperatura) {
}
