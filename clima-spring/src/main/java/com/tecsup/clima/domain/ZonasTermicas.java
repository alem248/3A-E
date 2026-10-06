package com.tecsup.clima.domain;

import java.util.List;

/**
 * Catalogo de zonas vulnerables de la costa peruana (costa norte).
 *
 * <p>Se usa para la carga inicial de {@code zona_vulnerable} y
 * {@code estacion_monitoreo} en la base de datos (ver
 * {@code config/DatosInicialesLoader}).</p>
 */
public final class ZonasTermicas {

    private ZonasTermicas() {
    }

    /** Zona del catalogo: identificador, nombre y poligono. */
    public static final class Zona {
        private final int id;
        private final String nombre;
        private final String departamento;
        private final double latitud;
        private final double longitud;
        private final List<List<Double>> poligono;

        Zona(int id, String nombre, String departamento, double latitud, double longitud,
            List<List<Double>> poligono) {
            this.id = id;
            this.nombre = nombre;
            this.departamento = departamento;
            this.latitud = latitud;
            this.longitud = longitud;
            this.poligono = poligono;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getDepartamento() {
            return departamento;
        }

        public double getLatitud() {
            return latitud;
        }

        public double getLongitud() {
            return longitud;
        }

        public List<List<Double>> getPoligono() {
            return poligono;
        }
    }

    public static List<Zona> getZonas() {
        return List.of(
            new Zona(1, "Tumbes", "Tumbes", -3.62, -81.0,
                List.of(List.of(-3.27, -80.75), List.of(-3.27, -81.25),
                    List.of(-3.97, -81.25), List.of(-3.97, -80.75))),
            new Zona(2, "Piura Norte", "Piura", -4.35, -81.35,
                List.of(List.of(-4.0, -81.1), List.of(-4.0, -81.6),
                    List.of(-4.7, -81.6), List.of(-4.7, -81.1))),
            new Zona(3, "Piura Sur (Paita)", "Piura", -5.25, -81.6,
                List.of(List.of(-4.9, -81.35), List.of(-4.9, -81.85),
                    List.of(-5.6, -81.85), List.of(-5.6, -81.35))),
            new Zona(4, "Lambayeque", "Lambayeque", -6.6, -80.6,
                List.of(List.of(-6.25, -80.35), List.of(-6.25, -80.85),
                    List.of(-6.95, -80.85), List.of(-6.95, -80.35))),
            new Zona(5, "La Libertad Norte", "La Libertad", -7.35, -79.95,
                List.of(List.of(-7.0, -79.7), List.of(-7.0, -80.2),
                    List.of(-7.7, -80.2), List.of(-7.7, -79.7))),
            new Zona(6, "La Libertad Sur (Chimbote)", "La Libertad", -9.1, -79.1,
                List.of(List.of(-8.75, -78.85), List.of(-8.75, -79.35),
                    List.of(-9.45, -79.35), List.of(-9.45, -78.85)))
        );
    }

    public static Zona getZona(int id) {
        return getZonas().stream()
            .filter(zona -> zona.getId() == id)
            .findFirst()
            .orElse(null);
    }
}