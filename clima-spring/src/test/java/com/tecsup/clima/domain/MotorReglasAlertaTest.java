package com.tecsup.clima.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MotorReglasAlertaTest {

    private static final ReglaAlerta PIURA_ROJO =
        new ReglaAlerta("Piura rojo", 2, 28.0, NivelRiesgo.ROJO, "Evacue", 24);
    private static final ReglaAlerta PIURA_NARANJA =
        new ReglaAlerta("Piura naranja", 2, 26.0, NivelRiesgo.NARANJA, "Preparese", 24);
    private static final ReglaAlerta TUMBES_NARANJA =
        new ReglaAlerta("Tumbes naranja", 1, 27.0, NivelRiesgo.NARANJA, "Atento", 12);

    private final MotorReglasAlerta motor =
        new MotorReglasAlerta(List.of(PIURA_NARANJA, PIURA_ROJO, TUMBES_NARANJA));

    @Test
    void disparaLaReglaSiLaTemperaturaSuperaElUmbralEnSuZona() {
        assertEquals(Optional.of(TUMBES_NARANJA), motor.evaluar(new LecturaZona(1, 27.5)));
    }

    @Test
    void elUmbralEsEstricto() {
        assertEquals(Optional.empty(), motor.evaluar(new LecturaZona(1, 27.0)));
    }

    @Test
    void laReglaNoAplicaAOtraZona() {
        assertEquals(Optional.empty(), motor.evaluar(new LecturaZona(5, 35.0)));
    }

    @Test
    void siVariasReglasSeCumplenGanaLaDeUmbralMasAlto() {
        assertEquals(Optional.of(PIURA_ROJO), motor.evaluar(new LecturaZona(2, 29.0)));
        assertEquals(Optional.of(PIURA_NARANJA), motor.evaluar(new LecturaZona(2, 27.0)));
    }

    @Test
    void evaluaVariasZonasYDevuelveUnaAlertaPorZona() {
        List<AlertaGenerada> alertas = motor.evaluar(List.of(
            new LecturaZona(2, 29.0), new LecturaZona(1, 25.0), new LecturaZona(5, 30.0)));

        assertEquals(1, alertas.size());
        assertEquals(PIURA_ROJO, alertas.get(0).regla());
        assertEquals("Evacue (TSM 29.0 C, umbral 28.0 C)", alertas.get(0).mensajeCompleto());
    }

    @Test
    void rechazaEntradasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> new MotorReglasAlerta(null));
        assertThrows(IllegalArgumentException.class, () -> motor.evaluar((LecturaZona) null));
        assertThrows(IllegalArgumentException.class, () -> new LecturaZona(0, 25.0));
        assertThrows(IllegalArgumentException.class, () -> new LecturaZona(1, Double.NaN));
    }

    @Test
    void laValidacionDetectaReglasMalConfiguradas() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<String> camposInvalidos = validator
            .validate(new ReglaAlerta("", -1, 99.0, NivelRiesgo.SIN_DATOS, " ", 0)).stream()
            .map(violacion -> violacion.getPropertyPath().toString())
            .collect(Collectors.toSet());

        assertEquals(Set.of("nombre", "zonaId", "temperaturaUmbral", "nivelEmitible", "mensaje", "vigenciaHoras"),
            camposInvalidos);
        assertTrue(validator.validate(PIURA_ROJO).isEmpty());
    }
}
