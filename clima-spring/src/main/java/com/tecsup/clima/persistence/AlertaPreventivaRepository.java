package com.tecsup.clima.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: ALERTA_PREVENTIVA. */
public interface AlertaPreventivaRepository extends JpaRepository<AlertaPreventivaEntity, Integer> {

    List<AlertaPreventivaEntity> findByZona(ZonaVulnerableEntity zona);

    List<AlertaPreventivaEntity> findByNivelRiesgo(NivelRiesgoEntity nivelRiesgo);

    /** Alerta aun vigente de la zona con ese nivel (evita duplicados del motor de reglas). */
    Optional<AlertaPreventivaEntity> findFirstByZonaIdAndNivelRiesgoIdAndVigenciaHastaAfterOrderByFechaEmisionDesc(
        Integer zonaId, Integer nivelRiesgoId, OffsetDateTime ahora);

    List<AlertaPreventivaEntity> findByVigenciaHastaAfterOrderByFechaEmisionDesc(OffsetDateTime ahora);

    List<AlertaPreventivaEntity> findByZonaIdAndVigenciaHastaAfterOrderByFechaEmisionDesc(
        Integer zonaId, OffsetDateTime ahora);
}