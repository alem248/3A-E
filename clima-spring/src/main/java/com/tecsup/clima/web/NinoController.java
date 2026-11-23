package com.tecsup.clima.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.service.IndiceNinoService;
import com.tecsup.clima.service.IndiceNinoService.IndiceNino;

/** Indicador propio del fenomeno El Nino (publico, para el mapa y la zona). */
@RestController
@RequestMapping("/api/nino")
public class NinoController {

    private final IndiceNinoService servicio;

    public NinoController(IndiceNinoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<?> indice() {
        IndiceNino indice = servicio.obtener();
        if (indice == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Índice Niño 3.4 no disponible."));
        }
        return ResponseEntity.ok(indice);
    }
}
