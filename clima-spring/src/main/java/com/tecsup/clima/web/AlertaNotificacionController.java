package com.tecsup.clima.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.service.FirebaseNotificacionService;
import com.tecsup.clima.service.ResultadoNotificacion;

/**
 * Dispara el envio de una alerta preventiva ya registrada en el DER.
 * Endpoint nuevo y aditivo de la HU-2.
 */
@RestController
@RequestMapping("/api/alertas")
public class AlertaNotificacionController {

    private final FirebaseNotificacionService notificacionService;

    public AlertaNotificacionController(FirebaseNotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping("/{alertaId}/notificar")
    public ResponseEntity<?> notificar(@PathVariable Integer alertaId) {
        try {
            ResultadoNotificacion resultado = notificacionService.enviarAlerta(alertaId);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(404).body(Map.of("error", error.getMessage()));
        }
    }
}
