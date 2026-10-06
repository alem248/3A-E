package com.tecsup.clima.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: ROL. */
public interface RolRepository extends JpaRepository<RolEntity, Integer> {

    Optional<RolEntity> findByNombre(String nombre);
}