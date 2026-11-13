package com.tecsup.clima.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Acceso de escritura a HISTORIAL_NOTIFICACION.
 *
 * <p>La entidad existente usa id asignado manualmente (sin generacion), por lo
 * que se calcula el siguiente identificador antes de registrar cada envio.</p>
 */
public interface HistorialNotificacionRegistroRepository
        extends JpaRepository<HistorialNotificacionEntity, Integer> {

    @Query("select coalesce(max(h.id), 0) from HistorialNotificacionEntity h")
    Integer obtenerUltimoId();
}
