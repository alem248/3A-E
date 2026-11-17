package com.tecsup.clima.web.dto;

import jakarta.validation.constraints.NotNull;

/** Estado solicitado al marcar o desmarcar una zona en el panel. */
public record ActualizarPrioridadZonaRequest(@NotNull Boolean prioritaria) {
}
