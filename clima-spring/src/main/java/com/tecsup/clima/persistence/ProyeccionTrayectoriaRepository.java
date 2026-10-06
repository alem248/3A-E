package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: PROYECCION_TRAYECTORIA. */
public interface ProyeccionTrayectoriaRepository extends JpaRepository<ProyeccionTrayectoriaEntity, Integer> {

    List<ProyeccionTrayectoriaEntity> findByEventoOrderByFechaEstimadaAsc(EventoClimaticoEntity evento);
}