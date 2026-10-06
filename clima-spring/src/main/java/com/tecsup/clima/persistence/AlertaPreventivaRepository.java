package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: ALERTA_PREVENTIVA. */
public interface AlertaPreventivaRepository extends JpaRepository<AlertaPreventivaEntity, Integer> {

    List<AlertaPreventivaEntity> findByZona(ZonaVulnerableEntity zona);

    List<AlertaPreventivaEntity> findByNivelRiesgo(NivelRiesgoEntity nivelRiesgo);
}