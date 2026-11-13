# Módulo administrativo Django

Panel de solo consulta desacoplado del backend Spring. Spring mantiene autenticación de negocio y es la fuente de verdad; Django protege las páginas con usuarios `is_staff` y consulta Spring desde el servidor, sin exponer credenciales de servicio al navegador.

## Requisitos y ejecución local

- Python 3.11+
- Spring Boot ejecutándose en `http://localhost:8080`
- El endpoint de zonas existente `GET /api/zonas`
- Un endpoint de historial de lecturas conforme al contrato inferior

```powershell
cd admin-django
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:DJANGO_SECRET_KEY = "clave-local-larga-y-aleatoria"
$env:DJANGO_DEBUG = "true"
$env:JAVA_API_BASE_URL = "http://localhost:8080"
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver 8000
```

Inicia sesión en `/cuentas/login/` con un usuario staff y abre `/historial/`. La selección de una zona consulta el proxy Django con Fetch; Django realiza la llamada de servidor a servidor a Spring, valida los datos y devuelve JSON. El cliente cancela peticiones obsoletas y vuelve a dibujar Chart.js sin refrescar la página.

## Contrato que debe exponer Spring

El proyecto ya expone `GET /api/zonas`, que alimenta el selector, pero no tiene endpoint de lecturas históricas. Añade en Spring, con sus servicios y repositorios existentes, este endpoint de solo lectura:

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

`fecha` debe ser ISO-8601 y `valor` numérico. `alerta` es opcional; cuando no se proporciona, la interfaz enciende el indicador si alguna lectura alcanza `umbralAlerta`. Apunta `JAVA_API_HISTORY_PATH` a la ruta implementada si difiere. No se consulta SQL compartido: el modelo `registro_tsm` actual no expone fecha/valor TSM directamente y un API evita acoplar dos aplicaciones al esquema interno.

## Integración paso a paso

1. Levanta Spring y verifica `GET /api/zonas`.
2. Implementa el endpoint de historial en Java. Consulta y ordena por fecha en el servicio/repositorio; filtra por zona y rango, impone un límite razonable y devuelve exclusivamente el DTO público documentado. Añade autorización para el rol administrador o protege el endpoint mediante red privada/credenciales de servicio.
3. Configura `JAVA_API_BASE_URL`, `JAVA_API_HISTORY_PATH`, `DJANGO_SECRET_KEY` y `DJANGO_ALLOWED_HOSTS` en el entorno de despliegue. Usa HTTPS/TLS entre servicios y guarda secretos fuera del repositorio.
4. Instala dependencias, migra el almacenamiento local de sesiones y crea un usuario staff. `is_staff` habilita historial y el admin Django; controla esos usuarios desde el sistema operativo de Django.
5. Publica `/historial/` detrás de HTTPS. La plantilla obtiene las zonas; al seleccionar una, `history.js` llama al endpoint JSON interno y actualiza el gráfico y la alerta.

## Estructura

- `config/`: settings mínimos, URL raíz y WSGI.
- `clima_admin/views.py`: autorización staff, validación de filtros y proxy JSON.
- `clima_admin/services.py`: adaptador HTTP aislado para Spring.
- `clima_admin/templates/`: interfaz Bootstrap y selector.
- `clima_admin/static/clima_admin/js/history.js`: Fetch, Chart.js y estado visual de alerta.

Bootstrap y Chart.js se cargan desde CDN; en producción, fija CSP y política de integridad de recursos según el proceso de despliegue, o sirve versiones revisadas desde archivos estáticos locales.
