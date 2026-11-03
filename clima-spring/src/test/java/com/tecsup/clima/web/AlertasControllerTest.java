package com.tecsup.clima.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tecsup.clima.service.EvaluacionAlertasService;
import com.tecsup.clima.service.ResultadoSimulacion;
import com.tecsup.clima.service.SimulacionEscenarioService;

@WebMvcTest(AlertasController.class)
class AlertasControllerTest {

    private static final String URL = "/api/reglas/simular-escenario";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EvaluacionAlertasService evaluacionService;

    @MockitoBean
    private SimulacionEscenarioService simulacionService;

    @Test
    void devuelveLasAlertasYElResultadoPorUbicacion() throws Exception {
        when(simulacionService.simular(eq("Nino fuerte"), any(), any()))
            .thenReturn(new ResultadoSimulacion("Nino fuerte",
                OffsetDateTime.parse("2026-11-03T12:00:00Z"),
                List.of(new ResultadoSimulacion.AlertaEscenario(
                    1, "Tumbes", "Tumbes rojo", "ROJO", 29.5, "Evacue")),
                List.of(new ResultadoSimulacion.EvaluacionUbicacion(
                    "Tumbes", -3.62, -81.0, 1, "Tumbes", "AMARILLO",
                    true, true, "ROJO", "Evacue"))));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Nino fuerte","lecturas":[{"zonaId":1,"temperatura":29.5}],
                     "ubicaciones":[{"etiqueta":"Tumbes","latitud":-3.62,"longitud":-81.0}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.escenario").value("Nino fuerte"))
            .andExpect(jsonPath("$.alertas[0].zona").value("Tumbes"))
            .andExpect(jsonPath("$.ubicaciones[0].alertaDisparada").value(true));
    }

    @Test
    void rechazaUnEscenarioVacio() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"x\",\"lecturas\":[],\"ubicaciones\":[]}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(simulacionService);
    }

    @Test
    void rechazaCoordenadasFueraDeRango() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"x","lecturas":[{"zonaId":1,"temperatura":29.5}],
                     "ubicaciones":[{"latitud":95,"longitud":-200}]}
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(simulacionService);
    }
}
