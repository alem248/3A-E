package com.tecsup.clima.domain.historico;

import java.time.LocalDate;

/** Una observación diaria proveniente del archivo climático de Open-Meteo. */
public record RegistroHistoricoDto(
    LocalDate fecha,
    Double temperaturaMediaC,
    Double temperaturaMaxC,
    Double temperaturaMinC,
    Double precipitacionMm
) {
}
