from datetime import date
import re

from django.conf import settings
from django.contrib.admin.views.decorators import staff_member_required
from django.http import JsonResponse
from django.shortcuts import render
from django.views.decorators.http import require_GET
from django.views.decorators.http import require_POST
from django.shortcuts import redirect
from django.urls import reverse

from .services import JavaApiError, get_climate_api
from .analytics import worsening_trend
from django.utils import timezone

from . import spring_admin
from .spring_admin import SpringAdminError

_ZONE_ID = re.compile(r"^[A-Za-z0-9_-]{1,40}$")
PERU_REGIONS = [
    "Amazonas", "Áncash", "Apurímac", "Arequipa", "Ayacucho", "Cajamarca", "Callao",
    "Cusco", "Huancavelica", "Huánuco", "Ica", "Junín", "La Libertad", "Lambayeque",
    "Lima", "Loreto", "Madre de Dios", "Moquegua", "Pasco", "Piura", "Puno",
    "San Martín", "Tacna", "Tumbes", "Ucayali",
]


@staff_member_required
@require_GET
def history_page(request):
    try:
        zones = get_climate_api().zones()
        if not isinstance(zones, list):
            raise JavaApiError("La API devolvió un catálogo de zonas inválido.")
    except JavaApiError:
        zones = []
        api_unavailable = True
    else:
        api_unavailable = False
    zonas_disponibles = zones if isinstance(zones, list) else []
    count = len(zonas_disponibles)
    active_alerts = sum(
        1 for zone in zonas_disponibles
        if isinstance(zone.get("nivel"), dict)
        and zone["nivel"].get("id") in {"NARANJA", "ROJO"}
    )
    available = sum(1 for zone in zonas_disponibles if zone.get("disponible") is True)
    return render(request, "clima_admin/history.html", {
        "zones": zonas_disponibles,
        "regions": PERU_REGIONS,
        "api_unavailable": api_unavailable,
        "zones_count": count,
        "active_alerts": active_alerts,
        "available_percent": round(100 * available / count) if count else 0,
    })


@staff_member_required
@require_GET
def zone_history(request, zone_id):
    if not _ZONE_ID.fullmatch(zone_id):
        return JsonResponse({"error": "Identificador de zona inválido."}, status=400)

    start = request.GET.get("desde")
    end = request.GET.get("hasta")
    try:
        if start:
            date.fromisoformat(start)
        if end:
            date.fromisoformat(end)
        if start and end and start > end:
            return JsonResponse({"error": "El rango de fechas no es válido."}, status=400)
        payload = get_climate_api().history(zone_id, start, end)
        points = payload["puntos"]
        if len(points) > 5000:
            return JsonResponse({"error": "El rango excede el máximo de 5000 lecturas."}, status=422)
        for point in points:
            if not isinstance(point, dict) or not isinstance(point.get("fecha"), str):
                raise JavaApiError("La API devolvió puntos de historial inválidos.")
            if not isinstance(point.get("valor"), (int, float)):
                raise JavaApiError("La API devolvió puntos de historial inválidos.")
    except ValueError:
        return JsonResponse({"error": "Usa fechas válidas en formato AAAA-MM-DD."}, status=400)
    except JavaApiError:
        return JsonResponse({"error": "No se pudo consultar el historial de la zona."}, status=502)
    return JsonResponse(payload)


@staff_member_required
@require_GET
def zone_trend(request, zone_id):
    if not _ZONE_ID.fullmatch(zone_id):
        return JsonResponse({"error": "Identificador de zona inválido."}, status=400)
    try:
        payload = get_climate_api().trend(zone_id)
        points = payload["puntos"]
        pattern = worsening_trend(payload, min_variation=request.session.get(
            "trend_threshold", settings.CLIMATE_TREND_MIN_VARIATION))
    except (JavaApiError, KeyError, TypeError):
        return JsonResponse({"error": "No se pudo evaluar la tendencia de la zona."}, status=502)
    return JsonResponse({
        "zonaId": zone_id,
        "patronEmpeoramiento": pattern,
        "registrosEvaluados": min(len(points), 3),
        "variacionMinima": request.session.get("trend_threshold", settings.CLIMATE_TREND_MIN_VARIATION),
        "ventanaDias": 3,
    })


