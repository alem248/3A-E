package com.tecsup.clima.domain;

import java.util.List;

/** Registros diarios ya filtrados por temporada y su resumen. */
public record HistoricoClima(List<RegistroClimaDiario> registros, ResumenClima resumen) {
}
