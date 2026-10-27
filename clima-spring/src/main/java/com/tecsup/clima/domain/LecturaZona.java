package com.tecsup.clima.domain;

/**
 * TSM observada en una zona vulnerable, entrada del motor de reglas.
 *
 * @param zonaId      id de {@code zona_vulnerable}
 * @param temperatura TSM en grados C
 */
public record LecturaZona(int zonaId, double temperatura) {

    public LecturaZona {
        if (zonaId <= 0) {
            throw new IllegalArgumentException("zonaId debe ser positivo");
        }
        if (!Double.isFinite(temperatura)) {
            throw new IllegalArgumentException("temperatura debe ser un numero finito");
        }
    }
}
