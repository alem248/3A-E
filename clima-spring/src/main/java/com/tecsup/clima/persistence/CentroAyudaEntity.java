package com.tecsup.clima.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DER: CENTRO_AYUDA. Punto de ayuda (albergue, hospital, mando) ubicado dentro
 * de una zona vulnerable.
 */
@Entity
@Table(name = "centro_ayuda")
public class CentroAyudaEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "zona_id")
    private ZonaVulnerableEntity zona;

    @Column(name = "tipo")
    private String tipo;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ZonaVulnerableEntity getZona() {
        return zona;
    }

    public void setZona(ZonaVulnerableEntity zona) {
        this.zona = zona;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}