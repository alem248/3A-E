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

    /** Regiones del interior (sin mar): se miden por calor y lluvias. */
    private static final List<String> INTERIOR = List.of(
        "Amazonas", "Apurímac", "Ayacucho", "Cajamarca", "Cusco", "Huancavelica", "Huánuco",
        "Junín", "Loreto", "Madre de Dios", "Pasco", "Puno", "San Martín", "Ucayali");

    public static boolean esContinental(String departamento) {
        return departamento != null && INTERIOR.contains(departamento);
    }

    /** Zona del catalogo: identificador, nombre, tipo y poligono. */
    public static final class Zona {
        private final int id;
        private final String nombre;
        private final String departamento;
        private final double latitud;
        private final double longitud;
        private final List<List<Double>> poligono;
        private final String tipo;

        Zona(int id, String nombre, String departamento, double latitud, double longitud,
            List<List<Double>> poligono) {
            this(id, nombre, departamento, latitud, longitud, poligono, "MARINA");
        }

        Zona(int id, String nombre, String departamento, double latitud, double longitud,
            List<List<Double>> poligono, String tipo) {
            this.id = id;
            this.nombre = nombre;
            this.departamento = departamento;
            this.latitud = latitud;
            this.longitud = longitud;
            this.poligono = poligono;
            this.tipo = tipo;
        }

        public String getTipo() {
            return tipo;
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
                    List.of(-9.45, -79.35), List.of(-9.45, -78.85))),
            // Costeras nuevas: estacion marina en el mar frente a cada region.
            new Zona(101, "Áncash", "Áncash", -9.4, -78.5, caja(-9.4, -78.5, 0.3, 0.3), "MARINA"),
            new Zona(102, "Lima Costa", "Lima", -12.0, -77.3, caja(-12.0, -77.3, 0.3, 0.3), "MARINA"),
            new Zona(103, "Callao", "Callao", -12.05, -77.25, caja(-12.05, -77.25, 0.15, 0.15), "MARINA"),
            new Zona(104, "Ica", "Ica", -14.0, -76.4, caja(-14.0, -76.4, 0.3, 0.3), "MARINA"),
            new Zona(105, "Arequipa", "Arequipa", -16.9, -72.2, caja(-16.9, -72.2, 0.3, 0.3), "MARINA"),
            new Zona(106, "Moquegua", "Moquegua", -17.6, -71.5, caja(-17.6, -71.5, 0.2, 0.2), "MARINA"),
            new Zona(107, "Tacna", "Tacna", -18.0, -70.7, caja(-18.0, -70.7, 0.2, 0.2), "MARINA"),
            // Interior: calor (temperatura del aire) y lluvias.
            new Zona(108, "Amazonas", "Amazonas", -6.23, -77.87, caja(-6.23, -77.87, 0.8, 0.8), "CONTINENTAL"),
            new Zona(109, "Apurímac", "Apurímac", -13.63, -72.88, caja(-13.63, -72.88, 0.7, 0.7), "CONTINENTAL"),
            new Zona(110, "Ayacucho", "Ayacucho", -13.16, -74.22, caja(-13.16, -74.22, 0.8, 0.8), "CONTINENTAL"),
            new Zona(111, "Cajamarca", "Cajamarca", -7.16, -78.51, caja(-7.16, -78.51, 0.8, 0.8), "CONTINENTAL"),
            new Zona(112, "Cusco", "Cusco", -13.53, -71.97, caja(-13.53, -71.97, 0.9, 0.9), "CONTINENTAL"),
            new Zona(113, "Huancavelica", "Huancavelica", -12.79, -74.98, caja(-12.79, -74.98, 0.7, 0.7), "CONTINENTAL"),
            new Zona(114, "Huánuco", "Huánuco", -9.93, -76.24, caja(-9.93, -76.24, 0.8, 0.8), "CONTINENTAL"),
            new Zona(115, "Junín", "Junín", -11.16, -75.99, caja(-11.16, -75.99, 0.9, 0.9), "CONTINENTAL"),
            new Zona(116, "Loreto", "Loreto", -3.75, -73.25, caja(-3.75, -73.25, 2.0, 2.0), "CONTINENTAL"),
            new Zona(117, "Madre de Dios", "Madre de Dios", -12.59, -69.19, caja(-12.59, -69.19, 1.2, 1.2), "CONTINENTAL"),
            new Zona(118, "Pasco", "Pasco", -10.68, -76.26, caja(-10.68, -76.26, 0.6, 0.6), "CONTINENTAL"),
            new Zona(119, "Puno", "Puno", -15.84, -70.02, caja(-15.84, -70.02, 1.0, 1.0), "CONTINENTAL"),
            new Zona(120, "San Martín", "San Martín", -6.49, -76.37, caja(-6.49, -76.37, 0.9, 0.9), "CONTINENTAL"),
            new Zona(121, "Ucayali", "Ucayali", -8.38, -74.55, caja(-8.38, -74.55, 1.5, 1.5), "CONTINENTAL")
        );
    }

    /** Rectangulo alrededor de un punto, en orden [lat, lon]. */
    private static List<List<Double>> caja(double lat, double lon, double margenLat, double margenLon) {
        return List.of(
            List.of(lat + margenLat, lon - margenLon), List.of(lat + margenLat, lon + margenLon),
            List.of(lat - margenLat, lon + margenLon), List.of(lat - margenLat, lon - margenLon));
    }


    public static Zona getZona(int id) {
        return getZonas().stream()
            .filter(zona -> zona.getId() == id)
            .findFirst()
            .orElse(null);
    }
}