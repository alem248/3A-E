from django.urls import path

from . import views

app_name = "clima_admin"
urlpatterns = [
    path("historial/", views.history_page, name="history"),
    path("api/zonas/<str:zone_id>/historial/", views.zone_history, name="zone_history"),
]
