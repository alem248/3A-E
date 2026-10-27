package com.tecsup.clima.service;

/** Los datos recibidos incumplen una regla de negocio (p. ej. fecha de captura futura). La API responde 400. */
public class DatoInvalidoException extends RuntimeException {

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
