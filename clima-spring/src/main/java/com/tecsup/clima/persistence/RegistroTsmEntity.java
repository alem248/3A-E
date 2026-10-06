package com.tecsup.clima.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DER: REGISTRO_TSM. Lectura de Temperatura Superficial del Mar medida por una
 * estacion durante un evento climatico. El campo {@code anomalia} representa la
 * diferencia respecto del valor baseclimatico (intensidad del Nino Costero).
 */
@Entity
@Table(name = "registro_tsm")
public class RegistroTsmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "evento_id")
    private EventoClimaticoEntity evento;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estacion_id")
    private EstacionMonitoreoEntity estacion;

    @Column(name = "latitud")
    private BigDecimal latitud;

    @Column(name = "longitud")
    private BigDecimal longitud;

    @Column(name = "anomalia")
    private BigDecimal anomalia;

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

    public EstacionMonitoreoEntity getEstacion() {
        return estacion;
    }

    public void setEstacion(EstacionMonitoreoEntity estacion) {
        this.estacion = estacion;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public BigDecimal getAnomalia() {
        return anomalia;
    }

    public void setAnomalia(BigDecimal anomalia) {
        this.anomalia = anomalia;
    }
}