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

**Uso en el proyecto.** Conceptualmente corresponde a `registro_precipitacion` y
al pronóstico de temperatura del endpoint `/api/weather`. La integración está
prevista para una segunda iteración.

### 1.3 IMARPE — Instituto del Mar del Perú

**Descripción.** Organismo que realiza el monitoreo satelital y oceanográfico
del litoral peruano. Es vital para detectar la *posición* de las masas de agua
caliente.

**Datos para el MVP.** Temperatura Superficial del Mar (TSM) y picos de
anomalías térmicas (ej. +8.5 °C). Coordenadas de las zonas marítimas de mayor
calentamiento.

**Acceso.** Repositorio Institucional (boletines diarios y semanales
oceanográficos) y estaciones oceanográficas costeras.

**Uso en el proyecto.** Es la fuente de la variable central del MVP. En la
implementación actual la TSM se consulta por API abierta y se normaliza contra
Open-Meteo; el registro se persiste en `registro_tsm`.

### 1.4 INDECI y CENEPRED — Gestión de Riesgo de Desastres

**Descripción.** Entidades rectoras en la prevención de riesgos, escenarios de
vulnerabilidad y respuesta ante desastres.

**Datos para el MVP.** Mapas de susceptibilidad poblacional, polígonos de zonas
inundables y ubicación de centros de recursos de Defensa Civil.

**Acceso.** Plataforma SIGRID, Visor INDECI y la Infraestructura de Datos
Espaciales del Perú (GeoPerú).

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
el MVP sin infraestructura governmenta. El modelo de datos ya contempla
sustituirla por IMARPE, SENAMHI o NOAA sin cambios estructurales: basta actualizar
la fila en `fuente_oficial`.

---

## 2. Estructura de base de datos

Diseño de las 15 tablas fundamentales del sistema, estructuradas de manera
relacional. Esta arquitectura soporta el MVP y la futura escalabilidad,
integrándose con Spring Boot (API web/móvil).

El DER se implementa con Spring Data JPA en
`clima-spring/src/main/java/com/tecsup/clima/persistence/`.

### 2.1 `rol`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| nombre | string | | ✅ |
| descripcion | string | | ✅ |

**Justificación.** Permite el control de acceso basado en roles (RBAC): el
poblador ve su mapa, Defensa Civil gestiona alertas y el administrador
administra el sistema. Catálogo sembrado con los roles POBLADOR, DEFENSA_CIVIL y
ADMIN.

### 2.2 `usuario`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| rol_id | int | FK | ✅ |
| nombres | string | | ✅ |
| email | string | | ✅ |
| fcm_token | string | | ✅ |
| password_hash | string | | ⚠️ No implementado |
| fecha_registro | date | | ⚠️ No implementado |

**Justificación.** Almacena las credenciales y el token de Firebase
(`fcm_token`) necesario para enviar notificaciones push al móvil o alertas al
frontend.

**Pendiente.** El hash de contraseña y la fecha de registro quedan para la
iteración de autenticación; el MVP opera sin login.

### 2.3 `ubicacion_usuario`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| usuario_id | int | FK | ✅ |
| latitud | decimal | | ✅ |
| longitud | decimal | | ✅ |
| ultima_actualizacion | datetime | | ⚠️ No implementado |

**Justificación.** Separa la ubicación del perfil del usuario para permitir
actualizaciones constantes desde el GPS del móvil sin sobrecargar la tabla
principal. Es vital para calcular la distancia a las zonas de riesgo.

### 2.4 `evento_climatico`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| nombre | string | | ✅ |
| fecha_inicio | date | | ✅ |
| estado | string | | ✅ |
| fecha_fin | date | | ⚠️ No implementado |

**Justificación.** Actúa como agrupador macro. Permite un registro histórico: si
ocurre otro fenómeno en el futuro, los datos no se mezclan y se pueden comparar
años distintos.

### 2.5 `fuente_oficial`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| nombre_institucion | string | | ✅ |
| url_api | string | | ✅ |
| tipo_dato | string | | ⚠️ No implementado |

