-- ===========================================================================
-- Clima Zona Costera - Esquema PostgreSQL + PostGIS (DER HU-1)
-- ---------------------------------------------------------------------------
-- Este script se ejecuta automaticamente con el perfil "postgres" una vez que
-- Hibernate ha creado las tablas del DER. Habilita la extension PostGIS y la
-- columna espacial de las zonas vulnerables.
-- ===========================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- Columna espacial (poligono de la zona vulnerable) con SRID 4326 (WGS84),
-- derivada del poligono en texto (WKT) definido en el DER.
ALTER TABLE zona_vulnerable
    ADD COLUMN IF NOT EXISTS geom geometry(Polygon, 4326);

-- Indice espacial GIST para consultas geograficas eficientes.
CREATE INDEX IF NOT EXISTS idx_zona_vulnerable_geom
    ON zona_vulnerable USING GIST (geom);

-- Indices de apoyo para las relaciones del DER.
CREATE INDEX IF NOT EXISTS idx_estacion_monitoreo_fuente
    ON estacion_monitoreo (fuente_id);

CREATE INDEX IF NOT EXISTS idx_registro_tsm_evento
    ON registro_tsm (evento_id);

CREATE INDEX IF NOT EXISTS idx_registro_tsm_estacion
    ON registro_tsm (estacion_id);

CREATE INDEX IF NOT EXISTS idx_alerta_preventiva_zona
    ON alerta_preventiva (zona_id);

CREATE INDEX IF NOT EXISTS idx_centro_ayuda_zona
    ON centro_ayuda (zona_id);