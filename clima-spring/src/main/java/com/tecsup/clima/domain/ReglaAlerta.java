package com.tecsup.clima.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Regla de negocio: "si la TSM &gt; {@code temperaturaUmbral} (X) y la zona es
 * {@code zonaId} (Y), entonces generar una alerta de nivel {@code nivel} (Z)".
 *
 * <p>Se carga desde {@code application.properties} ({@code alertas.reglas[n].*})
 * y se valida al arrancar: una regla mal escrita impide iniciar la aplicacion.</p>
 *
 * @param nombre            identificador legible de la regla
 * @param zonaId            id de {@code zona_vulnerable} a la que aplica
 * @param temperaturaUmbral TSM en grados C que debe superarse (comparacion estricta)
 * @param nivel             nivel de riesgo de la alerta emitida
 * @param mensaje           texto de la alerta para el poblador
 * @param vigenciaHoras     horas que la alerta permanece vigente
 */
public record ReglaAlerta(
    @NotBlank String nombre,
    @NotNull @Positive Integer zonaId,
    @NotNull @DecimalMin("-5.0") @DecimalMax("40.0") Double temperaturaUmbral,
    @NotNull NivelRiesgo nivel,
    @NotBlank @Size(max = 500) String mensaje,
    @Min(1) @Max(720) int vigenciaHoras) {

    /** Evalua la condicion de la regla sobre una lectura. */
    public boolean seCumple(LecturaZona lectura) {
        return lectura.zonaId() == zonaId && lectura.temperatura() > temperaturaUmbral;
    }

    /** SIN_DATOS clasifica zonas sin lectura; no es un nivel de alerta valido. */
    @JsonIgnore
    @AssertTrue(message = "nivel no puede ser SIN_DATOS")
    public boolean isNivelEmitible() {
        return nivel != NivelRiesgo.SIN_DATOS;
    }
}
