from django.contrib import admin
from django.contrib.auth import views as auth_views
from django.urls import include, path, reverse_lazy

from clima_admin.forms import PanelAuthenticationForm

urlpatterns = [
    path("admin/", admin.site.urls),
    path(
        "cuentas/login/",
        auth_views.LoginView.as_view(
            authentication_form=PanelAuthenticationForm,
            template_name="registration/login.html",
        ),
        name="panel_login",
    ),
    path(
        "cuentas/logout/",
        auth_views.LogoutView.as_view(next_page=reverse_lazy("panel_login")),
        name="panel_logout",
    ),
    path("cuentas/", include("django.contrib.auth.urls")),
    path("", include("clima_admin.urls")),
]
