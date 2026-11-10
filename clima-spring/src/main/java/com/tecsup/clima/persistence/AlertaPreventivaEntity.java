package com.tecsup.clima.persistence;

import java.time.OffsetDateTime;

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
 * DER: ALERTA_PREVENTIVA. Alerta presentada en una zona vulnerable y
 * categorizada por un nivel de riesgo.
 *
 * <p>Ademas del DER guarda {@code fecha_emision} y {@code vigencia_hasta}, que el
 * motor de reglas usa para no duplicar una alerta mientras siga vigente.</p>
 */
@Entity
@Table(name = "alerta_preventiva")
public class AlertaPreventivaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "zona_id")
    private ZonaVulnerableEntity zona;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "nivel_riesgo_id")
    private NivelRiesgoEntity nivelRiesgo;

    @Column(name = "mensaje", length = 1000)
    private String mensaje;

    @Column(name = "fecha_emision")
    private OffsetDateTime fechaEmision;

    @Column(name = "vigencia_hasta")
    private OffsetDateTime vigenciaHasta;

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

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(OffsetDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public OffsetDateTime getVigenciaHasta() {
        return vigenciaHasta;
    }

    public void setVigenciaHasta(OffsetDateTime vigenciaHasta) {
        this.vigenciaHasta = vigenciaHasta;
    }
}