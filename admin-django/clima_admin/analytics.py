from datetime import date, datetime, timedelta

from django.conf import settings
from django.utils import timezone


def worsening_trend(payload, *, min_variation=None, today=None):
    """True when the latest three readings, within the last 3 days, rise by threshold each step."""
    threshold = settings.CLIMATE_TREND_MIN_VARIATION if min_variation is None else min_variation
    today = today or timezone.localdate()
    cutoff = today - timedelta(days=2)
    recent = []
    for point in payload.get("puntos", []):
        try:
            observed = datetime.fromisoformat(point["fecha"].replace("Z", "+00:00")).date()
            value = float(point["valor"])
        except (KeyError, TypeError, ValueError):
            continue
        if cutoff <= observed <= today:
            recent.append((observed, value))
    recent.sort(key=lambda item: item[0])
    latest = recent[-3:]
    return len(latest) == 3 and all(
        latest[index + 1][1] - latest[index][1] >= threshold
        for index in range(2)
    )


