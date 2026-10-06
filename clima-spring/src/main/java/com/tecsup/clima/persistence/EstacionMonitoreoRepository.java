package com.tecsup.clima.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: ESTACION_MONITOREO. */
public interface EstacionMonitoreoRepository extends JpaRepository<EstacionMonitoreoEntity, Integer> {

    List<EstacionMonitoreoEntity> findByFuente(FuenteOficialEntity fuente);

    Optional<EstacionMonitoreoEntity> findFirstByOrderByIdAsc();
}