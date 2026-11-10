package com.tecsup.clima.domain.historico;

import java.time.LocalDate;
import java.util.List;

/** Resultado autocontenido para la consulta de un rango histórico. */
public record SerieHistoricaResponse(
    LocalDate desde,
    LocalDate hasta,
    double latitud,
    double longitud,
    List<RegistroHistoricoDto> registros,
    boolean disponible,
    String mensaje
) {
    public SerieHistoricaResponse {
        registros = registros == null ? List.of() : List.copyOf(registros);
    }
}
