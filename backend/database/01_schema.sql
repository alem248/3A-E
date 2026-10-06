-- =====================================================================
-- Prevención y Análisis del Niño Costero - Esquema PostgreSQL + PostGIS
-- Uso:  createdb ninocostero
--       psql -d ninocostero -f database/01_schema.sql
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS postgis;

DROP TABLE IF EXISTS historial_notificacion, medida_preventiva, alerta_preventiva,
    centro_ayuda, zona_vulnerable, nivel_riesgo, proyeccion_trayectoria,
    registro_precipitacion, registro_tsm, estacion_monitoreo, fuente_oficial,
    evento_climatico, ubicacion_usuario, usuario, rol CASCADE;

-- 1. rol -------------------------------------------------------------
CREATE TABLE rol (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(30) NOT NULL UNIQUE,   -- Poblador, Defensa Civil, Admin
    descripcion VARCHAR(200)
);

-- 2. usuario ---------------------------------------------------------
CREATE TABLE usuario (
    id             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rol_id         INT NOT NULL REFERENCES rol(id),
    nombres        VARCHAR(120) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    fcm_token      VARCHAR(255),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. ubicacion_usuario -----------------------------------------------
CREATE TABLE ubicacion_usuario (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id           INT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    latitud              NUMERIC(9,6) NOT NULL CHECK (latitud BETWEEN -90 AND 90),
    longitud             NUMERIC(9,6) NOT NULL CHECK (longitud BETWEEN -180 AND 180),
    ultima_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    geom geometry(Point,4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(longitud::double precision, latitud::double precision), 4326)) STORED
);

-- 4. evento_climatico ------------------------------------------------
CREATE TABLE evento_climatico (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin    DATE,
    estado       VARCHAR(10) NOT NULL DEFAULT 'Activo' CHECK (estado IN ('Activo','Inactivo')),
    CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

-- 5. fuente_oficial --------------------------------------------------
CREATE TABLE fuente_oficial (
    id                 INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_institucion VARCHAR(80) NOT NULL,   -- SENAMHI, NOAA, IMARPE
    url_api            VARCHAR(255),
    tipo_dato          VARCHAR(60)
);

-- 6. estacion_monitoreo ----------------------------------------------
CREATE TABLE estacion_monitoreo (
    id        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fuente_id INT NOT NULL REFERENCES fuente_oficial(id),
    nombre    VARCHAR(120) NOT NULL,
    tipo      VARCHAR(20) NOT NULL CHECK (tipo IN ('Boya','Satélite','Estación Terrestre')),
    latitud   NUMERIC(9,6) NOT NULL CHECK (latitud BETWEEN -90 AND 90),
    longitud  NUMERIC(9,6) NOT NULL CHECK (longitud BETWEEN -180 AND 180),
    geom geometry(Point,4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(longitud::double precision, latitud::double precision), 4326)) STORED
);

-- 7. registro_tsm (Temperatura Superficial del Mar) ------------------
CREATE TABLE registro_tsm (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id   INT NOT NULL REFERENCES evento_climatico(id),
    estacion_id INT NOT NULL REFERENCES estacion_monitoreo(id),
    latitud     NUMERIC(9,6) NOT NULL CHECK (latitud BETWEEN -90 AND 90),
    longitud    NUMERIC(9,6) NOT NULL CHECK (longitud BETWEEN -180 AND 180),
    temperatura NUMERIC(5,2) NOT NULL,          -- °C
    anomalia    NUMERIC(5,2),                   -- °C respecto al promedio
    fecha_hora  TIMESTAMPTZ NOT NULL,
    geom geometry(Point,4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(longitud::double precision, latitud::double precision), 4326)) STORED
);

-- 8. registro_precipitacion ------------------------------------------
CREATE TABLE registro_precipitacion (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id        INT NOT NULL REFERENCES evento_climatico(id),
    estacion_id      INT NOT NULL REFERENCES estacion_monitoreo(id),
    milimetros_lluvia NUMERIC(7,2) NOT NULL CHECK (milimetros_lluvia >= 0),
    fecha_hora       TIMESTAMPTZ NOT NULL
);

