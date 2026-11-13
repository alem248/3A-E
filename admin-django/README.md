# Módulo administrativo Django

Panel de consulta desacoplado del backend Spring. Django autentica sus propias cuentas y consulta Spring desde el servidor, sin exponer credenciales de servicio al navegador. Las cuentas administradoras (`is_staff`) pueden cambiar la configuración y gestionar cuentas; las cuentas de usuario tienen acceso de solo lectura a las secciones del panel.

## Requisitos y ejecución local

- Python 3.11+
- Spring Boot ejecutándose en `http://localhost:8081`
- El endpoint de zonas existente `GET /api/zonas`
- Un endpoint de historial de lecturas conforme al contrato inferior

```powershell
cd admin-django
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:DJANGO_SECRET_KEY = "clave-local-larga-y-aleatoria"
$env:DJANGO_DEBUG = "true"
$env:JAVA_API_BASE_URL = "http://localhost:8081"
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver 8000
```

Abre `http://localhost:8000/`; si no has iniciado sesión, el panel te lleva a `/cuentas/login/`. Elige el tipo de cuenta que corresponde a tus credenciales:

- **Administrador:** crea un superusuario con `python manage.py createsuperuser`, o usa una cuenta marcada como staff. Puede consultar el panel, cambiar el umbral y gestionar usuarios.
- **Usuario:** un administrador debe crear la cuenta desde **Gestión de usuarios** y dejar desactivado el permiso de personal (`is_staff`). Puede consultar las secciones y el historial; las opciones de configuración y administración quedan restringidas.

Las cuentas y contraseñas se guardan en la base SQLite local de Django (`db.sqlite3`) y son independientes de las cuentas de Spring. El panel de administración nativo de Django está en `/admin/`. El historial se abre en `/historial/`; la selección de una zona consulta el proxy Django con Fetch. Django consulta Spring desde el servidor, valida la respuesta y devuelve JSON; el cliente actualiza Chart.js sin recargar la página.

## Contrato que debe exponer Spring

Spring expone `GET /api/zonas` para el selector y el endpoint de lecturas históricas implementado en `AdminHistoryController`:

```http
GET /api/v1/admin/zonas/{zoneId}/historial?desde=2026-01-01&hasta=2026-01-31
```

Respuesta JSON esperada:

```json
{
  "zonaId": "1",
  "zonaNombre": "Tumbes",
  "unidad": "°C",
  "umbralAlerta": 27.0,
  "alerta": false,
  "puntos": [
    { "fecha": "2026-01-01T00:00:00Z", "valor": 25.4 },
    { "fecha": "2026-01-02T00:00:00Z", "valor": 27.2 }
  ]
}
```

`fecha` es ISO-8601 y `valor` numérico. El servicio convierte la anomalía almacenada a TSM sumando la base climática de 24 °C. `alerta` y el umbral de 28 °C se derivan del nivel ROJO. El endpoint consulta únicamente datos de los últimos 30 días por defecto y admite un rango máximo de cinco años.

## Integración paso a paso

1. Levanta Spring en el puerto `8081` y verifica `GET http://localhost:8081/api/zonas`.
2. Ejecuta la aplicación Spring una vez para que Hibernate agregue `fecha_observacion` en la tabla existente. Las filas históricas previas no tienen fecha; se incluirán nuevas lecturas cuando Spring las registre. El endpoint solo expone el DTO público.
3. Configura `JAVA_API_BASE_URL`, `JAVA_API_HISTORY_PATH`, `DJANGO_SECRET_KEY` y `DJANGO_ALLOWED_HOSTS` en el entorno de despliegue. Usa HTTPS/TLS entre servicios y guarda secretos fuera del repositorio.
4. Instala dependencias, migra el almacenamiento local de sesiones y crea un superusuario. Crea las cuentas de usuario desde **Gestión de usuarios** y deja `is_staff` desactivado; `is_staff` reserva la configuración y la administración nativa a personal autorizado.
5. Publica `/historial/` detrás de HTTPS y restringe Spring a la red interna o añade autenticación de servicio al endpoint administrativo antes de exponerlo fuera de ella. La plantilla obtiene las zonas; al seleccionar una, `history.js` consulta Django y actualiza el gráfico y la alerta.

## Estructura

- `config/`: settings mínimos, URL raíz y WSGI.
- `clima_admin/views.py`: autorización staff, validación de filtros y proxy JSON.
- `clima_admin/services.py`: adaptador HTTP aislado para Spring.
- `clima_admin/templates/`: interfaz Bootstrap y selector.
- `clima_admin/static/clima_admin/js/history.js`: Fetch, Chart.js y estado visual de alerta.
- `clima-spring/.../AdminHistoryController.java`: API HTTP de historial para el módulo Django.

Bootstrap y Chart.js se cargan desde CDN; en producción, fija CSP y política de integridad de recursos según el proceso de despliegue, o sirve versiones revisadas desde archivos estáticos locales.

## Evaluación automática de tendencia

Django expone `GET /api/zonas/{zoneId}/tendencia/`, protegido con la misma sesión staff. El servicio pide a Spring las lecturas de los últimos tres días y devuelve `patronEmpeoramiento` como booleano, junto con la ventana y la variación mínima configurada. El patrón se activa si existen al menos tres lecturas dentro de la ventana y cada uno de los dos cambios consecutivos sube al menos el umbral configurado (por defecto `0.5 °C`). El indicador se muestra al lado del gráfico. Ajusta `CLIMATE_TREND_MIN_VARIATION` en el entorno, por ejemplo `1.0` para exigir un aumento de 1 °C entre lecturas consecutivas.

Al cambiar de zona, el navegador solicita simultáneamente el historial y la evaluación de tendencia. La evaluación usa lecturas históricas de Spring; no crea un modelo Django paralelo ni duplica el repositorio de datos.

## Interfaz administrativa unificada

La plantilla `templates/admin/base_site.html` redefine el branding, la navegación lateral y el control de tema para las páginas estándar de Django Admin, incluidos los formularios y la gestión de usuarios. Las páginas del panel extienden esa misma plantilla base. El tema claro/oscuro se conserva en `localStorage` del navegador. La sección Configuración guarda el umbral de tendencia en la sesión activa; para un valor global persistente, establece `CLIMATE_TREND_MIN_VARIATION` en el entorno.

El selector del historial contiene las 25 regiones del Perú y filtra las zonas que realmente llegan desde `GET /api/zonas`. La API actual solo tiene cobertura para Tumbes, Piura, Lambayeque y La Libertad (seis estaciones); las otras regiones aparecen en el selector, pero informan que todavía no tienen cobertura. No se fabrican datos para completar regiones faltantes. Zonas, lecturas actuales y reglas usan los endpoints existentes de Spring; los indicadores estadísticos sin API histórica permanecen como no disponibles.

Las páginas propias del panel extienden `clima_admin/base.html`, que a su vez reutiliza la plantilla global de Django Admin (`templates/admin/base_site.html`). El historial presenta tarjetas compactas con valores derivados del catálogo actual de Spring y un enlace al conteo de alertas activas. La disponibilidad mostrada corresponde al porcentaje de zonas con lectura actual, no al uptime del servicio.
