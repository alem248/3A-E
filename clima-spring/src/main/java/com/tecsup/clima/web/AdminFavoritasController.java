package com.tecsup.clima.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.persistence.UbicacionFavoritaRepository;
import com.tecsup.clima.persistence.UbicacionFavoritaRepository.RankingFavorita;

/** Ranking agregado de lugares favoritos (solo ADMIN). No expone usuarios ni coordenadas. */
@RestController
@RequestMapping("/api/admin/favoritas")
public class AdminFavoritasController {

    private static final int LIMITE = 20;

    private final UbicacionFavoritaRepository repository;

    public AdminFavoritasController(UbicacionFavoritaRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/ranking")
    public List<Map<String, Object>> ranking() {
        return repository.rankingGlobal().stream()
                .limit(LIMITE)
                .map(this::fila)
                .toList();
    }

    private Map<String, Object> fila(RankingFavorita favorita) {
        return Map.of("nombre", favorita.getNombre(), "veces", favorita.getVeces());
    }
}
