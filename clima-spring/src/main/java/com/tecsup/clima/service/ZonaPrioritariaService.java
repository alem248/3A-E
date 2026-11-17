package com.tecsup.clima.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;
import com.tecsup.clima.web.dto.PrioridadZonaResponse;

/** Consulta y actualiza el orden de prioridad de las zonas del panel. */
@Service
public class ZonaPrioritariaService {

    private final ZonaVulnerableRepository zonas;

    public ZonaPrioritariaService(ZonaVulnerableRepository zonas) {
        this.zonas = zonas;
    }

    @Transactional(readOnly = true)
    public List<PrioridadZonaResponse> listarOrdenadas() {
        return zonas.findAllByOrderByPrioritariaDescIdAsc().stream()
            .map(this::aRespuesta)
            .toList();
    }

    @Transactional
    public PrioridadZonaResponse actualizar(int zonaId, boolean prioritaria) {
        ZonaVulnerableEntity zona = zonas.findById(zonaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("La zona " + zonaId + " no existe"));
        zona.setPrioritaria(prioritaria);
        return aRespuesta(zonas.save(zona));
    }

    private PrioridadZonaResponse aRespuesta(ZonaVulnerableEntity zona) {
        return new PrioridadZonaResponse(zona.getId(), zona.isPrioritaria());
    }
}
