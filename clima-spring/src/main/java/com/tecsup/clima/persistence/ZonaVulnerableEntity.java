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

<<<<<<< HEAD
    @Column(name = "prioritaria", nullable = false, columnDefinition = "boolean default false")
    private boolean prioritaria = false;

=======
    @Column(name = "nombre", length = 120)
    private String nombre;

    @Column(name = "departamento", length = 80)
    private String departamento;

    @Column(name = "prioritaria", nullable = false, columnDefinition = "boolean default false")
    private boolean prioritaria = false;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

>>>>>>> feat(spring): entidades y repositorios de zonas vulnerables y TSM
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

    public boolean isPrioritaria() {
        return prioritaria;
    }

    public void setPrioritaria(boolean prioritaria) {
        this.prioritaria = prioritaria;
    }
}
