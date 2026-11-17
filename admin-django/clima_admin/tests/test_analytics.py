from django.test import SimpleTestCase

from clima_admin.analytics import worsening_trend


class WorseningTrendTests(SimpleTestCase):
    def test_detects_two_consecutive_increases_over_threshold(self):
        payload = {"puntos": [
            {"fecha": "2026-01-01T00:00:00", "valor": 24.0},
            {"fecha": "2026-01-01T06:00:00", "valor": 24.5},
            {"fecha": "2026-01-01T12:00:00", "valor": 25.0},
        ]}

        self.assertTrue(worsening_trend(payload, min_variation=0.5))

    def test_rejects_readings_below_threshold_or_not_increasing(self):
        payload = {"puntos": [
            {"fecha": "2026-01-01T00:00:00", "valor": 24.0},
            {"fecha": "2026-01-01T06:00:00", "valor": 24.4},
            {"fecha": "2026-01-01T12:00:00", "valor": 25.0},
        ]}

        self.assertFalse(worsening_trend(payload, min_variation=0.5))

    def test_returns_false_with_fewer_than_three_valid_readings(self):
        payload = {"puntos": [
            {"fecha": "2026-01-01T00:00:00", "valor": 24.0},
            {"fecha": "2026-01-01T06:00:00", "valor": 24.5},
            {"fecha": "fecha inválida", "valor": 25.0},
        ]}

        self.assertFalse(worsening_trend(payload, min_variation=0.5))

    def test_sorts_by_timestamp_and_uses_only_the_latest_three(self):
        payload = {"puntos": [
            {"fecha": "2026-01-01T12:00:00", "valor": 25.0},
            {"fecha": "2026-01-01T00:00:00", "valor": 24.0},
            {"fecha": "2025-12-31T00:00:00", "valor": 90.0},
            {"fecha": "2026-01-01T06:00:00", "valor": 24.5},
        ]}

        self.assertTrue(worsening_trend(payload, min_variation=0.5))
