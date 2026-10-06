package com.tecsup.clima.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DER: MEDIDA_PREVENTIVA. Accion preventiva detallada en una alerta
 * (evacuacion,afilamiento de ayuda, etc.).
 */
@Entity
@Table(name = "medida_preventiva")
public class MedidaPreventivaEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "alerta_id")
    private AlertaPreventivaEntity alerta;

    @Column(name = "descripcion_accion")
    private String descripcionAccion;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AlertaPreventivaEntity getAlerta() {
        return alerta;
    }

    public void setAlerta(AlertaPreventivaEntity alerta) {
        this.alerta = alerta;
    }

    public String getDescripcionAccion() {
        return descripcionAccion;
    }

    public void setDescripcionAccion(String descripcionAccion) {
        this.descripcionAccion = descripcionAccion;
    }
}