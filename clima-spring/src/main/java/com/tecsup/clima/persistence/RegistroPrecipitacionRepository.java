package com.tecsup.clima.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio DER: REGISTRO_PRECIPITACION. */
public interface RegistroPrecipitacionRepository extends JpaRepository<RegistroPrecipitacionEntity, Integer> {

    List<RegistroPrecipitacionEntity> findByEvento(EventoClimaticoEntity evento);
}