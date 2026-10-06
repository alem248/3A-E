package com.tecsup.clima.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DER: ZONA_VULNERABLE. Zona geografica de la costa clasificada por nivel de
 * riesgo.
 *
 * <p>Segun el DER el poligono se guarda como texto (WKT). En el perfil
 * {@code postgres} se agrega la columna espacial {@code geom geometry(Polygon,4326)}
 * mediante {@code db/postgres/schema.sql}, indexada con GIST.</p>
 */
@Entity
@Table(name = "zona_vulnerable")
public class ZonaVulnerableEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nivel_riesgo_id")
    private NivelRiesgoEntity nivelRiesgo;

    @Column(name = "poligono", length = 2000)
    private String poligono;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public NivelRiesgoEntity getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(NivelRiesgoEntity nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public String getPoligono() {
        return poligono;
    }

    public void setPoligono(String poligono) {
        this.poligono = poligono;
    }
}