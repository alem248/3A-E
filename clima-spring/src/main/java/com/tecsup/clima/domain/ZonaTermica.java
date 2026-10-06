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
     * Implementa el algoritmo "ray casting" (par/impar), suficiente para los
     * poligonos rectangulares y convexos usados por la costa peruana.
     */
    public boolean contiene(Double puntoLatitud, Double puntoLongitud) {
        if (puntoLatitud == null || puntoLongitud == null) {
            return false;
        }

        if (poligono == null || poligono.size() < 3) {
            return false;
        }

        boolean dentro = false;
        int total = poligono.size();

        for (int i = 0, j = total - 1; i < total; j = i++) {
            // Vértices: obtener(0) = latitud (y), obtener(1) = longitud (x).
            double lati = poligono.get(i).get(0);
            double loni = poligono.get(i).get(1);
            double latj = poligono.get(j).get(0);
            double lonj = poligono.get(j).get(1);

            boolean cruza = ((lati > puntoLatitud) != (latj > puntoLatitud))
                && (puntoLongitud < (lonj - loni) * (puntoLatitud - lati) / (latj - lati) + loni);

            if (cruza) {
                dentro = !dentro;
            }
        }

        return dentro;
    }

    /**
     * Devuelve el poligono en formato WKT (SRID 4326) para insertarlo en la
     * columna espacial PostGIS, por ejemplo: POLYGON((lon lat, ...)).
     */
    public String poligonoWkt() {
        if (poligono == null || poligono.isEmpty()) {
            return null;
        }

        StringBuilder wkt = new StringBuilder("POLYGON((");
        List<List<Double>> anillo = new java.util.ArrayList<>(poligono);

        // PostGIS exige que el anillo este cerrado (primer punto = ultimo).
        if (anillo.size() > 0) {
            List<Double> primero = anillo.get(0);
            List<Double> ultimo = anillo.get(anillo.size() - 1);

            if (!primero.get(0).equals(ultimo.get(0)) || !primero.get(1).equals(ultimo.get(1))) {
                anillo.add(primero);
            }
        }

        for (int i = 0; i < anillo.size(); i++) {
            if (i > 0) {
                wkt.append(", ");
            }

            // WKT usa el orden X=longitud, Y=latitud.
            wkt.append(anillo.get(i).get(1)).append(" ").append(anillo.get(i).get(0));
        }

        return wkt.append("))").toString();
    }
}