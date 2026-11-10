package com.tecsup.clima.web;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.domain.historico.SerieHistoricaResponse;
import com.tecsup.clima.service.HistoricoClimaService;

/** API de series climáticas diarias históricas. */
@RestController
@RequestMapping("/api/historicos")
public class HistoricoController {

    private final HistoricoClimaService historicoService;

    public HistoricoController(HistoricoClimaService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping("/serie")
    public ResponseEntity<?> consultarSerie(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Double latitud,
            @RequestParam(required = false) Double longitud) {
        if ((latitud == null) != (longitud == null)) {
            return ResponseEntity.badRequest()
                .body(Map.of("mensaje", "Envía latitud y longitud juntas, o deja ambas sin indicar."));
        }
        try {
            SerieHistoricaResponse respuesta = historicoService.consultarSerie(desde, hasta, latitud, longitud);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        }
    }
}
