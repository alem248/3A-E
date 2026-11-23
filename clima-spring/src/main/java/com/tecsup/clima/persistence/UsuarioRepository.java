package com.tecsup.clima.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: USUARIO. */
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {

    Optional<UsuarioEntity> findByEmail(String email);
}
