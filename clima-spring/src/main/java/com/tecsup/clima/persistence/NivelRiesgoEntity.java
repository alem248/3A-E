package com.tecsup.clima.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * DER: NIVEL_RIESGO. Catalogo de semaforo termico (verde, amarillo, naranja,
 * rojo y sin datos). Los umbrales numericos viven en el dominio
 * ({@link com.tecsup.clima.domain.NivelRiesgo}).
 */
@Entity
@Table(name = "nivel_riesgo")
public class NivelRiesgoEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "color_hex")
    private String colorHex;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}