from django.contrib import admin
from django.contrib.auth import views as auth_views
from django.conf import settings
from django.urls import include, path

from clima_admin.views import PanelLoginView, UserRegistrationView, confirm_panel_logout

urlpatterns = [
    path("admin/", admin.site.urls),
    path(
        "cuentas/login/",
        PanelLoginView.as_view(),
        name="panel_login",
    ),
    path("cuentas/registro/", UserRegistrationView.as_view(), name="panel_register"),
    path("cuentas/cerrar-sesion/", confirm_panel_logout, name="panel_logout_confirm"),
    path(
        "cuentas/logout/",
        auth_views.LogoutView.as_view(next_page=f"{settings.LOGIN_URL}?logged_out=1"),
        name="panel_logout",
    ),
    path("cuentas/", include("django.contrib.auth.urls")),
    path("", include("clima_admin.urls")),
]
