package com.tecsup.clima.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * DER: FUENTE_OFICIAL. Institucion que provee los datos climaticos
 * (por ejemplo Open-Meteo) y su endpoint oficial.
 */
@Entity
@Table(name = "fuente_oficial")
public class FuenteOficialEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "nombre_institucion")
    private String nombreInstitucion;

    @Column(name = "url_api")
    private String urlApi;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombreInstitucion() {
        return nombreInstitucion;
    }

    public void setNombreInstitucion(String nombreInstitucion) {
        this.nombreInstitucion = nombreInstitucion;
    }

    public String getUrlApi() {
        return urlApi;
    }

    public void setUrlApi(String urlApi) {
        this.urlApi = urlApi;
    }
}