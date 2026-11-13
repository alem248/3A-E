from django.urls import path

from . import views

app_name = "clima_admin"
urlpatterns = [
    path("", views.section_page, {"section": {"key": "dashboard", "title": "Panel de control"}}, name="dashboard"),
    path("zonas/", views.section_page, {"section": {"key": "zones", "title": "Zonas"}}, name="zones"),
    path("lecturas/", views.section_page, {"section": {"key": "readings", "title": "Lecturas"}}, name="readings"),
    path("alertas/", views.section_page, {"section": {"key": "alerts", "title": "Alertas"}}, name="alerts"),
    path("estadisticas/", views.section_page, {"section": {"key": "statistics", "title": "Estadísticas"}}, name="statistics"),
    path("configuracion/", views.settings_page, name="settings"),
    path("configuracion/tema/", views.theme_preference, name="theme"),
    path("configuracion/preferencias/", views.save_settings, name="save_settings"),
    path("historial/", views.history_page, name="history"),
    path("api/zonas/<str:zone_id>/historial/", views.zone_history, name="zone_history"),
    path("api/zonas/<str:zone_id>/tendencia/", views.zone_trend, name="zone_trend"),
]
