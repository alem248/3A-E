package com.tecsup.clima.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.service.AdminHistoryService;

@RestController
@RequestMapping("/api/v1/admin/zonas")
public class AdminHistoryController {
    private final AdminHistoryService history;

    public AdminHistoryController(AdminHistoryService history) { this.history = history; }

    @GetMapping("/{zoneId}/historial")
    public AdminHistoryResponse historial(@PathVariable int zoneId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return history.consultar(zoneId, desde, hasta);
    }
}
