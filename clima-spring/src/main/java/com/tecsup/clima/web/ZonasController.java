package com.tecsup.clima.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.domain.ZonaTermica;
import com.tecsup.clima.service.ZonasTermicasService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ZonasController {

    private final ZonasTermicasService zonasService;

    public ZonasController(ZonasTermicasService zonasService) {
        this.zonasService = zonasService;
    }

    /**
     * Devuelve las zonas termicas con su TSM en tiempo real.
     *
     * <p>Sin parametros retorna todas las zonas. Si se envian {@code lat} y
     * {@code lon}, filtra las zonas cuyo poligono contiene ese punto
     * (filtro por zona geografica de la HU-1).</p>
     */
    @GetMapping("/zonas")
    public ResponseEntity<List<ZonaTermica>> getZonas(
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon) {

        if (lat == null || lon == null) {
            return ResponseEntity.ok(zonasService.consultarTsmPorZonas());
        }

        return ResponseEntity.ok(zonasService.consultarTsmPorZonas(lat, lon));
    }
}
