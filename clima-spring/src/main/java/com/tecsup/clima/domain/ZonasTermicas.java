package com.tecsup.clima.domain;

import java.util.Arrays;
import java.util.List;

public final class ZonasTermicas {

    private ZonasTermicas() {
    }

    public static List<ZonaTermica> getZonas() {
        return Arrays.asList(
            zona("tumbes", "Tumbes", "Tumbes", -3.62, -81.0,
                new double[][]{{-3.27, -80.75}, {-3.27, -81.25}, {-3.97, -81.25}, {-3.97, -80.75}}),
            zona("piura-norte", "Piura Norte", "Piura", -4.35, -81.35,
                new double[][]{{-4.0, -81.1}, {-4.0, -81.6}, {-4.7, -81.6}, {-4.7, -81.1}}),
            zona("piura-sur", "Piura Sur (Paita)", "Piura", -5.25, -81.6,
                new double[][]{{-4.9, -81.35}, {-4.9, -81.85}, {-5.6, -81.85}, {-5.6, -81.35}}),
            zona("lambayeque", "Lambayeque", "Lambayeque", -6.6, -80.6,
                new double[][]{{-6.25, -80.35}, {-6.25, -80.85}, {-6.95, -80.85}, {-6.95, -80.35}}),
            zona("la-libertad-norte", "La Libertad Norte", "La Libertad", -7.35, -79.95,
                new double[][]{{-7.0, -79.7}, {-7.0, -80.2}, {-7.7, -80.2}, {-7.7, -79.7}}),
            zona("la-libertad-sur", "La Libertad Sur (Chimbote)", "La Libertad", -9.1, -79.1,
                new double[][]{{-8.75, -78.85}, {-8.75, -79.35}, {-9.45, -79.35}, {-9.45, -78.85}})
        );
    }

    private static ZonaTermica zona(String id, String nombre, String departamento,
                                    double lat, double lon, double[][] puntos) {
        return new ZonaTermica(id, nombre, departamento, lat, lon,
            aLista(puntos), null, null, NivelRiesgo.SIN_DATOS, false);
    }

    private static List<List<Double>> aLista(double[][] puntos) {
        List<List<Double>> poligono = new java.util.ArrayList<>();

        for (double[] punto : puntos) {
            poligono.add(Arrays.asList(punto[0], punto[1]));
        }

        return poligono;
    }
}