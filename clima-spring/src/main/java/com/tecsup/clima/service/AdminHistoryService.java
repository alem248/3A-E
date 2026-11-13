package com.tecsup.clima.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.domain.NivelRiesgo;
import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.RegistroTsmEntity;
import com.tecsup.clima.persistence.RegistroTsmRepository;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;
import com.tecsup.clima.web.AdminHistoryResponse;

@Service
public class AdminHistoryService {
    private static final double BASE_CLIMATICA = 24.0;
    private final RegistroTsmRepository registros;
    private final ZonaVulnerableRepository zonas;

    public AdminHistoryService(RegistroTsmRepository registros, ZonaVulnerableRepository zonas) {
        this.registros = registros;
        this.zonas = zonas;
    }

    @Transactional(readOnly = true)
    public AdminHistoryResponse consultar(int zoneId, LocalDate desde, LocalDate hasta) {
        if (!zonas.existsById(zoneId)) throw new RecursoNoEncontradoException("Zona no encontrada");
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(30);
        if (inicio.isAfter(fin) || inicio.isBefore(fin.minusYears(5))) {
            throw new DatoInvalidoException("El rango debe ser válido y no superar cinco años");
        }
        List<AdminHistoryResponse.PuntoHistorial> puntos = registros
            .findByEstacion_IdAndFechaObservacionBetweenOrderByFechaObservacionAsc(
                zoneId, inicio.atStartOfDay(), fin.atTime(LocalTime.MAX))
            .stream().limit(5000)
            .map(this::comoPunto).toList();
        ZonasTermicas.Zona zona = ZonasTermicas.getZona(zoneId);
        boolean alerta = puntos.stream().anyMatch(p -> p.valor() >= 28.0);
        return new AdminHistoryResponse(Integer.toString(zoneId), zona != null ? zona.getNombre() : Integer.toString(zoneId),
            "°C", NivelRiesgo.ROJO.getUmbralInferior(), alerta, puntos);
    }

    private AdminHistoryResponse.PuntoHistorial comoPunto(RegistroTsmEntity registro) {
        return new AdminHistoryResponse.PuntoHistorial(registro.getFechaObservacion().toString(),
            registro.getAnomalia().doubleValue() + BASE_CLIMATICA);
    }
}
