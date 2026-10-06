package pe.tecsup.ninocostero.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas SQL/PostGIS. Se usan parámetros nombrados (sin concatenar valores) para evitar inyección SQL. */
@Repository
public class ClimaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ClimaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Agrega JOIN/WHERE del filtro geográfico sobre una columna geom
    private void aplicarFiltro(FiltroGeo f, String geomCol, StringBuilder from,
                               StringBuilder where, MapSqlParameterSource p) {
        if (f == null || f.vacio()) return;
        double radioM = (f.radioKm() == null ? 100.0 : f.radioKm()) * 1000.0;

        if (f.hayZona()) {
            from.append(" JOIN zona_vulnerable zf ON zf.id = :zonaId ");
            where.append(" AND ST_DWithin(").append(geomCol)
                 .append("::geography, zf.poligono::geography, :radioM) ");
            p.addValue("zonaId", f.zonaId());
            p.addValue("radioM", radioM);
        } else if (f.hayBbox()) {
            where.append(" AND ").append(geomCol)
                 .append(" && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326) ");
            p.addValue("minLon", f.minLon()).addValue("minLat", f.minLat())
             .addValue("maxLon", f.maxLon()).addValue("maxLat", f.maxLat());
        } else if (f.hayPunto()) {
            where.append(" AND ST_DWithin(").append(geomCol)
                 .append("::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radioM) ");
            p.addValue("lon", f.lon()).addValue("lat", f.lat()).addValue("radioM", radioM);
        }
    }

    // ---- Zonas vulnerables ----
    public List<Map<String, Object>> zonas() {
        return jdbc.queryForList("""
            SELECT z.id, z.nombre_zona, n.nombre AS nivel_riesgo, n.color_hex,
                   ST_AsGeoJSON(z.poligono) AS geometry
            FROM zona_vulnerable z
            JOIN nivel_riesgo n ON n.id = z.nivel_riesgo_id
            ORDER BY z.id
            """, new MapSqlParameterSource());
    }

    public List<Map<String, Object>> zonaPorId(int id) {
        return jdbc.queryForList("""
            SELECT z.id, z.nombre_zona, n.nombre AS nivel_riesgo, n.color_hex,
                   ST_AsGeoJSON(z.poligono) AS geometry
            FROM zona_vulnerable z
            JOIN nivel_riesgo n ON n.id = z.nivel_riesgo_id
            WHERE z.id = :id
            """, new MapSqlParameterSource("id", id));
    }

    // ---- TSM: última lectura por estación (tiempo real) ----
    public List<Map<String, Object>> tsmActual(FiltroGeo f) {
        StringBuilder from = new StringBuilder("""
            FROM registro_tsm t
            JOIN estacion_monitoreo e ON e.id = t.estacion_id
            JOIN evento_climatico ev ON ev.id = t.evento_id AND ev.estado = 'Activo'
            """);
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        MapSqlParameterSource p = new MapSqlParameterSource();
        aplicarFiltro(f, "t.geom", from, where, p);

        String sql = "SELECT DISTINCT ON (t.estacion_id) t.id, t.estacion_id, e.nombre AS estacion, "
                + "t.latitud, t.longitud, t.temperatura, t.anomalia, t.fecha_hora, "
                + "ST_AsGeoJSON(t.geom) AS geometry "
                + from + where
                + " ORDER BY t.estacion_id, t.fecha_hora DESC";
        return jdbc.queryForList(sql, p);
    }

    // ---- TSM: historial en las últimas N horas ----
    public List<Map<String, Object>> tsmHistorial(int horas, FiltroGeo f) {
        StringBuilder from = new StringBuilder("""
            FROM registro_tsm t
            JOIN estacion_monitoreo e ON e.id = t.estacion_id
            JOIN evento_climatico ev ON ev.id = t.evento_id AND ev.estado = 'Activo'
            """);
        StringBuilder where = new StringBuilder(" WHERE t.fecha_hora >= now() - make_interval(hours => :horas) ");
        MapSqlParameterSource p = new MapSqlParameterSource("horas", horas);
        aplicarFiltro(f, "t.geom", from, where, p);

        String sql = "SELECT t.id, t.estacion_id, e.nombre AS estacion, t.latitud, t.longitud, "
                + "t.temperatura, t.anomalia, t.fecha_hora, ST_AsGeoJSON(t.geom) AS geometry "
                + from + where + " ORDER BY t.fecha_hora DESC";
        return jdbc.queryForList(sql, p);
    }

    // ---- Precipitación: última lectura por estación ----
    public List<Map<String, Object>> precipitacionActual(FiltroGeo f) {
        StringBuilder from = new StringBuilder("""
            FROM registro_precipitacion r
            JOIN estacion_monitoreo e ON e.id = r.estacion_id
            JOIN evento_climatico ev ON ev.id = r.evento_id AND ev.estado = 'Activo'
            """);
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        MapSqlParameterSource p = new MapSqlParameterSource();
        aplicarFiltro(f, "e.geom", from, where, p);

        String sql = "SELECT DISTINCT ON (r.estacion_id) r.id, r.estacion_id, e.nombre AS estacion, "
                + "r.milimetros_lluvia, r.fecha_hora, ST_AsGeoJSON(e.geom) AS geometry "
                + from + where
                + " ORDER BY r.estacion_id, r.fecha_hora DESC";
        return jdbc.queryForList(sql, p);
    }

    // ---- Estaciones de monitoreo ----
    public List<Map<String, Object>> estaciones(FiltroGeo f) {
        StringBuilder from = new StringBuilder("""
            FROM estacion_monitoreo e
            JOIN fuente_oficial fo ON fo.id = e.fuente_id
            """);
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        MapSqlParameterSource p = new MapSqlParameterSource();
        aplicarFiltro(f, "e.geom", from, where, p);

        String sql = "SELECT e.id, e.nombre, e.tipo, fo.nombre_institucion AS fuente, "
                + "ST_AsGeoJSON(e.geom) AS geometry "
                + from + where + " ORDER BY e.id";
        return jdbc.queryForList(sql, p);
    }

    // ---- Trayectoria proyectada ----
    public List<Map<String, Object>> trayectoria() {
        return jdbc.queryForList("""
            SELECT pt.id, pt.evento_id, pt.lat_futura, pt.lon_futura, pt.fecha_estimada,
                   pt.confiabilidad_porcentaje, ST_AsGeoJSON(pt.geom) AS geometry
            FROM proyeccion_trayectoria pt
            JOIN evento_climatico ev ON ev.id = pt.evento_id AND ev.estado = 'Activo'
            WHERE pt.fecha_estimada >= current_date
            ORDER BY pt.fecha_estimada
            """, new MapSqlParameterSource());
    }

    // ---- Alertas vigentes y medidas ----
    public List<Map<String, Object>> alertasActivas(Integer zonaId) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String filtroZona = "";
        if (zonaId != null) {
            filtroZona = " AND a.zona_vulnerable_id = :zonaId ";
            p.addValue("zonaId", zonaId);
        }
        return jdbc.queryForList("""
            SELECT a.id, a.zona_vulnerable_id, z.nombre_zona, n.nombre AS nivel_riesgo, n.color_hex,
                   a.mensaje, a.fecha_emision, a.vigencia_hasta
            FROM alerta_preventiva a
            JOIN zona_vulnerable z ON z.id = a.zona_vulnerable_id
            JOIN nivel_riesgo n ON n.id = a.nivel_riesgo_id
            WHERE a.vigencia_hasta >= now() """ + filtroZona + " ORDER BY a.fecha_emision DESC", p);
    }

    public List<Map<String, Object>> medidasDeAlerta(int alertaId) {
        return jdbc.queryForList("""
            SELECT id, titulo, descripcion_accion
            FROM medida_preventiva WHERE alerta_id = :id ORDER BY id
            """, new MapSqlParameterSource("id", alertaId));
    }

    // ---- Centros de ayuda ----
    public List<Map<String, Object>> centrosAyuda(Integer zonaId, String tipo) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (zonaId != null) { where.append(" AND c.zona_vulnerable_id = :zonaId "); p.addValue("zonaId", zonaId); }
        if (tipo != null)   { where.append(" AND c.tipo = :tipo ");                 p.addValue("tipo", tipo); }
        return jdbc.queryForList("SELECT c.id, c.zona_vulnerable_id, c.nombre, c.tipo, c.aforo, "
                + "ST_AsGeoJSON(c.geom) AS geometry FROM centro_ayuda c" + where + " ORDER BY c.id", p);
    }

    // ---- ¿En qué zona está el usuario? (cruce espacial PostGIS) ----
    public List<Map<String, Object>> zonaDelUsuario(int usuarioId) {
        return jdbc.queryForList("""
            SELECT z.id AS zona_id, z.nombre_zona, n.nombre AS nivel_riesgo, n.color_hex
            FROM (SELECT geom FROM ubicacion_usuario
                  WHERE usuario_id = :id ORDER BY ultima_actualizacion DESC LIMIT 1) u
            JOIN zona_vulnerable z ON ST_Contains(z.poligono, u.geom)
            JOIN nivel_riesgo n ON n.id = z.nivel_riesgo_id
            """, new MapSqlParameterSource("id", usuarioId));
    }

    public boolean usuarioTieneUbicacion(int usuarioId) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM ubicacion_usuario WHERE usuario_id = :id",
                new MapSqlParameterSource("id", usuarioId), Integer.class);
        return n != null && n > 0;
    }
}
