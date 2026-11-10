package com.tecsup.clima.domain;

import java.util.Locale;

/**
 * Resultado de una regla cumplida: que regla se disparo y con que lectura.
 *
 * @param regla   regla cumplida
 * @param lectura lectura que la disparo
 */
public record AlertaGenerada(ReglaAlerta regla, LecturaZona lectura) {

    /** Mensaje de la regla con la TSM observada, para que el poblador vea el dato real. */
    public String mensajeCompleto() {
        return String.format(Locale.ROOT, "%s (TSM %.1f C, umbral %.1f C)",
            regla.mensaje(), lectura.temperatura(), regla.temperaturaUmbral());
    }
}
