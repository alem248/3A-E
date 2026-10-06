package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: HISTORIAL_NOTIFICACION. */
public interface HistorialNotificacionRepository extends JpaRepository<HistorialNotificacionEntity, Integer> {

    List<HistorialNotificacionEntity> findByUsuarioOrderByIdDesc(UsuarioEntity usuario);

    List<HistorialNotificacionEntity> findByAlerta(AlertaPreventivaEntity alerta);
}