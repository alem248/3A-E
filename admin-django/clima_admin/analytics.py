from datetime import datetime, timezone
import math

from django.conf import settings


def worsening_trend(payload, *, min_variation=None):
    """Return true when each of the latest three readings rises by the threshold."""
    threshold = settings.CLIMATE_TREND_MIN_VARIATION if min_variation is None else min_variation
    if not isinstance(payload, dict) or not isinstance(payload.get("puntos"), list):
        return False

    readings = []
    for point in payload["puntos"]:
        try:
            observed = datetime.fromisoformat(point["fecha"].replace("Z", "+00:00"))
            value = float(point["valor"])
        except (AttributeError, KeyError, TypeError, ValueError):
            continue
        if not math.isfinite(value):
            continue
        if observed.tzinfo is not None:
            observed = observed.astimezone(timezone.utc).replace(tzinfo=None)
        readings.append((observed, value))

    latest = sorted(readings, key=lambda item: item[0])[-3:]
    return len(latest) == 3 and all(
        latest[index + 1][1] - latest[index][1] >= threshold
        for index in range(2)
    )
