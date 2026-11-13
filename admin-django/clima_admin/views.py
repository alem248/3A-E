from datetime import date
import re

from django.contrib.admin.views.decorators import staff_member_required
from django.http import JsonResponse
from django.shortcuts import render
from django.views.decorators.http import require_GET

from .services import JavaApiError, get_climate_api

_ZONE_ID = re.compile(r"^[A-Za-z0-9_-]{1,40}$")


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
