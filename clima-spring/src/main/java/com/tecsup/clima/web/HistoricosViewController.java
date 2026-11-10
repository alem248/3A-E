package com.tecsup.clima.web;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Sirve la pantalla aislada de tendencias históricas. */
@Controller
public class HistoricosViewController {

    @GetMapping("/historicos")
    public String historicos(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate ultimoHistorico = hoy.minusDays(7);
        model.addAttribute("fechaMaxima", hoy);
        model.addAttribute("fechaHasta", ultimoHistorico);
        model.addAttribute("fechaDesde", ultimoHistorico.minusDays(29));
        model.addAttribute("fechaComparar", ultimoHistorico);
        return "historicos";
    }
}
