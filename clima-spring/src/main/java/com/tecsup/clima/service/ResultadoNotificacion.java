package com.tecsup.clima.service;

/** Resumen del envio de una alerta preventiva a los usuarios afectados. */
public record ResultadoNotificacion(Integer alertaId,
                                    int destinatarios,
                                    int enviados,
                                    int fallidos,
                                    boolean simulado) {
}
