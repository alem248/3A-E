package com.tecsup.clima.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.service.UbicacionRegistrada;
import com.tecsup.clima.service.UbicacionService;

/**
 * Recibe la posicion GPS del dispositivo. El permiso de ubicacion lo solicita
 * el cliente (navegador o app movil, ver {@code static/js/ubicacion.js}); este
 * endpoint solo recibe coordenadas ya autorizadas.
 */
@RestController
@RequestMapping("/api/usuarios/{usuarioId}/ubicacion")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    /** Guarda la posicion actual. Ej: {"latitud": -5.08, "longitud": -81.6, "precisionMetros": 15} */
    @PostMapping
    public ResponseEntity<UbicacionRegistrada> registrar(@PathVariable int usuarioId,
                                                         @Valid @RequestBody UbicacionRequest request) {
        UbicacionRegistrada registrada = ubicacionService.registrar(usuarioId, request.latitud(),
            request.longitud(), request.precisionMetros(), request.fechaCaptura());

        return ResponseEntity.created(URI.create("/api/usuarios/" + usuarioId + "/ubicacion"))
            .body(registrada);
    }

    /** Ultima posicion registrada del usuario, su zona y las alertas vigentes. */
    @GetMapping
    public ResponseEntity<UbicacionRegistrada> ultima(@PathVariable int usuarioId) {
        return ResponseEntity.ok(ubicacionService.ultima(usuarioId));
    }
}
