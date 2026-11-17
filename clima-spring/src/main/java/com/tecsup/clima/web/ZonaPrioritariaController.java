package com.tecsup.clima.web;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.service.ZonaPrioritariaService;
import com.tecsup.clima.web.dto.ActualizarPrioridadZonaRequest;
import com.tecsup.clima.web.dto.PrioridadZonaResponse;

/** API aditiva para consultar el orden y actualizar la prioridad de zonas. */
@RestController
@RequestMapping("/api/zonas-prioritarias")
public class ZonaPrioritariaController {

    private final ZonaPrioritariaService prioridades;

    public ZonaPrioritariaController(ZonaPrioritariaService prioridades) {
        this.prioridades = prioridades;
    }

    @GetMapping
    public List<PrioridadZonaResponse> listar() {
        return prioridades.listarOrdenadas();
    }

    @PatchMapping("/{zonaId}")
    public PrioridadZonaResponse actualizar(@PathVariable int zonaId,
                                             @Valid @RequestBody ActualizarPrioridadZonaRequest solicitud) {
        return prioridades.actualizar(zonaId, solicitud.prioritaria());
    }
}
