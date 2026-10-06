# Niño Costero - Backend (PostgreSQL + PostGIS + Spring Boot)

API REST para obtener datos en tiempo real del Niño Costero y filtrarlos por zona geográfica.

## 1. Base de datos
Requiere PostgreSQL con la extensión PostGIS instalada.

```bash
createdb ninocostero
psql -d ninocostero -f database/01_schema.sql
psql -d ninocostero -f database/02_seed.sql   # datos de prueba
```

## 2. Ejecutar la API
```bash
export DB_USER=postgres
export DB_PASSWORD=tu_clave
mvn spring-boot:run
```
Corre en `http://localhost:8080`.

## 3. Endpoints (todos GET, devuelven GeoJSON cuando hay geometría)

| Endpoint | Descripción |
|---|---|
| `/api/zonas`, `/api/zonas/{id}` | Zonas vulnerables (polígonos + color de riesgo) |
| `/api/tsm/actual` | Última temperatura del mar por estación |
| `/api/tsm/historial?horas=24` | Historial TSM (1-720 h) |
| `/api/precipitacion/actual` | Última precipitación por estación |
| `/api/estaciones` | Estaciones de monitoreo |
| `/api/trayectoria` | Proyección de trayectoria futura |
| `/api/alertas/activas?zonaId=` | Alertas vigentes con medidas preventivas |
| `/api/centros-ayuda?zonaId=&tipo=` | Refugios, almacenes y hospitales |
| `/api/usuarios/{id}/riesgo` | Zona de riesgo según la última ubicación del usuario |

### Filtro por zona geográfica (en tsm, precipitación y estaciones; usar solo UNO)
- Por zona: `?zonaId=1&radioKm=150` (radio por defecto: 100 km alrededor del polígono)
- Por rectángulo: `?minLat=-7&minLon=-82&maxLat=-4&maxLon=-79`
- Por punto: `?lat=-5.2&lon=-80.6&radioKm=80`

### Ejemplos
```bash
curl "http://localhost:8080/api/tsm/actual?zonaId=1&radioKm=150"
curl "http://localhost:8080/api/tsm/actual?minLat=-7&minLon=-82&maxLat=-4&maxLon=-79"
curl "http://localhost:8080/api/alertas/activas?zonaId=1"
curl "http://localhost:8080/api/usuarios/1/riesgo"
```
