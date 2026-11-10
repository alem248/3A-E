package com.tecsup.clima.service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resumen de una evaluacion del motor de reglas.
 *
 * @param fecha        momento de la evaluacion
 * @param alertas      alertas disparadas (creadas ahora o ya vigentes)
 * @param advertencias reglas que no pudieron evaluarse (zona inexistente, sin TSM...)
 */
public record ResultadoEvaluacion(OffsetDateTime fecha, List<Alerta> alertas, List<String> advertencias) {

    public enum Estado { CREADA, YA_VIGENTE }

    /**
     * @param alertaId    id en {@code alerta_preventiva}
     * @param zonaId      zona afectada
     * @param regla       nombre de la regla cumplida
     * @param nivel       nivel de riesgo emitido
     * @param temperatura TSM observada
     * @param estado      si se inserto ahora o ya existia una vigente
     */
    public record Alerta(int alertaId, int zonaId, String regla, String nivel, double temperatura, Estado estado) {
    }
}
