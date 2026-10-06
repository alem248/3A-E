# HU-1 · Modelo de datos (DER → PostgreSQL + PostGIS)

El modelo entidad-relación del proyecto **Clima Zona Costera**. Se implementa con
Spring Data JPA en `clima-spring/src/main/java/com/tecsup/clima/persistence/`.

## Diagrama entidad-relación

```mermaid
erDiagram
    FUENTE_OFICIAL ||--o{ ESTACION_MONITOREO : administra
    NIVEL_RIESGO ||--o{ ZONA_VULNERABLE : clasifica
    NIVEL_RIESGO ||--o{ ALERTA_PREVENTIVA : categoriza
    ROL ||--o{ USUARIO : tiene
    USUARIO ||--o{ UBICACION_USUARIO : registra
    USUARIO ||--o{ HISTORIAL_NOTIFICACION : recibe
    HISTORIAL_NOTIFICACION }o--|| ALERTA_PREVENTIVA : genera
    ZONA_VULNERABLE ||--o{ CENTRO_AYUDA : ubica
    ZONA_VULNERABLE ||--o{ ALERTA_PREVENTIVA : presenta
    ALERTA_PREVENTIVA ||--o{ MEDIDA_PREVENTIVA : detalla
    EVENTO_CLIMATICO ||--o{ PROYECCION_TRAYECTORIA : proyecta
    EVENTO_CLIMATICO ||--o{ REGISTRO_TSM : contiene
    EVENTO_CLIMATICO ||--o{ REGISTRO_PRECIPITACION : contiene
    ESTACION_MONITOREO ||--o{ REGISTRO_TSM : mide

    FUENTE_OFICIAL {
        int id PK
        string nombre_institucion
        string url_api
    }
    NIVEL_RIESGO {
        int id PK
        string nombre
        string color_hex
    }
    ROL {
        int id PK
        string nombre
        string descripcion
    }
    EVENTO_CLIMATICO {
        int id PK
        string nombre
        date fecha_inicio
        string estado
    }
    ESTACION_MONITOREO {
        int id PK
        int fuente_id FK
        string tipo
        decimal latitud
        decimal longitud
    }
    ZONA_VULNERABLE {
        int id PK
        int nivel_riesgo_id FK
        string poligono
    }
    USUARIO {
        int id PK
        int rol_id FK
        string nombres
        string email
        string fcm_token
    }
    PROYECCION_TRAYECTORIA {
        int id PK
        int evento_id FK
        decimal lat_futura
        decimal lon_futura
        date fecha_estimada
    }
    REGISTRO_TSM {
        int id PK
        int evento_id FK
        int estacion_id FK
        decimal latitud
        decimal longitud
        decimal anomalia
    }
    REGISTRO_PRECIPITACION {
        int id PK
        int evento_id FK
        decimal milimetros
    }
    CENTRO_AYUDA {
        int id PK
        int zona_id FK
        string tipo
    }
    ALERTA_PREVENTIVA {
        int id PK
        int zona_id FK
        int nivel_riesgo_id FK
        string mensaje
    }
    UBICACION_USUARIO {
        int id PK
        int usuario_id FK
        decimal latitud
        decimal longitud
    }
    HISTORIAL_NOTIFICACION {
        int id PK
        int usuario_id FK
        int alerta_id FK
        boolean leida
    }
    MEDIDA_PREVENTIVA {
        int id PK
        int alerta_id FK
        string descripcion_accion
    }
```

## Tablas

| Tabla | Propósito |
|---|---|
| `fuente_oficial` | Institución y API oficial de datos climáticos (Open-Meteo). |
| `nivel_riesgo` | Semáforo térmico: nombre y color hexadecimal. |
| `rol` | Roles de usuario (poblador, Defensa Civil, admin). |
| `evento_climatico` | Evento activo (Niño Costero) con fecha de inicio y estado. |
| `estacion_monitoreo` | Estación que administra una fuente oficial y sus coordenadas. |
| `zona_vulnerable` | Zona geográfica clasificada por nivel de riesgo, con su polígono. |
| `usuario` | Usuario del sistema con rol y token FCM. |
| `proyeccion_trayectoria` | Posición futura estimada del evento (trayectoria). |
| `registro_tsm` | Lectura de TSM por estación y evento, con su anomalía. |
| `registro_precipitacion` | Milímetros de precipitación del evento. |
| `centro_ayuda` | Centro de ayuda ubicado en una zona. |
| `alerta_preventiva` | Alerta por zona y nivel de riesgo. |
| `ubicacion_usuario` | Posición registrada por el usuario. |
| `historial_notificacion` | Notificaciones enviadas al usuario y su estado de lectura. |
| `medida_preventiva` | Acciones preventivas detalladas en una alerta. |

## Uso de PostGIS

El DER almacena `zona_vulnerable.poligono` como texto (WKT). En el perfil
`postgres` se agrega la capa espacial:

1. `db/postgres/schema.sql` habilita la extensión PostGIS, agrega la columna
   `geom geometry(Polygon, 4326)` y crea el índice espacial GIST.
2. `PostgisGeometriaRefrescador` puebla `geom` con
   `ST_SetSRID(ST_GeomFromText(poligono), 4326)` después de sembrar el catálogo,
   habilitando consultas espaciales (`ST_Contains`, `ST_Intersects`, etc.).

## Datos iniciales

`DatosInicialesLoader` siembra de forma idempotente: `rol`, `nivel_riesgo`,
`fuente_oficial`, `evento_climatico`, `estacion_monitoreo` y `zona_vulnerable`
(las 6 zonas de la costa norte). Las zonas y sus estaciones comparten el mismo
`id`, de modo que el `registro_tsm` de una estación corresponde a su zona.

## Despliegue de la base de datos

```bash
# 1) Levantar PostgreSQL + PostGIS
docker compose -f db/docker-compose.yml up -d

# 2) Arrancar la aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

> Para desarrollo sin infraestructura se usa el perfil `dev` (H2 en memoria),
> que replica las mismas tablas sin necesidad de Docker.