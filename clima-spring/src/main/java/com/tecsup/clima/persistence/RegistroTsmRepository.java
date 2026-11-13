package com.tecsup.clima.persistence;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: REGISTRO_TSM. */
public interface RegistroTsmRepository extends JpaRepository<RegistroTsmEntity, Integer> {

    List<RegistroTsmEntity> findByEvento(EventoClimaticoEntity evento);

    List<RegistroTsmEntity> findByEventoAndEstacion(EventoClimaticoEntity evento, EstacionMonitoreoEntity estacion);

    List<RegistroTsmEntity> findByEstacion_IdAndFechaObservacionBetweenOrderByFechaObservacionAsc(
        Integer estacionId, LocalDateTime desde, LocalDateTime hasta);
}
