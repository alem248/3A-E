package com.tecsup.clima.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DER: REGISTRO_PRECIPITACION. Milimetros de precipitacion medidos por una
 * estacion durante un evento climatico.
 */
@Entity
@Table(name = "registro_precipitacion")
public class RegistroPrecipitacionEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "evento_id")
    private EventoClimaticoEntity evento;

    @Column(name = "milimetros")
    private BigDecimal milimetros;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EventoClimaticoEntity getEvento() {
        return evento;
    }

    public void setEvento(EventoClimaticoEntity evento) {
        this.evento = evento;
    }

    public BigDecimal getMilimetros() {
        return milimetros;
    }

    public void setMilimetros(BigDecimal milimetros) {
        this.milimetros = milimetros;
    }
}