package com.tecsup.clima.persistence;

import java.math.BigDecimal;
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

/** Ubicacion que el usuario guarda con un nombre propio (casa, trabajo, playa). */
@Entity
@Table(name = "ubicacion_favorita")
public class UbicacionFavoritaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "etiqueta", nullable = false, length = 60)
    private String etiqueta;

    @Column(name = "latitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    public UbicacionFavoritaEntity() {
    }

    public UbicacionFavoritaEntity(UsuarioEntity usuario, String etiqueta, BigDecimal latitud, BigDecimal longitud) {
        this.usuario = usuario;
        this.etiqueta = etiqueta;
        this.latitud = latitud;
        this.longitud = longitud;
        this.fechaRegistro = OffsetDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
