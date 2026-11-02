package com.tecsup.clima.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.service.PreferenciaAlertaService;
import com.tecsup.clima.web.dto.ActualizarPreferenciaRequest;

/**
 * Endpoints nuevos y aditivos para consultar y actualizar las preferencias de
 * alertas de un usuario por nivel de riesgo (HU-2).
 */
@RestController
@RequestMapping("/api/preferencias")
public class PreferenciaAlertaController {

    private final PreferenciaAlertaService preferenciaService;

    public PreferenciaAlertaController(PreferenciaAlertaService preferenciaService) {
        this.preferenciaService = preferenciaService;
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> listar(@PathVariable Integer usuarioId) {
        try {
            return ResponseEntity.ok(preferenciaService.listar(usuarioId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(404).body(Map.of("error", error.getMessage()));
        }
    }

    @PutMapping("/{usuarioId}/{nivelRiesgoId}")
    public ResponseEntity<?> actualizar(@PathVariable Integer usuarioId,
                                        @PathVariable Integer nivelRiesgoId,
                                        @RequestBody(required = false) ActualizarPreferenciaRequest request) {
        if (request == null || request.activa() == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El campo 'activa' es obligatorio."));
        }

        try {
            return ResponseEntity.ok(
                preferenciaService.actualizar(usuarioId, nivelRiesgoId, request.activa()));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
        }
    }
}
