package com.tecsup.clima.domain;

import java.time.LocalDate;

/**
 * Clima de un dia para una coordenada: temperatura maxima y minima del aire (grados C)
 * y precipitacion acumulada (milimetros). Los campos pueden ser null si la fuente no los entrega.
 */
public record RegistroClimaDiario(LocalDate fecha,
                                  Double temperaturaMaxC,
                                  Double temperaturaMinC,
                                  Double precipitacionMm) {
}
