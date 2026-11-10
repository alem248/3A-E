package com.tecsup.clima.config;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import com.tecsup.clima.domain.ReglaAlerta;

/**
 * Configuracion del motor de reglas ({@code alertas.*} en application.properties).
 *
 * @param evaluacionAutomatica si es {@code true} las reglas se evaluan cada {@code alertas.intervalo-ms}
 * @param reglas               reglas a evaluar
 */
@Validated
@ConfigurationProperties(prefix = "alertas")
public record ReglasAlertaProperties(
    @DefaultValue("false") boolean evaluacionAutomatica,
    @NotNull @DefaultValue List<@Valid ReglaAlerta> reglas) {
}