**Justificación.** Registra de dónde proviene la data. Si una API cambia, se
actualiza aquí sin romper la estructura de los registros.

### 2.6 `estacion_monitoreo`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| fuente_id | int | FK | ✅ |
| tipo | string | | ✅ |
| latitud | decimal | | ✅ |
| longitud | decimal | | ✅ |
| nombre | string | | ⚠️ No implementado |

**Justificación.** Identifica los puntos de recolección de datos fijos y da
trazabilidad técnica a los registros mostrados en el mapa interactivo. Los tipos
considerados son Boya, Satélite y Estación Terrestre.

### 2.7 `registro_tsm` — Temperatura Superficial del Mar

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| evento_id | int | FK | ✅ |
| estacion_id | int | FK | ✅ |
| latitud | decimal | | ✅ |
| longitud | decimal | | ✅ |
| anomalia | decimal | | ✅ |
| temperatura | decimal | | ⚠️ No implementado |
| fecha_hora | datetime | | ⚠️ No implementado |

**Justificación.** Es el núcleo del MVP: guarda las coordenadas exactas de las
masas de agua caliente, es decir la *posición e intensidad* que pide la historia
de usuario. La anomalía se calcula respecto del valor baseclimático de 24 °C.

> ⚠️ **Limitación conocida.** El DER persiste solo la *anomalía*. La temperatura
> absoluta se consulta en vivo a la API y se expone en el endpoint, pero no se
> guarda. Sin ella, si los umbrales de riesgo cambian no se puede reclasificar el
> histórico. Se recomienda agregar `temperatura` en la siguiente iteración.

### 2.8 `registro_precipitacion`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| evento_id | int | FK | ✅ |
| milimetros | decimal | | ✅ |
| estacion_id | int | FK | | ⚠️ No implementado |
| milimetros_lluvia | decimal | | | ⚠️ No implementado |
| fecha_hora | datetime | | ⚠️ No implementado |

**Justificación.** El Niño Costero genera lluvias extremas. Cruzar la temperatura
del mar con las precipitaciones en la costa permite anticipar desbordes de ríos.

### 2.9 `proyeccion_trayectoria`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| evento_id | int | FK | ✅ |
| lat_futura | decimal | | ✅ |
| lon_futura | decimal | | ✅ |
| fecha_estimada | date | | ✅ |
| confiabilidad_porcentaje | decimal | | ⚠️ No implementado |

**Justificación.** Satisface el requerimiento de *trayectoria* de la historia de
usuario, mostrando hacia dónde se expandirá la anomalía térmica en los próximos
días.

### 2.10 `nivel_riesgo`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| nombre | string | | ✅ |
| color_hex | string | | ✅ |
| descripcion | string | | ⚠️ No implementado |

**Justificación.** Catálogo estático que estandariza la severidad de las alertas
para que el frontend pinte los mapas con los colores oficiales de Defensa Civil.

> Nota de implementación: los umbrales numéricos (verde < 24 °C, amarillo 24–26 °C,
> naranja 26–28 °C, rojo ≥ 28 °C) viven en el dominio `NivelRiesgo` del código,
> no en la base de datos, para permitir ajustar la lógica sin migración.

### 2.11 `zona_vulnerable`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| nivel_riesgo_id | int | FK | ✅ |
| poligono | string (WKT) | | ✅ |
| nombre_zona | string | | ⚠️ No implementado |

**Justificación.** Define áreas geográficas propensas a inundaciones o huaicos.
Se cruza espacialmente con PostGIS para determinar si un usuario está dentro del
polígono.

> ⚠️ **Limitación conocida.** El polígono se guarda como texto WKT, tal como
> define el DER, y la capa PostGIS agrega la columna `geom geometry(Polygon,4326)`
> con índice GIST. El *nombre* de la zona se resuelve desde el catálogo en código,
> por lo que un nombre nuevo requiere modificar código en lugar de un `INSERT`.

