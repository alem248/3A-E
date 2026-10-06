package com.tecsup.clima.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: EVENTO_CLIMATICO. */
public interface EventoClimaticoRepository extends JpaRepository<EventoClimaticoEntity, Integer> {

    Optional<EventoClimaticoEntity> findFirstByEstadoIgnoreCase(String estado);

    List<EventoClimaticoEntity> findByEstadoIgnoreCase(String estado);
}