-- 9. proyeccion_trayectoria ------------------------------------------
CREATE TABLE proyeccion_trayectoria (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_id               INT NOT NULL REFERENCES evento_climatico(id),
    lat_futura              NUMERIC(9,6) NOT NULL CHECK (lat_futura BETWEEN -90 AND 90),
    lon_futura              NUMERIC(9,6) NOT NULL CHECK (lon_futura BETWEEN -180 AND 180),
    fecha_estimada          DATE NOT NULL,
    confiabilidad_porcentaje NUMERIC(5,2) NOT NULL CHECK (confiabilidad_porcentaje BETWEEN 0 AND 100),
    geom geometry(Point,4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(lon_futura::double precision, lat_futura::double precision), 4326)) STORED
);

-- 10. nivel_riesgo (catálogo) ----------------------------------------
CREATE TABLE nivel_riesgo (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(20) NOT NULL UNIQUE,    -- Verde, Amarillo, Naranja, Rojo
    color_hex   CHAR(7) NOT NULL CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
    descripcion VARCHAR(200)
);

-- 11. zona_vulnerable ------------------------------------------------
CREATE TABLE zona_vulnerable (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_zona     VARCHAR(120) NOT NULL,
    nivel_riesgo_id INT NOT NULL REFERENCES nivel_riesgo(id),
    poligono        geometry(Polygon,4326) NOT NULL
);

-- 12. alerta_preventiva ----------------------------------------------
CREATE TABLE alerta_preventiva (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    zona_vulnerable_id INT NOT NULL REFERENCES zona_vulnerable(id),
    nivel_riesgo_id   INT NOT NULL REFERENCES nivel_riesgo(id),
    mensaje           TEXT NOT NULL,
    fecha_emision     TIMESTAMPTZ NOT NULL DEFAULT now(),
    vigencia_hasta    TIMESTAMPTZ NOT NULL,
    CHECK (vigencia_hasta > fecha_emision)
);

-- 13. medida_preventiva ----------------------------------------------
CREATE TABLE medida_preventiva (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    alerta_id         INT NOT NULL REFERENCES alerta_preventiva(id) ON DELETE CASCADE,
    titulo            VARCHAR(120) NOT NULL,
    descripcion_accion TEXT NOT NULL
);

-- 14. centro_ayuda ---------------------------------------------------
CREATE TABLE centro_ayuda (
    id                 INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    zona_vulnerable_id INT NOT NULL REFERENCES zona_vulnerable(id),
    nombre             VARCHAR(120),
    tipo               VARCHAR(20) NOT NULL CHECK (tipo IN ('Refugio','Almacén','Hospital')),
    latitud            NUMERIC(9,6) NOT NULL CHECK (latitud BETWEEN -90 AND 90),
    longitud           NUMERIC(9,6) NOT NULL CHECK (longitud BETWEEN -180 AND 180),
    aforo              INT CHECK (aforo >= 0),
    geom geometry(Point,4326) GENERATED ALWAYS AS
        (ST_SetSRID(ST_MakePoint(longitud::double precision, latitud::double precision), 4326)) STORED
);

-- 15. historial_notificacion -----------------------------------------
CREATE TABLE historial_notificacion (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  INT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    alerta_id   INT NOT NULL REFERENCES alerta_preventiva(id) ON DELETE CASCADE,
    leida       BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índices espaciales (GIST) y de consulta frecuente ------------------
CREATE INDEX idx_ubicacion_usuario_geom ON ubicacion_usuario USING GIST (geom);
CREATE INDEX idx_estacion_geom          ON estacion_monitoreo USING GIST (geom);
CREATE INDEX idx_tsm_geom               ON registro_tsm USING GIST (geom);
CREATE INDEX idx_trayectoria_geom       ON proyeccion_trayectoria USING GIST (geom);
CREATE INDEX idx_zona_poligono          ON zona_vulnerable USING GIST (poligono);
CREATE INDEX idx_centro_ayuda_geom      ON centro_ayuda USING GIST (geom);

CREATE INDEX idx_tsm_estacion_fecha  ON registro_tsm (estacion_id, fecha_hora DESC);
CREATE INDEX idx_tsm_evento_fecha    ON registro_tsm (evento_id, fecha_hora DESC);
CREATE INDEX idx_precip_estacion_fecha ON registro_precipitacion (estacion_id, fecha_hora DESC);
CREATE INDEX idx_alerta_zona_vigencia ON alerta_preventiva (zona_vulnerable_id, vigencia_hasta);
CREATE INDEX idx_ubicacion_usuario_fecha ON ubicacion_usuario (usuario_id, ultima_actualizacion DESC);
CREATE INDEX idx_notificacion_usuario ON historial_notificacion (usuario_id, fecha_envio DESC);
