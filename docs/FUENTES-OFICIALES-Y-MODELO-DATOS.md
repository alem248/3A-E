# Fuentes Oficiales de Datos Climáticos — MVP Niño Costero

> **Proyecto:** Clima Zona Costera · **Historia de usuario:** HU-1
> **Institución:** Tecsup
> Documento de investigación técnica y modelo de datos.

---

## 1. Fuentes oficiales de datos climáticos

A continuación se detallan las principales fuentes oficiales del Estado Peruano e
internacionales que proveen datos verídicos y confiables para documentar el clima,
la temperatura del mar y modelar el desplazamiento de anomalías térmicas
(El Niño Costero). Esta información sirve como base para el MVP desarrollado en
Tecsup.

### 1.1 ENFEN — Estudio Nacional del Fenómeno El Niño

**Descripción.** Entidad científica oficial multisectorial en Perú. Es la
autoridad principal que consolida y emite los comunicados oficiales sobre el
estado del fenómeno El Niño.

**Datos para el MVP.** Estado de alerta oficial (Inactivo, Alerta, Activo), el
Índice Costero El Niño (ICEN) y la magnitud esperada (débil, moderado, fuerte,
extraordinario).

**Acceso.** Boletines quincenales, informes técnicos y comunicados oficiales.
Proporciona la narrativa y el estado global que la aplicación debe mostrar.

**Uso en el proyecto.** Alimenta la tabla `evento_climatico`: el evento activo
("Niño Costero") se registra con su fecha de inicio y su estado, que es el
agrupador macro de todas las lecturas del MVP.

### 1.2 SENAMHI — Servicio Nacional de Meteorología e Hidrología

**Descripción.** Institución encargada de la información meteorológica,
hidrológica y climática a nivel nacional.

**Datos para el MVP.** Datos de precipitación, temperatura del aire y pronóstico
de lluvias extremas en la costa norte (Piura, Tumbes, Lambayeque). Datos
espaciales interpolados (Grilla PISCO).

**Acceso.** Portal de Datos Abiertos del SENAMHI, repositorios del SINIA y API
del Sistema Integrado de Información de los Recursos Hídricos (SIIRH).

**Uso en el proyecto.** Corresponde a la tabla `registro_precipitacion` y al
pronóstico de temperatura del endpoint `/api/weather`. La integración con la
Grilla PISCO queda para una iteración posterior.

### 1.3 IMARPE — Instituto del Mar del Perú

**Descripción.** Organismo que realiza el monitoreo satelital y oceanográfico
del litoral peruano. Es vital para detectar la *posición* de las masas de agua
caliente.

**Datos para el MVP.** Temperatura Superficial del Mar (TSM) y picos de
anomalías térmicas (ej. +8.5 °C). Coordenadas de las zonas marítimas de mayor
calentamiento.

**Acceso.** Repositorio Institucional (boletines diarios y semanales
oceanográficos) y estaciones oceanográficas costeras.

**Uso en el proyecto.** Es la fuente de la variable central del MVP. Alimenta la
tabla `estacion_monitoreo` (los puntos de medición costeros) y sus lecturas se
registran en `registro_tsm`.

### 1.4 INDECI y CENEPRED — Gestión de Riesgo de Desastres

**Descripción.** Entidades rectoras en la prevención de riesgos, escenarios de
vulnerabilidad y respuesta ante desastres.

**Datos para el MVP.** Mapas de susceptibilidad poblacional, polígonos de zonas
inundables y ubicación de centros de recursos de Defensa Civil.

**Acceso.** Plataforma SIGRID (Sistema de Información para la Gestión del Riesgo
de Desastres), Visor INDECI y la Infraestructura de Datos Espaciales del Perú
(GeoPerú).

**Uso en el proyecto.** Sostiene las tablas `zona_vulnerable` (polígonos de
vulnerabilidad), `centro_ayuda` y `alerta_preventiva`.

### 1.5 NOAA — National Oceanic and Atmospheric Administration

**Descripción.** Agencia estadounidense con monitoreo satelital global. Aunque es
extranjera, sus datos satelitales abiertos son la base técnica de muchas
aplicaciones climáticas peruanas.

**Datos para el MVP.** Bases de datos Raster y coordenadas lat/lon diarias de la
Temperatura Superficial del Mar global (OISST).

**Acceso.** APIs públicas gratuitas. Altamente recomendable para programar y
automatizar la extracción del polígono de calor en la costa peruana en tiempo
real.

