package com.tecsup.clima.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DER: PROYECCION_TRAYECTORIA. Posicion futura estimada del evento climatico,
 * es decir la trayectoria del Niño Costero.
 */
@Entity
@Table(name = "proyeccion_trayectoria")
public class ProyeccionTrayectoriaEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "evento_id")
    private EventoClimaticoEntity evento;

    @Column(name = "lat_futura")
    private BigDecimal latFutura;

    @Column(name = "lon_futura")
    private BigDecimal lonFutura;

    @Column(name = "fecha_estimada")
    private LocalDate fechaEstimada;

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

    public BigDecimal getLatFutura() {
        return latFutura;
    }

    public void setLatFutura(BigDecimal latFutura) {
        this.latFutura = latFutura;
    }

    public BigDecimal getLonFutura() {
        return lonFutura;
    }

    public void setLonFutura(BigDecimal lonFutura) {
        this.lonFutura = lonFutura;
    }

    public LocalDate getFechaEstimada() {
        return fechaEstimada;
    }

    public void setFechaEstimada(LocalDate fechaEstimada) {
        this.fechaEstimada = fechaEstimada;
    }
}