package com.tecsup.clima.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tecsup.clima.service.DatoInvalidoException;
import com.tecsup.clima.service.UbicacionRegistrada;
import com.tecsup.clima.service.UbicacionService;

@WebMvcTest(UbicacionController.class)
class UbicacionControllerTest {

    private static final String URL = "/api/usuarios/1/ubicacion";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UbicacionService ubicacionService;

    @Test
    void registraUnaUbicacionValidaYResponde201() throws Exception {
        when(ubicacionService.registrar(eq(1), eq(-5.25), eq(-81.6), eq(12.0), any()))
            .thenReturn(new UbicacionRegistrada(5, 1, new BigDecimal("-5.250000"), new BigDecimal("-81.600000"),
                12.0, OffsetDateTime.parse("2026-10-27T15:00:00Z"), true,
                new UbicacionRegistrada.Zona(3, "Piura Sur (Paita)", "ROJO", "#EF4444"), List.of()));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitud\": -5.25, \"longitud\": -81.6, \"precisionMetros\": 12}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", URL))
            .andExpect(jsonPath("$.enZonaVulnerable").value(true))
            .andExpect(jsonPath("$.zona.nombre").value("Piura Sur (Paita)"));
    }

    @Test
    void coordenadasFueraDeRangoDan400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitud\": 95, \"longitud\": -200}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje", containsString("latitud")))
            .andExpect(jsonPath("$.mensaje", containsString("longitud")));

        verifyNoInteractions(ubicacionService);
    }

    @Test
    void coordenadasAusentesOJsonMalFormadoDan400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{lat"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje", containsString("JSON")));
    }

    @Test
    void unErrorDeNegocioDelServicioDa400ConSuMensaje() throws Exception {
        when(ubicacionService.registrar(eq(1), anyDouble(), anyDouble(), any(), any()))
            .thenThrow(new DatoInvalidoException("fechaCaptura no puede estar en el futuro"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitud\": -5.2, \"longitud\": -81.6, \"fechaCaptura\": \"2030-01-01T00:00:00Z\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("fechaCaptura no puede estar en el futuro"));
    }
}
