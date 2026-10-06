# Clima Zona Costera · HU-1

Mapa interactivo del clima de la costa peruana para **Defensa Civil**: muestra
en tiempo real la temperatura, la intensidad y la trayectoria del Niño Costero,
coloreando las zonas según su nivel de riesgo (verde / amarillo / naranja / rojo).

El repositorio evolucionó del frontend React + Vite original a un **backend
Java Spring Boot** que sirve la API REST y el mapa Leaflet.

## Arquitectura

```
clima-spring/                        Backend Spring Boot + Maven
├── src/main/java/com/tecsup/clima/
│   ├── ClimaCosteroApplication.java
│   ├── config/         WeatherProperties, ZonasSeedLoader
│   ├── domain/         NivelRiesgo, ZonaTermica, WeatherData, WeatherSnapshot
│   ├── persistence/    Entidades JPA y repositorios (modelo de datos)
│   ├── scheduler/      AutoRefreshScheduler (refresco cada 15 min)
│   ├── service/        WeatherService, WeatherCacheService, ZonasTermicasService
│   └── web/            WeatherController, ZonasController (REST)
├── src/main/resources/
│   ├── application.properties            perfil dev (H2 en memoria)
│   ├── application-postgres.properties   perfil postgres (PostgreSQL + PostGIS)
│   ├── db/postgres/schema.sql            extensión PostGIS + columna geom + índices
│   └── static/                           frontend Leaflet (HTML/CSS/JS)
├── db/docker-compose.yml                 PostgreSQL 16 + PostGIS
└── docs/HU-1-modelo-datos.md             diagrama entidad-relación
```

## Requisitos

- JDK 17 o superior
- Maven 3.9+ (o el wrapper si se agrega)
- Docker (opcional, solo para el perfil PostgreSQL + PostGIS)

## Ejecución

### Perfil `dev` (por defecto) · sin infraestructura

Usa H2 en memoria y siembra el catálogo de zonas automáticamente.

```bash
cd clima-spring
mvn spring-boot:run
# http://localhost:8080
```

### Perfil `postgres` · PostgreSQL + PostGIS

```bash
cd clima-spring

# 1) Levantar la base de datos
docker compose -f db/docker-compose.yml up -d

# 2) Arrancar la aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## API REST

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/weather` | Temperatura, latitud, longitud y última actualización (caché de 15 min). |
| GET | `/api/zonas` | Zonas térmicas con su TSM en tiempo real y nivel de riesgo. |
| GET | `/api/zonas?lat=..&lon=..` | **Filtro por zona geográfica**: solo las zonas que contienen el punto. |

## Cumplimiento de la HU-1

| Tarea | Estado | Dónde |
|---|---|---|
| Investigar fuentes oficiales (temperatura/trayectoria) | ✅ | Open-Meteo `forecast` + `marine-api` |
| Diagrama entidad-relación y tablas necesarias | ✅ | `docs/HU-1-modelo-datos.md` |
| Endpoints REST en tiempo real + filtro por zona geográfica | ✅ | `WeatherController`, `ZonasController` (`?lat&lon`) |
| Base de datos (PostgreSQL + PostGIS) | ✅ | `persistence/`, `application-postgres.properties`, `schema.sql`, `docker-compose.yml` |
| Librería de mapas (Leaflet) | ✅ | `static/index.html`, `static/js/mapa.js` |
| Lógica de colores por temperatura | ✅ | `NivelRiesgo` |
| Conectar el mapa con la API | ✅ | `static/js/mapa.js`, `App.jsx` |
| Refresco cada 15 min sin recargar | ✅ | `AutoRefreshScheduler`, `static/js/clima.js` |

## Alternativa en React (raíz del repo)

En la raíz se conserva el frontend React + Vite que consume las APIs públicas
directamente:

```bash
npm install
npm run dev
```
