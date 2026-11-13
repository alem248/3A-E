package com.tecsup.clima.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.service.PreferenciaAlertaService;

/** Vista Thymeleaf para activar o desactivar alertas por nivel de riesgo. */
@Controller
@RequestMapping("/preferencias")
public class PreferenciasViewController {

    private final PreferenciaAlertaService preferenciaService;

    public PreferenciasViewController(PreferenciaAlertaService preferenciaService) {
        this.preferenciaService = preferenciaService;
    }

    @GetMapping
    public String pantalla(@RequestParam(name = "usuarioId", defaultValue = "1") Integer usuarioId,
                           Model model) {
        model.addAttribute("usuarioId", usuarioId);

        try {
            UsuarioEntity usuario = preferenciaService.obtenerUsuario(usuarioId);
            model.addAttribute("usuario", usuario);
            model.addAttribute("preferencias", preferenciaService.listar(usuarioId));
        } catch (IllegalArgumentException error) {
            model.addAttribute("error", error.getMessage());
            model.addAttribute("preferencias", List.of());
        }

        return "preferencias";
    }
}
