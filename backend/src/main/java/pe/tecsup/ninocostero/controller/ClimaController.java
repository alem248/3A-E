package pe.tecsup.ninocostero.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.tecsup.ninocostero.repository.ClimaRepository;
import pe.tecsup.ninocostero.repository.FiltroGeo;

@RestController
@RequestMapping("/api")
public class ClimaController {

    private final ClimaRepository repo;

    public ClimaController(ClimaRepository repo) {
        this.repo = repo;
    }

    // ---- Zonas vulnerables ----
    @GetMapping("/zonas")
    public Map<String, Object> zonas() {
        return GeoJsonUtil.featureCollection(repo.zonas());
    }

    @GetMapping("/zonas/{id}")
    public Map<String, Object> zona(@PathVariable int id) {
        return GeoJsonUtil.featureCollection(zonaExistente(id));
    }

    // ---- Datos en tiempo real (filtrables por zona geográfica) ----
    /** Última TSM por estación. Ej: /api/tsm/actual?zonaId=1&radioKm=150 */
    @GetMapping("/tsm/actual")
    public Map<String, Object> tsmActual(
            @RequestParam(required = false) Integer zonaId,
            @RequestParam(required = false) Double radioKm,
            @RequestParam(required = false) Double minLat, @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLat, @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
        FiltroGeo f = construirFiltro(zonaId, radioKm, minLat, minLon, maxLat, maxLon, lat, lon);
        return GeoJsonUtil.featureCollection(repo.tsmActual(f));
    }

    /** Historial TSM de las últimas N horas (1-720). */
    @GetMapping("/tsm/historial")
    public Map<String, Object> tsmHistorial(
            @RequestParam(defaultValue = "24") int horas,
            @RequestParam(required = false) Integer zonaId,
            @RequestParam(required = false) Double radioKm,
            @RequestParam(required = false) Double minLat, @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLat, @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
        if (horas < 1 || horas > 720) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horas debe estar entre 1 y 720");
        }
        FiltroGeo f = construirFiltro(zonaId, radioKm, minLat, minLon, maxLat, maxLon, lat, lon);
        return GeoJsonUtil.featureCollection(repo.tsmHistorial(horas, f));
    }

    @GetMapping("/precipitacion/actual")
    public Map<String, Object> precipitacionActual(
            @RequestParam(required = false) Integer zonaId,
            @RequestParam(required = false) Double radioKm,
            @RequestParam(required = false) Double minLat, @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLat, @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
        FiltroGeo f = construirFiltro(zonaId, radioKm, minLat, minLon, maxLat, maxLon, lat, lon);
        return GeoJsonUtil.featureCollection(repo.precipitacionActual(f));
    }

    @GetMapping("/estaciones")
    public Map<String, Object> estaciones(
            @RequestParam(required = false) Integer zonaId,
            @RequestParam(required = false) Double radioKm,
            @RequestParam(required = false) Double minLat, @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLat, @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
        FiltroGeo f = construirFiltro(zonaId, radioKm, minLat, minLon, maxLat, maxLon, lat, lon);
        return GeoJsonUtil.featureCollection(repo.estaciones(f));
    }

    @GetMapping("/trayectoria")
    public Map<String, Object> trayectoria() {
        return GeoJsonUtil.featureCollection(repo.trayectoria());
    }

    // ---- Alertas, medidas y centros de ayuda ----
    @GetMapping("/alertas/activas")
    public List<Map<String, Object>> alertasActivas(@RequestParam(required = false) Integer zonaId) {
        return conMedidas(repo.alertasActivas(zonaId));
    }

    @GetMapping("/centros-ayuda")
    public Map<String, Object> centrosAyuda(
            @RequestParam(required = false) Integer zonaId,
            @RequestParam(required = false) String tipo) {
        if (tipo != null && !List.of("Refugio", "Almacén", "Hospital").contains(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tipo debe ser Refugio, Almacén o Hospital");
        }
        return GeoJsonUtil.featureCollection(repo.centrosAyuda(zonaId, tipo));
    }

    // ---- "¿Qué tan cerca estoy?": riesgo según la última ubicación del usuario ----
    @GetMapping("/usuarios/{id}/riesgo")
    public Map<String, Object> riesgoUsuario(@PathVariable int id) {
        if (!repo.usuarioTieneUbicacion(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no tiene ubicación registrada");
        }
        List<Map<String, Object>> zonas = repo.zonaDelUsuario(id);
        Map<String, Object> out = new LinkedHashMap<>();
        if (zonas.isEmpty()) {
            out.put("enZonaVulnerable", false);
            out.put("alertas", List.of());
            return out;
        }
        Map<String, Object> zona = zonas.get(0);
        out.put("enZonaVulnerable", true);
        out.put("zona", zona);
        out.put("alertas", conMedidas(repo.alertasActivas(((Number) zona.get("zona_id")).intValue())));
        return out;
    }

    // ---- Auxiliares ----
    private List<Map<String, Object>> zonaExistente(int id) {
        List<Map<String, Object>> z = repo.zonaPorId(id);
        if (z.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona " + id + " no encontrada");
        }
        return z;
    }

    private List<Map<String, Object>> conMedidas(List<Map<String, Object>> alertas) {
        for (Map<String, Object> a : alertas) {
            a.put("medidas", repo.medidasDeAlerta(((Number) a.get("id")).intValue()));
        }
        return alertas;
    }

    /** Valida que solo se use un modo de filtro y que los rangos sean correctos. */
    private FiltroGeo construirFiltro(Integer zonaId, Double radioKm,
                                      Double minLat, Double minLon, Double maxLat, Double maxLon,
                                      Double lat, Double lon) {
        boolean bbox = minLat != null || minLon != null || maxLat != null || maxLon != null;
        boolean punto = lat != null || lon != null;
        int modos = (zonaId != null ? 1 : 0) + (bbox ? 1 : 0) + (punto ? 1 : 0);

        if (modos > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use solo un filtro: zonaId, bbox (minLat,minLon,maxLat,maxLon) o punto (lat,lon)");
        }
        if (bbox) {
            if (minLat == null || minLon == null || maxLat == null || maxLon == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "bbox requiere minLat, minLon, maxLat y maxLon");
            }
            validarLat(minLat); validarLat(maxLat); validarLon(minLon); validarLon(maxLon);
            if (minLat > maxLat || minLon > maxLon) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bbox inválido: min mayor que max");
            }
        }
        if (punto) {
            if (lat == null || lon == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "punto requiere lat y lon");
            }
            validarLat(lat); validarLon(lon);
        }
        if (radioKm != null && (radioKm <= 0 || radioKm > 1000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "radioKm debe estar entre 0 y 1000");
        }
        if (zonaId != null) {
            zonaExistente(zonaId);
        }
        return new FiltroGeo(zonaId, radioKm, minLat, minLon, maxLat, maxLon, lat, lon);
    }

    private void validarLat(double v) {
        if (v < -90 || v > 90) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitud fuera de rango");
    }

    private void validarLon(double v) {
        if (v < -180 || v > 180) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Longitud fuera de rango");
    }
}
