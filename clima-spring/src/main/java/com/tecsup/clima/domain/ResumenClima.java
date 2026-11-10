package com.tecsup.clima.domain;

/**
 * Promedios y totales de un conjunto de registros diarios. Los promedios quedan null
 * cuando no hay datos validos para calcularlos.
 */
public record ResumenClima(int diasConsiderados,
                           Double temperaturaMaxPromedioC,
                           Double temperaturaMinPromedioC,
                           Double precipitacionTotalMm) {
}
