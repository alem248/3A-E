package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de zonas termicas. Permite persistir y consultar las zonas
 * del modelo de datos HU-1 (PostgreSQL/PostGIS en el perfil productivo).
 */
public interface ZonaTermicaRepository extends JpaRepository<ZonaTermicaEntity, String> {

    List<ZonaTermicaEntity> findAllByOrderByOrdenAsc();
}
