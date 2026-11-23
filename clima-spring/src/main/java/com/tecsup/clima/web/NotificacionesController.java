package com.tecsup.clima.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.service.NotificacionService;

/** Avisos del usuario con sesion iniciada (campanita de la pagina principal). */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionesController {

    private final NotificacionService notificacionService;

    public NotificacionesController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication autenticacion) {
        String email = emailDe(autenticacion);
        if (email == null) {
            return sinSesion();
        }
        return ResponseEntity.ok(notificacionService.listar(email));
    }

    @PostMapping("/leidas")
    public ResponseEntity<?> marcarLeidas(Authentication autenticacion) {
        String email = emailDe(autenticacion);
        if (email == null) {
            return sinSesion();
        }
        notificacionService.marcarLeidas(email);
        return ResponseEntity.noContent().build();
    }

    private static String emailDe(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getName())) {
            return null;
        }
        return autenticacion.getName();
    }

    private static ResponseEntity<?> sinSesion() {
        return ResponseEntity.status(401).body(Map.of("error", "Inicia sesion para ver tus avisos."));
    }
}