@staff_member_required
@require_GET
def section_page(request, section):
    api = get_climate_api()
    context = {"section": section, "title": section["title"], "items": [], "notice": ""}
    try:
        if section["key"] in {"dashboard", "zones", "readings", "statistics"}:
            zones = api.zones()
            context["items"] = zones if isinstance(zones, list) else []
            context["notice"] = "Datos actuales desde Spring. El historial se genera con las lecturas persistidas."
        elif section["key"] == "alerts":
            context["items"] = api.rules()
            context["notice"] = "Reglas configuradas en el backend Spring."
    except JavaApiError:
        context["notice"] = "No fue posible conectar con Spring. Verifica el estado del servicio."
    if section["key"] == "zones":
        items = context["items"]
        context["zones_count"] = len(items)
        context["sin_lectura"] = sum(1 for z in items if z.get("disponible") is not True)
        context["con_riesgo"] = sum(
            1 for z in items
            if isinstance(z.get("nivel"), dict) and z["nivel"].get("id") in {"NARANJA", "ROJO"}
        )
        return render(request, "clima_admin/zonas.html", context)
    if section["key"] == "alerts":
        reglas = context["items"] if isinstance(context["items"], list) else []
        context["items"] = reglas
        context["reglas_total"] = len(reglas)
        context["zonas_con_regla"] = len({r.get("zonaId") for r in reglas if isinstance(r, dict)})
        return render(request, "clima_admin/alertas.html", context)

    if section["key"] == "statistics":
        zonas = context["items"]
        niveles = {}
        departamentos = {}
        temperaturas = []
        for zona in zonas:
            nivel = zona.get("nivel") if isinstance(zona.get("nivel"), dict) else None
            nombre_nivel = nivel.get("nombre") or "Sin nivel" if nivel else "Sin nivel"
            color = (nivel or {}).get("color") or "#64748b"
            entrada = niveles.setdefault(nombre_nivel, {"nombre": nombre_nivel, "color": color, "total": 0})
            entrada["total"] += 1
            region = zona.get("departamento") or "Sin región"
            fila = departamentos.setdefault(region, {"nombre": region, "zonas": 0, "con_riesgo": 0})
            fila["zonas"] += 1
            if nivel and nivel.get("id") in {"NARANJA", "ROJO"}:
                fila["con_riesgo"] += 1
            try:
                temperaturas.append(float(zona["tsm"]))
            except (KeyError, TypeError, ValueError):
                pass
        context.update({
            "zones_count": len(zonas),
            "disponibles": sum(1 for zona in zonas if zona.get("disponible") is True),
            "niveles": sorted(niveles.values(), key=lambda n: -n["total"]),
            "departamentos": sorted(departamentos.values(), key=lambda d: (-d["zonas"], d["nombre"])),
            "temp_promedio": round(sum(temperaturas) / len(temperaturas), 1) if temperaturas else None,
            "temp_min": min(temperaturas) if temperaturas else None,
            "temp_max": max(temperaturas) if temperaturas else None,
            "spark_temps": _puntos_linea(temperaturas),
        })
        return render(request, "clima_admin/estadisticas.html", context)

    return render(request, "clima_admin/section.html", context)


@staff_member_required
@require_GET
def settings_page(request):
    error = request.session.pop("settings_error", "")
    success = request.session.pop("settings_success", "")
    return render(request, "clima_admin/settings.html", {
        "threshold": request.session.get("trend_threshold", settings.CLIMATE_TREND_MIN_VARIATION),
        "error": error,
        "success": success,
    })


@staff_member_required
@require_POST
def save_settings(request):
    try:
        threshold = float(request.POST.get("trend_threshold", ""))
        if not 0 < threshold <= 10:
            raise ValueError
    except ValueError:
        request.session["settings_error"] = "El umbral debe ser mayor que 0 y no superar 10 °C."
    else:
        request.session["trend_threshold"] = threshold
        request.session["settings_success"] = "Preferencias guardadas para esta sesión."
    return redirect("clima_admin:settings")


@staff_member_required
@require_POST
def theme_preference(request):
    next_url = request.POST.get("next", "")
    from django.utils.http import url_has_allowed_host_and_scheme
    if not url_has_allowed_host_and_scheme(next_url, allowed_hosts={request.get_host()}):
        next_url = reverse("clima_admin:dashboard")
    return redirect(next_url)