**Uso en el proyecto.** Alternativa técnica recomendada para escalar la detección
del polígono de calor por encima de la consulta punto a punto actual.

### 1.6 Fuente efectivamente integrada en el MVP

| Aspecto | Detalle |
|---|---|
| Institución | Open-Meteo (aggregator de NOAA/ECMWF) |
| Endpoint | `https://marine-api.open-meteo.com/v1/marine` |
| Variable | `sea_surface_temperature` (TSM) |
| Tabla `fuente_oficial` | id 1 · "Open-Meteo" · url de la API |

Se eligió por ser una API pública, sin clave y estable, lo que permite ejecutar
el MVP sin infraestructura governmenta. El modelo ya contempla sustituirla por
IMARPE, SENAMHI o NOAA sin cambios estructurales: basta actualizar la fila en
`fuente_oficial`, tal como está designed para ello.

---

## 2. Estructura de base de datos

Diseño de las 15 tablas fundamentales del sistema, estructuradas de manera
relacional. Esta arquitectura soporta el MVP y la futura escalabilidad,
integrándose con Spring Boot (API web/móvil).

El DER se implementa con Spring Data JPA en
`clima-spring/src/main/java/com/tecsup/clima/persistence/`.

### 2.1 `rol`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| nombre | string | |
| descripcion | string | |

**Justificación.** Permite el control de acceso basado en roles (RBAC): el
poblador ve su mapa, Defensa Civil gestiona alertas y el administrador
administra el sistema. Catálogo con los roles POBLADOR, DEFENSA_CIVIL y ADMIN.

### 2.2 `usuario`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| rol_id | int | FK |
| nombres | string | |
| email | string | |
| fcm_token | string | |

**Justificación.** Almacena la identidad del usuario y el token de Firebase
(`fcm_token`) necesario para enviar notificaciones push al móvil o alertas al
frontend.

### 2.3 `ubicacion_usuario`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| usuario_id | int | FK |
| latitud | decimal | |
| longitud | decimal | |

**Justificación.** Separa la ubicación del perfil del usuario para permitir
actualizaciones constantes desde el GPS del móvil sin sobrecargar la tabla
principal. Es vital para calcular la distancia a las zonas de riesgo.

### 2.4 `evento_climatico`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| nombre | string | |
| fecha_inicio | date | |
| estado | string | |

**Justificación.** Actúa como agrupador macro. Permite un registro histórico: si
ocurre otro fenómeno en el futuro, los datos no se mezclan y se pueden comparar
años distintos.

### 2.5 `fuente_oficial`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| nombre_institucion | string | |
| url_api | string | |

**Justificación.** Registra de dónde proviene la data. Si una API cambia, se
actualiza aquí sin romper la estructura de los registros.

### 2.6 `estacion_monitoreo`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| fuente_id | int | FK |
| tipo | string | |
| latitud | decimal | |
| longitud | decimal | |

**Justificación.** Identifica los puntos de recolección de datos fijos y da
trazabilidad técnica a los registros mostrados en el mapa interactivo. Los tipos
considerados son Boya, Satélite y Estación Terrestre.

### 2.7 `registro_tsm` — Temperatura Superficial del Mar

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| evento_id | int | FK |
| estacion_id | int | FK |
| latitud | decimal | |
| longitud | decimal | |
| anomalia | decimal | |

**Justificación.** Es el núcleo del MVP. Guarda las coordenadas exactas de las
masas de agua caliente, es decir la *posición* solicitada en la historia de
usuario, y la `anomalia` cuantifica su *intensidad* respecto del valor
baseclimático de la costa (24 °C).

### 2.8 `registro_precipitacion`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| evento_id | int | FK |
| milimetros | decimal | |

**Justificación.** El Niño Costero genera lluvias extremas. Cruzar la
precipitación con la temperatura del mar en la costa permite anticipar
desbordes de ríos.

### 2.9 `proyeccion_trayectoria`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| evento_id | int | FK |
| lat_futura | decimal | |
| lon_futura | decimal | |
| fecha_estimada | date | |

**Justificación.** Satisface el requerimiento de *trayectoria* de la historia de
usuario, mostrando hacia dónde se expandirá la anomalía térmica en los próximos
días.

### 2.10 `nivel_riesgo`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| nombre | string | |
| color_hex | string | |

**Justificación.** Catálogo estático. Estandariza la severidad de las alertas
para que el frontend pinte los mapas y la interfaz con los colores oficiales de
Defensa Civil.

