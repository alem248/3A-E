package com.tecsup.clima.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Evalua las reglas de alerta sobre las lecturas de TSM de cada zona.
 * No depende de Spring ni de la base de datos, por lo que se prueba de forma aislada.
 *
 * <p>Si varias reglas de una misma zona se cumplen gana la de umbral mas alto
 * (la mas especifica): con "&gt; 26 C -&gt; NARANJA" y "&gt; 28 C -&gt; ROJO",
 * una lectura de 29 C genera solo la alerta ROJO.</p>
 */
public class MotorReglasAlerta {

    private final List<ReglaAlerta> reglas;

    public MotorReglasAlerta(Collection<ReglaAlerta> reglas) {
        if (reglas == null) {
            throw new IllegalArgumentException("La lista de reglas no puede ser nula");
        }
        this.reglas = List.copyOf(reglas);
    }

    public List<ReglaAlerta> getReglas() {
        return reglas;
    }

    /** Regla ganadora para la lectura, o vacio si ninguna se cumple. */
    public Optional<ReglaAlerta> evaluar(LecturaZona lectura) {
        if (lectura == null) {
            throw new IllegalArgumentException("La lectura no puede ser nula");
        }

        return reglas.stream()
            .filter(regla -> regla.seCumple(lectura))
            .max(Comparator.comparingDouble(ReglaAlerta::temperaturaUmbral));
    }

    /** Evalua varias zonas: una alerta por cada zona con alguna regla cumplida. */
    public List<AlertaGenerada> evaluar(Collection<LecturaZona> lecturas) {
        List<AlertaGenerada> alertas = new ArrayList<>();

        for (LecturaZona lectura : lecturas) {
            evaluar(lectura).ifPresent(regla -> alertas.add(new AlertaGenerada(regla, lectura)));
        }

        return alertas;
    }
}
