package com.tecsup.clima.web;

import java.util.List;

public record AdminHistoryResponse(
    String zonaId, String zonaNombre, String unidad, Double umbralAlerta,
    boolean alerta, List<PuntoHistorial> puntos) {
    public record PuntoHistorial(String fecha, double valor) {}
}