> **Nota de implementación.** Los umbrales numéricos (verde < 24 °C, amarillo
> 24–26 °C, naranja 26–28 °C, rojo ≥ 28 °C) se aplican en el dominio `NivelRiesgo`
> del código, no en la base de datos, porque el DER define este catálogo solo
> con nombre y color.

### 2.11 `zona_vulnerable`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| nivel_riesgo_id | int | FK |
| poligono | string | |

**Justificación.** Define áreas geográficas propensas a inundaciones o huaicos.
Se puede cruzar espacialmente con PostGIS para saber si un usuario está dentro
del polígono.

> **Nota de implementación.** El DER define `poligono` como texto, por lo que se
> almacena en formato WKT. En el perfil `postgres` se agrega la capa espacial:
> columna `geom geometry(Polygon,4326)` con índice GIST, poblada desde el WKT con
> `ST_SetSRID(ST_GeomFromText(poligono), 4326)`.

### 2.12 `alerta_preventiva`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| zona_id | int | FK |
| nivel_riesgo_id | int | FK |
| mensaje | string | |

**Justificación.** Las advertencias oficiales generadas por el sistema o por el
administrador de Defensa Civil para informar que una anomalía climática está
afectando una zona.

### 2.13 `medida_preventiva`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| alerta_id | int | FK |
| descripcion_accion | string | |

**Justificación.** Satisface la parte de la historia de usuario: *poder tomar
medidas preventivas*. Brinda instrucciones claras, por ejemplo "Alejarse de la
ribera del río Piura" o "Preparar mochilas de emergencia".

### 2.14 `centro_ayuda`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| zona_id | int | FK |
| tipo | string | |

**Justificación.** Proporciona puntos seguros a los pobladores en el mapa
interactivo durante una alerta roja, aportando gran valor social al aplicativo.
Tipos considerados: Refugio, Almacén y Hospital.

### 2.15 `historial_notificacion`

| Campo | Tipo | Clave |
|---|---|---|
| id | int | PK |
| usuario_id | int | FK |
| alerta_id | int | FK |
| leida | boolean | |

**Justificación.** Mantiene el registro de auditoría de los avisos enviados al
poblador. Permite medir si las personas realmente están recibiendo y leyendo las
alertas a tiempo.

---

## 3. Relaciones del DER

| Origen | Relación | Destino |
|---|---|---|
| `rol` | tiene | `usuario` |
| `usuario` | registra | `ubicacion_usuario` |
| `usuario` | recibe | `historial_notificacion` |
| `fuente_oficial` | administra | `estacion_monitoreo` |
| `estacion_monitoreo` | mide | `registro_tsm` |
| `estacion_monitoreo` | contiene | `registro_precipitacion` |
| `evento_climatico` | contiene | `registro_tsm` |
| `evento_climatico` | contiene | `registro_precipitacion` |
| `evento_climatico` | proyecta | `proyeccion_trayectoria` |
| `nivel_riesgo` | clasifica | `zona_vulnerable` |
| `nivel_riesgo` | categoriza | `alerta_preventiva` |
| `zona_vulnerable` | presenta | `alerta_preventiva` |
| `zona_vulnerable` | ubica | `centro_ayuda` |
| `alerta_preventiva` | genera | `historial_notificacion` |
| `alerta_preventiva` | detalla | `medida_preventiva` |

El diagrama completo en formato Mermaid está en
[`HU-1-modelo-datos.md`](HU-1-modelo-datos.md).

---

## 4. Implementación técnica

| Aspecto | Detalle |
|---|---|
| Framework | Spring Boot 3.5.15 · Java 17 · Maven |
| Persistencia | Spring Data JPA (Hibernate 6) |
| Base de datos | PostgreSQL 16 + PostGIS 3.4 (perfil `postgres`) |
| Perfil de desarrollo | H2 en memoria (perfil `dev`), sin Docker |
| Mapa | Leaflet 1.9.4 |
| Fuente de datos | Open-Meteo marine-api (TSM) y forecast-api (temperatura del aire) |

Las 15 entidades JPA y sus 15 repositorios están en
`clima-spring/src/main/java/com/tecsup/clima/persistence/`. La capa espacial se
habilita en `clima-spring/src/main/resources/db/postgres/schema.sql` y la base de
datos se levanta con `clima-spring/db/docker-compose.yml`.

### Endpoints REST

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/weather` | Temperatura, latitud, longitud y última actualización |
| GET | `/api/zonas` | Zonas vulnerables con TSM en tiempo real y nivel de riesgo |
| GET | `/api/zonas?lat=..&lon=..` | Filtro por zona geográfica (punto en polígono) |
