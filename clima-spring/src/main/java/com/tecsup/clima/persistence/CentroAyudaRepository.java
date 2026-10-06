package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: CENTRO_AYUDA. */
public interface CentroAyudaRepository extends JpaRepository<CentroAyudaEntity, Integer> {

    List<CentroAyudaEntity> findByZona(ZonaVulnerableEntity zona);
}