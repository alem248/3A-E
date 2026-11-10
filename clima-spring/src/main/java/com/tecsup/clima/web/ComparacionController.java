package com.tecsup.clima.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.service.ComparacionUbicacion;
import com.tecsup.clima.service.ComparacionUbicacionService;

/** Indica si la ultima ubicacion del usuario esta en, cerca de o fuera de una zona del fenomeno. */
@RestController
@RequestMapping("/api/usuarios/{usuarioId}/comparacion")
public class ComparacionController {

    private final ComparacionUbicacionService comparacionService;

    public ComparacionController(ComparacionUbicacionService comparacionService) {
        this.comparacionService = comparacionService;
    }

    /** Ej: {"estado": "CERCA", "zonaNombre": "...", "distanciaKm": 26.6, "riesgo": true} */
    @GetMapping
    public ResponseEntity<ComparacionUbicacion> comparar(@PathVariable int usuarioId) {
        return ResponseEntity.ok(comparacionService.comparar(usuarioId));
    }
}
