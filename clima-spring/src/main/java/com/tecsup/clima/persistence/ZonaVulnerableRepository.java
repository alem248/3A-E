package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: ZONA_VULNERABLE. */
public interface ZonaVulnerableRepository extends JpaRepository<ZonaVulnerableEntity, Integer> {

    List<ZonaVulnerableEntity> findAllByOrderByIdAsc();

    List<ZonaVulnerableEntity> findByNivelRiesgo(NivelRiesgoEntity nivelRiesgo);
}