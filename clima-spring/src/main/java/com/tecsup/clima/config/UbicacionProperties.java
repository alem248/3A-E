package com.tecsup.clima.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Limites para aceptar una posicion GPS ({@code ubicacion.*}).
 *
 * @param precisionMaximaM      se rechaza una precision peor que esta (metros): una posicion
 *                              por antena celular puede ubicar al usuario en otra zona
 * @param antiguedadMaximaHoras se rechazan posiciones capturadas hace mas de estas horas
 * @param toleranciaFuturoSeg   margen para relojes de dispositivo adelantados
 */
@Validated
@ConfigurationProperties(prefix = "ubicacion")
public record UbicacionProperties(
    @DefaultValue("1000") @Min(1) int precisionMaximaM,
    @DefaultValue("24") @Min(1) @Max(720) int antiguedadMaximaHoras,
    @DefaultValue("300") @Min(0) int toleranciaFuturoSeg) {
}
