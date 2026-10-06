package com.tecsup.clima.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: NIVEL_RIESGO. */
public interface NivelRiesgoRepository extends JpaRepository<NivelRiesgoEntity, Integer> {

    Optional<NivelRiesgoEntity> findByNombre(String nombre);
}