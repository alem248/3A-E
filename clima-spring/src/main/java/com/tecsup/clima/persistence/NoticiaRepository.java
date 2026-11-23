package com.tecsup.clima.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: NOTICIA. */
public interface NoticiaRepository extends JpaRepository<NoticiaEntity, Integer> {

    List<NoticiaEntity> findAllByOrderByFechaCreacionDesc();

    List<NoticiaEntity> findByEstadoOrderByFechaCreacionDesc(NoticiaEntity.Estado estado);

    Optional<NoticiaEntity> findFirstByTitulo(String titulo);
}
