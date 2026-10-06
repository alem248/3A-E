package pe.tecsup.ninocostero.repository;

/**
 * Filtro geográfico opcional. Solo se puede usar UN modo a la vez:
 *  - zona:  zonaId (+ radioKm alrededor del polígono)
 *  - bbox:  minLat, minLon, maxLat, maxLon
 *  - punto: lat, lon (+ radioKm)
 */
public record FiltroGeo(
        Integer zonaId,
        Double radioKm,
        Double minLat, Double minLon, Double maxLat, Double maxLon,
        Double lat, Double lon) {

    public boolean hayZona()  { return zonaId != null; }
    public boolean hayBbox()  { return minLat != null; }
    public boolean hayPunto() { return lat != null; }
    public boolean vacio()    { return !hayZona() && !hayBbox() && !hayPunto(); }
}
