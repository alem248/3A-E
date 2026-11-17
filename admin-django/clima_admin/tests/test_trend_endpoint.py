from django.contrib.auth import get_user_model
from django.test import TestCase
from unittest.mock import patch


class ZoneTrendEndpointTests(TestCase):
    def setUp(self):
        user = get_user_model().objects.create_user(
            username="trend-admin", password="test-password", is_staff=True
        )
        self.client.force_login(user)

    @patch("clima_admin.views.get_climate_api")
    def test_endpoint_returns_true_for_worsening_pattern(self, get_api):
        get_api.return_value.trend.return_value = {"puntos": [
            {"fecha": "2026-01-01T00:00:00", "valor": 24.0},
            {"fecha": "2026-01-01T06:00:00", "valor": 24.5},
            {"fecha": "2026-01-01T12:00:00", "valor": 25.0},
        ]}

        response = self.client.get("/api/zonas/1/tendencia/")

        self.assertEqual(response.status_code, 200)
        self.assertIs(response.json()["patronEmpeoramiento"], True)
        self.assertEqual(response.json()["registrosEvaluados"], 3)

    @patch("clima_admin.views.get_climate_api")
    def test_endpoint_returns_false_when_fewer_than_three_records(self, get_api):
        get_api.return_value.trend.return_value = {"puntos": [
            {"fecha": "2026-01-01T00:00:00", "valor": 24.0},
            {"fecha": "2026-01-01T06:00:00", "valor": 24.5},
        ]}

        response = self.client.get("/api/zonas/1/tendencia/")

        self.assertEqual(response.status_code, 200)
        self.assertIs(response.json()["patronEmpeoramiento"], False)
        self.assertEqual(response.json()["registrosEvaluados"], 2)
