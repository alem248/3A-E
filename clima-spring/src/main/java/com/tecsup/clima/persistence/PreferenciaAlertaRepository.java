package com.tecsup.clima.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de preferencias de alertas por nivel de riesgo (HU-2). */
public interface PreferenciaAlertaRepository extends JpaRepository<PreferenciaAlertaEntity, Integer> {

    Optional<PreferenciaAlertaEntity> findByUsuarioAndNivelRiesgo(UsuarioEntity usuario,
                                                                  NivelRiesgoEntity nivelRiesgo);

    List<PreferenciaAlertaEntity> findByUsuario(UsuarioEntity usuario);
}
