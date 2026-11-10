package com.tecsup.clima.domain.historico;

import java.time.LocalDate;

/** Comparación de la media diaria actual estimada con una media histórica. */
public record ComparacionHistoricaResponse(
    LocalDate fechaActual,
    LocalDate fechaHistorica,
    double latitud,
    double longitud,
    Double temperaturaActualMediaC,
    Double temperaturaHistoricaMediaC,
    Double diferenciaAbsolutaC,
    Double desviacionPorcentual,
    boolean disponible,
    String mensaje
) {
}
