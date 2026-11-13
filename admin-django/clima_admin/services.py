from django.conf import settings
from django.core.exceptions import ImproperlyConfigured
import httpx
from datetime import timedelta
from django.utils import timezone


class JavaApiError(Exception):
    """The Spring API could not provide a valid response."""


class JavaClimateApi:
    """HTTP adapter; Java remains the system of record for climate data."""

    def __init__(self, client=None):
        self.client = client or httpx.Client(
            base_url=settings.JAVA_API_BASE_URL,
            timeout=settings.JAVA_API_TIMEOUT_SECONDS,
            headers={"Accept": "application/json"},
        )

    def zones(self):
        return self._get_json("/api/zonas")

    def history(self, zone_id, start=None, end=None):
        path = settings.JAVA_API_HISTORY_PATH.format(zone_id=zone_id)
        params = {key: value for key, value in {"desde": start, "hasta": end}.items() if value}
        payload = self._get_json(path, params=params)
        if not isinstance(payload, dict) or not isinstance(payload.get("puntos"), list):
            raise JavaApiError("La API devolvió un historial con formato inválido.")
        return payload

    def trend(self, zone_id):
        today = timezone.localdate()
        return self.history(zone_id, (today - timedelta(days=2)).isoformat(), today.isoformat())

    def _get_json(self, path, params=None):
        try:
            response = self.client.get(path, params=params)
            response.raise_for_status()
            return response.json()
        except (httpx.HTTPError, ValueError) as exc:
            raise JavaApiError("No se pudo consultar el servicio climático.") from exc


def get_climate_api():
    if not settings.JAVA_API_BASE_URL:
        raise ImproperlyConfigured("Configura JAVA_API_BASE_URL para conectar Django con Spring.")
    return JavaClimateApi()