def _puntos_linea(valores, ancho=90, alto=28):
    """Puntos para un polyline SVG a partir de valores reales (sin datos inventados)."""
    if len(valores) < 2:
        return ""
    minimo, maximo = min(valores), max(valores)
    rango = (maximo - minimo) or 1
    paso = ancho / (len(valores) - 1)
    return " ".join(f"{i * paso:.1f},{alto - (v - minimo) / rango * (alto - 4) - 2:.1f}"
                    for i, v in enumerate(valores))


@staff_member_required
@require_GET
def dashboard_page(request):
    api = get_climate_api()
    cookie = request.headers.get("Cookie", "")

    zonas, api_error = [], False
    try:
        datos = api.zones()
        zonas = datos if isinstance(datos, list) else []
    except JavaApiError:
        api_error = True

    usuarios_total = None
    try:
        usuarios = spring_admin.listar_usuarios(cookie)
        usuarios_total = len(usuarios) if isinstance(usuarios, list) else None
    except SpringAdminError:
        usuarios_total = None

    temperatura = None
    try:
        clima = api.weather()
        if isinstance(clima, dict) and isinstance(clima.get("temperature"), (int, float)):
            temperatura = clima["temperature"]
    except JavaApiError:
        temperatura = None

    orden = {"ROJO": 0, "NARANJA": 1, "AMARILLO": 2, "VERDE": 3}
    con_riesgo = [z for z in zonas if isinstance(z.get("nivel"), dict)
                  and z["nivel"].get("id") in {"ROJO", "NARANJA", "AMARILLO"}]
    con_riesgo.sort(key=lambda z: orden.get(z["nivel"].get("id"), 9))
    alertas_activas = sum(1 for z in zonas if isinstance(z.get("nivel"), dict)
                          and z["nivel"].get("id") in {"ROJO", "NARANJA"})

    niveles = {}
    for zona in zonas:
        nivel = zona.get("nivel")
        if isinstance(nivel, dict):
            registro = niveles.setdefault(nivel.get("id"), {
                "id": nivel.get("id"), "nombre": nivel.get("nombre"), "color": nivel.get("color"), "total": 0})
            registro["total"] += 1

    ubicaciones, ubicaciones_error = [], False
    try:
        datos_ubicaciones = spring_admin.listar_ubicaciones(cookie)
        ubicaciones = datos_ubicaciones if isinstance(datos_ubicaciones, list) else []
    except SpringAdminError:
        ubicaciones_error = True
    niveles_por_zona = {z.get("nombre"): (z.get("nivel") or {}).get("id") for z in zonas if isinstance(z, dict)}
    for ubicacion in ubicaciones:
        ubicacion["nivel"] = niveles_por_zona.get(ubicacion.get("zonaNombre"))

    temperaturas = [z["tsm"] for z in zonas if isinstance(z.get("tsm"), (int, float))]
    temperaturas_riesgo = [z["tsm"] for z in con_riesgo if isinstance(z.get("tsm"), (int, float))]
    lista_niveles = sorted(niveles.values(), key=lambda n: orden.get(n["id"], 9))
    total_niveles = sum(n["total"] for n in lista_niveles)
    partes, acumulado = [], 0.0
    for n in lista_niveles:
        porcentaje = n["total"] / total_niveles * 100 if total_niveles else 0
        partes.append(f"{n['color'] or '#64748b'} {acumulado:.2f}% {acumulado + porcentaje:.2f}%")
        acumulado += porcentaje
    donut = f"conic-gradient({', '.join(partes)})" if partes else "conic-gradient(#1e3a5f 0% 100%)"

    return render(request, "clima_admin/dashboard.html", {
        "title": "Panel de administración",
        "ubicaciones": ubicaciones,
        "ubicaciones_error": ubicaciones_error,
        "spark_zonas": _puntos_linea(temperaturas),
        "spark_alertas": _puntos_linea(temperaturas_riesgo),
        "donut": donut,
        "zonas": zonas,
        "zonas_riesgo": con_riesgo[:6],
        "api_error": api_error,
        "usuarios_total": usuarios_total,
        "alertas_activas": alertas_activas,
        "temperatura": temperatura,
        "niveles": sorted(niveles.values(), key=lambda n: orden.get(n["id"], 9)),
        "ahora": timezone.localtime(),
    })

