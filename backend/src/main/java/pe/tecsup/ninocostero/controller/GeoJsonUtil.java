package pe.tecsup.ninocostero.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Convierte filas SQL (con columna "geometry" en texto GeoJSON) a FeatureCollection para Leaflet/Mapbox. */
final class GeoJsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeoJsonUtil() {}

    static Map<String, Object> featureCollection(List<Map<String, Object>> rows) {
        List<Map<String, Object>> features = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> props = new LinkedHashMap<>(row);
            Object geomText = props.remove("geometry");
            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", parse(geomText));
            feature.put("properties", props);
            features.add(feature);
        }
        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "FeatureCollection");
        fc.put("features", features);
        return fc;
    }

    static JsonNode parse(Object geomText) {
        if (geomText == null) return null;
        try {
            return MAPPER.readTree(geomText.toString());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("GeoJSON inválido", e);
        }
    }
}
