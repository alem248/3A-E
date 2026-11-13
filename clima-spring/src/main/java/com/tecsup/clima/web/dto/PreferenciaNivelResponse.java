package com.tecsup.clima.web.dto;

/** Preferencia de alerta de un usuario para un nivel de riesgo. */
public record PreferenciaNivelResponse(Integer nivelRiesgoId,
                                       String nombre,
                                       String colorHex,
                                       String descripcion,
                                       boolean activa) {
}
