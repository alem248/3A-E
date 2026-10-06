package com.tecsup.clima.persistence;

import java.time.LocalDateTime;

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
 * Lectura historica de Temperatura Superficial del Mar (TSM) de una zona.
 * Relacion 1:N (una zona tiene muchas lecturas) y permite conservar la
 * trayectoria temporal de la temperatura.
 */
@Entity
@Table(name = "lectura_tsm")
public class LecturaTsmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zona_id", nullable = false)
    private ZonaTermicaEntity zona;

    @Column(name = "tsm")
    private Double tsm;

    @Column(name = "nivel_riesgo", length = 32)
    private String nivelRiesgo;

    @Column(name = "actualizado_en", length = 40)
    private String actualizadoEn;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZonaTermicaEntity getZona() {
        return zona;
    }

    public void setZona(ZonaTermicaEntity zona) {
        this.zona = zona;
    }

    public Double getTsm() {
        return tsm;
    }

    public void setTsm(Double tsm) {
        this.tsm = tsm;
    }

    public String getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(String nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public String getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(String actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }

    public LocalDateTime getRegistradoEn() {
        return registradoEn;
    }

    public void setRegistradoEn(LocalDateTime registradoEn) {
        this.registradoEn = registradoEn;
    }
}
