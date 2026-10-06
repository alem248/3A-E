package com.tecsup.clima.persistence;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Entidad que representa una zona geografica de la costa peruana.
 *
 * <p>En PostgreSQL/PostGIS la columna espacial {@code geom geometry(Polygon,4326)}
 * se crea y puebla desde {@code poligono_wkt} mediante el script
 * {@code db/postgres/schema.sql}. El modelo JPA mantiene los vertices en la tabla
 * {@code zona_poligono} para que el proyecto tambien funcione en el perfil {@code dev} (H2).</p>
 */
@Entity
@Table(name = "zona_termica")
public class ZonaTermicaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "departamento", nullable = false, length = 120)
    private String departamento;

    @Column(name = "latitud", nullable = false)
    private Double latitud;

    @Column(name = "longitud", nullable = false)
    private Double longitud;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "poligono_wkt", length = 1000)
    private String poligonoWkt;

    @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<ZonaPoligonoEntity> poligono = new ArrayList<>();

    public ZonaTermicaEntity() {
    }

    public void agregarVertice(double latitudPunto, double longitudPunto) {
        ZonaPoligonoEntity vertice = new ZonaPoligonoEntity();
        vertice.setZona(this);
        vertice.setOrden(this.poligono.size());
        vertice.setLatitud(latitudPunto);
        vertice.setLongitud(longitudPunto);
        this.poligono.add(vertice);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public String getPoligonoWkt() {
        return poligonoWkt;
    }

    public void setPoligonoWkt(String poligonoWkt) {
        this.poligonoWkt = poligonoWkt;
    }

    public List<ZonaPoligonoEntity> getPoligono() {
        return poligono;
    }

    public void setPoligono(List<ZonaPoligonoEntity> poligono) {
        this.poligono = poligono;
    }
}
