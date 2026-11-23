from django.urls import path

from . import favoritas, noticias, usuarios, views
from .sso import salir

app_name = "clima_admin"
urlpatterns = [
    path("", views.dashboard_page, name="dashboard"),
    path("zonas/", views.section_page, {"section": {"key": "zones", "title": "Zonas"}}, name="zones"),
    path("lecturas/", views.section_page, {"section": {"key": "readings", "title": "Lecturas"}}, name="readings"),
    path("alertas/", views.section_page, {"section": {"key": "alerts", "title": "Alertas"}}, name="alerts"),
    path("estadisticas/", views.section_page, {"section": {"key": "statistics", "title": "Estadísticas"}}, name="statistics"),
    path("configuracion/", views.settings_page, name="settings"),
    path("configuracion/tema/", views.theme_preference, name="theme"),
    path("configuracion/preferencias/", views.save_settings, name="save_settings"),
    path("historial/", views.history_page, name="history"),
    path("salir/", salir, name="salir"),
    path("usuarios/", usuarios.usuarios_lista, name="usuarios"),
    path("favoritas/", favoritas.favoritas_ranking, name="favoritas"),
    path("noticias/", noticias.noticias_lista, name="noticias"),
    path("noticias/nueva/", noticias.noticia_nueva, name="noticia_nueva"),
    path("noticias/<int:noticia_id>/editar/", noticias.noticia_editar, name="noticia_editar"),
    path("noticias/<int:noticia_id>/eliminar/", noticias.noticia_eliminar, name="noticia_eliminar"),
    path("noticias/<int:noticia_id>/imagen/", noticias.noticia_imagen, name="noticia_imagen"),
    path("usuarios/<int:usuario_id>/monitorear/", usuarios.usuario_monitorear, name="usuario_monitorear"),
    path("noticias/<int:noticia_id>/aprobar/", noticias.noticia_cambiar_estado,
         {"accion": "aprobar"}, name="noticia_aprobar"),
    path("noticias/<int:noticia_id>/descartar/", noticias.noticia_cambiar_estado,
         {"accion": "descartar"}, name="noticia_descartar"),
    path("usuarios/nuevo/", usuarios.usuario_nuevo, name="usuario_nuevo"),
    path("usuarios/<int:usuario_id>/eliminar/", usuarios.usuario_eliminar, name="usuario_eliminar"),
    path("usuarios/<int:usuario_id>/", usuarios.usuario_detalle, name="usuario_detalle"),
    path("api/zonas/<str:zone_id>/historial/", views.zone_history, name="zone_history"),
    path("api/zonas/<str:zone_id>/tendencia/", views.zone_trend, name="zone_trend"),
]
