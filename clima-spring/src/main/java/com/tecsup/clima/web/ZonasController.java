package com.tecsup.clima.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/zonas")
    public ResponseEntity<List<ZonaTermica>> getZonas() {
        return ResponseEntity.ok(zonasService.consultarTsmPorZonas());
    }
}