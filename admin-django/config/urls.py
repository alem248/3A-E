from django.conf import settings
from django.contrib import admin
from django.urls import include, path

from clima_admin.sso import entrar_desde_spring

admin.site.site_url = settings.SPRING_WEB_URL

urlpatterns = [
    path("admin/", admin.site.urls),
    path("cuentas/desde-spring/", entrar_desde_spring, name="entrar_desde_spring"),
    path("cuentas/", include("django.contrib.auth.urls")),
    path("", include("clima_admin.urls")),
]
