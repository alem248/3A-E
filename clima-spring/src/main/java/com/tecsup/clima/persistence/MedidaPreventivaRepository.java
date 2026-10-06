package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: MEDIDA_PREVENTIVA. */
public interface MedidaPreventivaRepository extends JpaRepository<MedidaPreventivaEntity, Integer> {

    List<MedidaPreventivaEntity> findByAlerta(AlertaPreventivaEntity alerta);
}