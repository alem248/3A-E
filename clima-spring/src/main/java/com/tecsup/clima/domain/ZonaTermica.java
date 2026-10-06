package com.tecsup.clima.domain;

import java.util.List;

public class ZonaTermica {

    private final String id;
    private final String nombre;
    private final String departamento;
    private final Double latitude;
    private final Double longitude;
    private final List<List<Double>> poligono;
    private final Double tsm;
    private final String actualizadoEn;
    private final NivelRiesgo nivel;
    private final boolean disponible;

    public ZonaTermica(String id, String nombre, String departamento,
                       Double latitude, Double longitude, List<List<Double>> poligono,
                       Double tsm, String actualizadoEn, NivelRiesgo nivel, boolean disponible) {
        this.id = id;
        this.nombre = nombre;
        this.departamento = departamento;
        this.latitude = latitude;
        this.longitude = longitude;
        this.poligono = poligono;
        this.tsm = tsm;
        this.actualizadoEn = actualizadoEn;
        this.nivel = nivel;
        this.disponible = disponible;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDepartamento() {
        return departamento;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public List<List<Double>> getPoligono() {
        return poligono;
    }

    public Double getTsm() {
        return tsm;
    }

    public String getActualizadoEn() {
        return actualizadoEn;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public boolean isDisponible() {
        return disponible;
    }

    /**
     * Indica si un punto (latitud, longitud) cae dentro del poligono de la zona.
     * Delega en {@link PoligonoUtils#contiene} (ray casting).
     */
    public boolean contiene(Double puntoLatitud, Double puntoLongitud) {
        return PoligonoUtils.contiene(poligono, puntoLatitud, puntoLongitud);
    }

    /**
     * Devuelve el poligono en formato WKT (SRID 4326) para almacenarlo en
     * {@code zona_vulnerable.poligono} y poblar la columna PostGIS {@code geom}.
     */
    public String poligonoWkt() {
        return PoligonoUtils.aWkt(poligono);
    }
}