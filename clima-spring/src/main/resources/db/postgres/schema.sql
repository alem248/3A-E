-- ===========================================================================
-- Clima Zona Costera - Esquema PostgreSQL + PostGIS (HU-1)
-- ---------------------------------------------------------------------------
-- Este script se ejecuta automaticamente con el perfil "postgres" una vez que
-- Hibernate ha creado las tablas relacionales. Agrega la extension PostGIS y
-- la columna espacial de las zonas termicas.
-- ===========================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- Columna espacial (poligono de la zona) con SRID 4326 (WGS84).
ALTER TABLE zona_termica
    ADD COLUMN IF NOT EXISTS geom geometry(Polygon, 4326);

-- Puebla la geometria desde el WKT almacenado por la aplicacion.
UPDATE zona_termica
SET geom = ST_SetSRID(ST_GeomFromText(poligono_wkt), 4326)
WHERE geom IS NULL
  AND poligono_wkt IS NOT NULL;

-- Indice espacial GIST para consultas geograficas eficientes.
CREATE INDEX IF NOT EXISTS idx_zona_termica_geom
    ON zona_termica USING GIST (geom);

CREATE INDEX IF NOT EXISTS idx_lectura_tsm_zona
    ON lectura_tsm (zona_id);

CREATE INDEX IF NOT EXISTS idx_lectura_tsm_registrado
    ON lectura_tsm (registrado_en);