### 2.12 `alerta_preventiva`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| zona_id | int | FK | ✅ |
| nivel_riesgo_id | int | FK | ✅ |
| mensaje | string | | ✅ |
| fecha_emision | datetime | | ⚠️ No implementado |
| vigencia_hasta | datetime | | ⚠️ No implementado |

**Justificación.** Advertencias oficiales generadas por el sistema o por el
administrador de Defensa Civil para informar que una anomalía climática está
afectando una zona.

### 2.13 `medida_preventiva`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| alerta_id | int | FK | ✅ |
| descripcion_accion | string | | ✅ |
| titulo | string | | ⚠️ No implementado |

**Justificación.** Satisface la parte de la historia de usuario: *poder tomar
medidas preventivas*. Brinda instrucciones claras, por ejemplo "Alejarse de la
ribera del río Piura" o "Preparar mochilas de emergencia".

### 2.14 `centro_ayuda`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| zona_id | int | FK | ✅ |
| tipo | string | | ✅ |
| latitud | decimal | | ⚠️ No implementado |
| longitud | decimal | | ⚠️ No implementado |
| aforo | int | | ⚠️ No implementado |

**Justificación.** Proporciona puntos seguros a los pobladores en el mapa
interactivo durante una alerta roja, aportando gran valor social al aplicativo.

### 2.15 `historial_notificacion`

| Campo | Tipo | Clave | Estado |
|---|---|---|---|
| id | int | PK | ✅ |
| usuario_id | int | FK | ✅ |
| alerta_id | int | FK | ✅ |
| leida | boolean | | ✅ |
| fecha_envio | datetime | | ⚠️ No implementado |

**Justificación.** Mantiene el registro de auditoría de los avisos enviados al
poblador. Permite medir si las personas realmente reciben y leen las alertas a
tiempo.

---

## 3. Resumen de cobertura

| Tabla | Estado | Observación |
|---|---|---|
| `rol` | Completa | — |
| `usuario` | Parcial | Faltan `password_hash`, `fecha_registro` |
| `ubicacion_usuario` | Parcial | Falta `ultima_actualizacion` |
| `evento_climatico` | Parcial | Falta `fecha_fin` |
| `fuente_oficial` | Parcial | Falta `tipo_dato` |
| `estacion_monitoreo` | Parcial | Falta `nombre` |
| `registro_tsm` | Parcial | Faltan `temperatura`, `fecha_hora` |
| `registro_precipitacion` | Parcial | Faltan `estacion_id`, `milimetros_lluvia`, `fecha_hora` |
| `proyeccion_trayectoria` | Parcial | Falta `confiabilidad_porcentaje` |
| `nivel_riesgo` | Parcial | Falta `descripcion` (vive en el dominio) |
| `zona_vulnerable` | Parcial | Falta `nombre_zona` |
| `alerta_preventiva` | Parcial | Faltan `fecha_emision`, `vigencia_hasta` |
| `medida_preventiva` | Parcial | Falta `titulo` |
| `centro_ayuda` | Parcial | Faltan `latitud`, `longitud`, `aforo` |
| `historial_notificacion` | Parcial | Falta `fecha_envio` |

Las 15 tablas, sus claves primarias, foráneas y relaciones están implementadas y
operativas. Los campos marcados ⚠️ corresponden a una iteración posterior.

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

Las 15 entidades JPA y sus repositorios están en
`clima-spring/src/main/java/com/tecsup/clima/persistence/`. La capa espacial se
habilita en `clima-spring/src/main/resources/db/postgres/schema.sql` y la base de
datos se levanta con `clima-spring/db/docker-compose.yml`.

### Endpoints REST

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/weather` | Temperatura, latitud, longitud y última actualización |
| GET | `/api/zonas` | Zonas vulnerables con TSM en tiempo real y nivel de riesgo |
| GET | `/api/zonas?lat=..&lon=..` | Filtro por zona geográfica (punto en polígono) |
