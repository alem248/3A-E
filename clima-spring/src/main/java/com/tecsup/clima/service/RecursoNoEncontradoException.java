package com.tecsup.clima.service;

/** El recurso pedido (usuario, ubicacion, nivel de riesgo...) no existe. La API responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
