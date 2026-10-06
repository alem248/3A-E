package com.tecsup.clima.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades geometricas para los poligonos de las zonas vulnerables.
 *
 * <p>El DER almacena el poligono de {@code zona_vulnerable} como texto (WKT).
 * Estas funciones permiten convertirlo a la lista de pares [latitud, longitud]
 * que consume Leaflet, filtrar por zona geografica y obtener el punto de
 * referencia de la zona.</p>
 */
public final class PoligonoUtils {

    private PoligonoUtils() {
    }

    /**
     * Convierte el poligono a WKT para la columna de texto del DER.
     * Formato: POLYGON((lon lat, lon lat, ...)), con el anillo cerrado.
     */
    public static String aWkt(List<List<Double>> poligono) {
        if (poligono == null || poligono.isEmpty()) {
            return null;
        }

        List<List<Double>> anillo = new ArrayList<>(poligono);

        // PostGIS exige que el anillo este cerrado (primer punto = ultimo).
        List<Double> primero = anillo.get(0);
        List<Double> ultimo = anillo.get(anillo.size() - 1);

        if (!primero.get(0).equals(ultimo.get(0)) || !primero.get(1).equals(ultimo.get(1))) {
            anillo.add(primero);
        }

        StringBuilder wkt = new StringBuilder("POLYGON((");

        for (int i = 0; i < anillo.size(); i++) {
            if (i > 0) {
                wkt.append(", ");
            }

            // WKT usa el orden X=longitud, Y=latitud.
            wkt.append(anillo.get(i).get(1)).append(" ").append(anillo.get(i).get(0));
        }

        return wkt.append("))").toString();
    }

    /**
     * Convierte el WKT almacenado en el DER a pares [latitud, longitud].
     */
    public static List<List<Double>> desdeWkt(String wkt) {
        List<List<Double>> poligono = new ArrayList<>();

        if (wkt == null || wkt.isEmpty()) {
            return poligono;
        }

        try {
            int inicio = wkt.indexOf("((");
            int fin = wkt.lastIndexOf("))");

            if (inicio < 0 || fin < 0) {
                return poligono;
            }

            String cuerpo = wkt.substring(inicio + 2, fin);

            for (String punto : cuerpo.split(",")) {
                String[] partes = punto.trim().split("\\s+");

                if (partes.length < 2) {
                    continue;
                }

                // El WKT guarda "longitud latitud".
                poligono.add(List.of(Double.parseDouble(partes[1]), Double.parseDouble(partes[0])));
            }
        } catch (NumberFormatException error) {
            return new ArrayList<>();
        }

        // PostGIS/WKT cierra el anillo repitiendo el primer punto; Leaflet no lo necesita.
        if (poligono.size() > 1
            && poligono.get(0).get(0).equals(poligono.get(poligono.size() - 1).get(0))
            && poligono.get(0).get(1).equals(poligono.get(poligono.size() - 1).get(1))) {
            poligono.remove(poligono.size() - 1);
        }

        return poligono;
    }

    /**
     * Indica si un punto (latitud, longitud) cae dentro del poligono.
     * Algoritmo ray casting (par/impar).
     */
    public static boolean contiene(List<List<Double>> poligono, Double latitud, Double longitud) {
        if (latitud == null || longitud == null || poligono == null || poligono.size() < 3) {
            return false;
        }

        boolean dentro = false;
        int total = poligono.size();

        for (int i = 0, j = total - 1; i < total; j = i++) {
            // Vertices: obtener(0) = latitud (y), obtener(1) = longitud (x).
            double lati = poligono.get(i).get(0);
            double loni = poligono.get(i).get(1);
            double latj = poligono.get(j).get(0);
            double lonj = poligono.get(j).get(1);

            boolean cruza = ((lati > latitud) != (latj > latitud))
                && (longitud < (lonj - loni) * (latitud - lati) / (latj - lati) + loni);

            if (cruza) {
                dentro = !dentro;
            }
        }

        return dentro;
    }

    /**
     * Punto representativo de la zona (centroide del poligono), usado para
     * consultar la temperatura de la fuente oficial.
     */
    public static double[] centroide(List<List<Double>> poligono) {
        if (poligono == null || poligono.isEmpty()) {
            return null;
        }

        double sumaLatitud = 0;
        double sumaLongitud = 0;

        for (List<Double> punto : poligono) {
            sumaLatitud += punto.get(0);
            sumaLongitud += punto.get(1);
        }

        return new double[] { sumaLatitud / poligono.size(), sumaLongitud / poligono.size() };
    }
}