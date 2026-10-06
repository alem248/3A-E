package com.tecsup.clima.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NivelRiesgoTest {

    @Test
    void clasificaPorDebajoDelUmbralVerde() {
        assertEquals(NivelRiesgo.VERDE, NivelRiesgo.clasificarTemperatura(20.0));
        assertEquals(NivelRiesgo.VERDE, NivelRiesgo.clasificarTemperatura(23.9));
    }

    @Test
    void clasificaLosUmbralesIntermedios() {
        assertEquals(NivelRiesgo.AMARILLO, NivelRiesgo.clasificarTemperatura(24.0));
        assertEquals(NivelRiesgo.AMARILLO, NivelRiesgo.clasificarTemperatura(25.9));
        assertEquals(NivelRiesgo.NARANJA, NivelRiesgo.clasificarTemperatura(26.0));
        assertEquals(NivelRiesgo.NARANJA, NivelRiesgo.clasificarTemperatura(27.9));
        assertEquals(NivelRiesgo.ROJO, NivelRiesgo.clasificarTemperatura(28.0));
        assertEquals(NivelRiesgo.ROJO, NivelRiesgo.clasificarTemperatura(31.5));
    }

    @Test
    void clasificaSinDatosCuandoNoHayLectura() {
        assertEquals(NivelRiesgo.SIN_DATOS, NivelRiesgo.clasificarTemperatura(null));
        assertEquals(NivelRiesgo.SIN_DATOS, NivelRiesgo.clasificarTemperatura(Double.NaN));
    }
}
