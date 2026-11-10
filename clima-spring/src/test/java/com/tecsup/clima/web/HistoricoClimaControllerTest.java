package com.tecsup.clima.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tecsup.clima.service.DatoInvalidoException;

class HistoricoClimaControllerTest {

    @Test
    void convierteListaDeMesesIgnorandoEspacios() {
        assertEquals(Set.of(12, 1, 2), HistoricoClimaController.parsearMeses("12,1, 2"));
    }

    @Test
    void sinMesesDevuelveNull() {
        assertNull(HistoricoClimaController.parsearMeses(null));
        assertNull(HistoricoClimaController.parsearMeses("   "));
    }

    @Test
    void rechazaMesesQueNoSonNumeros() {
        assertThrows(DatoInvalidoException.class, () -> HistoricoClimaController.parsearMeses("enero,febrero"));
    }
}
