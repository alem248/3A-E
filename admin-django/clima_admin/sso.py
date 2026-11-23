"""
Acceso al panel de administracion usando la sesion ya iniciada en Spring.

Spring y Django corren en el mismo equipo (localhost), asi que el navegador
envia la misma cookie de sesion a ambos. Django le pregunta a Spring quien
esta conectado y, si es ADMIN, abre la sesion de Django sin pedir contrasena.
"""
from urllib.parse import quote

import httpx
from django.conf import settings
from django.contrib.auth import get_user_model, login, logout
from django.shortcuts import redirect
from django.utils.http import url_has_allowed_host_and_scheme
from django.views.decorators.http import require_GET, require_POST


def _datos_sesion_spring(request):
    cookie = request.headers.get("Cookie", "")
    if not cookie:
        return None
    try:
        respuesta = httpx.get(
            f"{settings.SPRING_WEB_URL}/api/auth/yo",
            headers={"Cookie": cookie},
            timeout=5,
        )
        respuesta.raise_for_status()
        return respuesta.json()
    except (httpx.HTTPError, ValueError):
        return None


@require_GET
def entrar_desde_spring(request):
    siguiente = request.GET.get("next", "/")
    if not url_has_allowed_host_and_scheme(siguiente, allowed_hosts={request.get_host()}):
        siguiente = "/"

    if request.user.is_authenticated and request.user.is_staff:
        return redirect(siguiente)

    datos = _datos_sesion_spring(request)
    if not datos or not datos.get("autenticado") or datos.get("rol") != "ADMIN":
        return redirect(f"{settings.LOGIN_URL}?next={quote(siguiente)}")

    email = datos["email"]
    User = get_user_model()
    usuario, creado = User.objects.get_or_create(username=email, defaults={"email": email})
    usuario.email = email
    usuario.is_staff = True
    usuario.is_superuser = True
    if creado:
        usuario.set_unusable_password()
    usuario.save()

    login(request, usuario, backend="django.contrib.auth.backends.ModelBackend")
    return redirect(siguiente)


@require_POST
def salir(request):
    """Cierra la sesion de Django y vuelve a la pagina principal, que cierra la de Spring."""
    logout(request)
    return redirect(f"{settings.SPRING_WEB_URL}/?salir=1")
