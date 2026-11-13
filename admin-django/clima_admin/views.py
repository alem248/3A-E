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
    return render(request, "clima_admin/history.html", {
        "zones": zones,
        "regions": PERU_REGIONS,
        "api_unavailable": api_unavailable,
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
