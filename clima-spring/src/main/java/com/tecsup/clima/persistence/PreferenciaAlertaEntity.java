package com.tecsup.clima.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Preferencia de notificaciones de un usuario por nivel de riesgo (HU-2).
 *
 * <p>Es una tabla nueva y aditiva: si un usuario no tiene fila para un nivel,
 * se considera que la alerta esta activa por defecto.</p>
 */
@Entity
@Table(name = "preferencia_alerta",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_preferencia_alerta_usuario_nivel",
        columnNames = {"usuario_id", "nivel_riesgo_id"}))
public class PreferenciaAlertaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "nivel_riesgo_id", nullable = false)
    private NivelRiesgoEntity nivelRiesgo;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

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

    public NivelRiesgoEntity getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(NivelRiesgoEntity nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
