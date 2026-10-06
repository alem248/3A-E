package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: UBICACION_USUARIO. */
public interface UbicacionUsuarioRepository extends JpaRepository<UbicacionUsuarioEntity, Integer> {

    List<UbicacionUsuarioEntity> findByUsuarioOrderByIdDesc(UsuarioEntity usuario);
}