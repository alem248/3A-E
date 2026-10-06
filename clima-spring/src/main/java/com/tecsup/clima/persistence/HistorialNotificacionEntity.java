package com.tecsup.clima.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DER: HISTORIAL_NOTIFICACION. Notificacion enviada a un usuario por una alerta
 * preventiva, con su estado de lectura.
 */
@Entity
@Table(name = "historial_notificacion")
public class HistorialNotificacionEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "alerta_id")
    private AlertaPreventivaEntity alerta;

    @Column(name = "leida")
    private boolean leida;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public AlertaPreventivaEntity getAlerta() {
        return alerta;
    }

    public void setAlerta(AlertaPreventivaEntity alerta) {
        this.alerta = alerta;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }
}