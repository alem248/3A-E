package com.tecsup.clima.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DER: ALERTA_PREVENTIVA. Alerta presentada en una zona vulnerable y
 * categorizada por un nivel de riesgo.
 */
@Entity
@Table(name = "alerta_preventiva")
public class AlertaPreventivaEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "zona_id")
    private ZonaVulnerableEntity zona;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "nivel_riesgo_id")
    private NivelRiesgoEntity nivelRiesgo;

    @Column(name = "mensaje")
    private String mensaje;

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

    public NivelRiesgoEntity getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(NivelRiesgoEntity nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}