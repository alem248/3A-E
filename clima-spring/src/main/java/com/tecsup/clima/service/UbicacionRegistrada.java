package com.tecsup.clima.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ubicacion guardada, la zona vulnerable en la que cae y sus alertas vigentes.
 *
 * @param id               id en {@code ubicacion_usuario}
 * @param usuarioId        duenio de la ubicacion
 * @param latitud          latitud guardada (6 decimales, ~0.1 m)
 * @param longitud         longitud guardada (6 decimales)
 * @param precisionMetros  precision reportada por el dispositivo (puede ser null)
 * @param fechaRegistro    marca temporal de la captura
 * @param enZonaVulnerable si el punto cae dentro de alguna zona
 * @param zona             zona que contiene el punto, o null
 * @param alertasVigentes  alertas vigentes de esa zona
 */
public record UbicacionRegistrada(
    int id,
    int usuarioId,
    BigDecimal latitud,
    BigDecimal longitud,
    Double precisionMetros,
    OffsetDateTime fechaRegistro,
    boolean enZonaVulnerable,
    Zona zona,
    List<Alerta> alertasVigentes) {

    /** Zona vulnerable que contiene el punto. */
    public record Zona(int id, String nombre, String nivelRiesgo, String colorHex) {
    }

    /** Alerta vigente de la zona. */
    public record Alerta(int id, String nivelRiesgo, String mensaje, OffsetDateTime vigenciaHasta) {
    }
}
