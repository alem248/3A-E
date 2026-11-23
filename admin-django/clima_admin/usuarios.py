from django.contrib import messages
from django.contrib.admin.views.decorators import staff_member_required
from django.shortcuts import redirect, render
from django.views.decorators.http import require_http_methods, require_POST

from . import spring_admin
from .spring_admin import SpringAdminError

ROLES = ["POBLADOR", "DEFENSA_CIVIL", "ADMIN"]


@staff_member_required
@require_POST
def usuario_monitorear(request, usuario_id):
    cookie = request.headers.get("Cookie", "")
    try:
        datos = spring_admin.monitorear_usuario(cookie, usuario_id)
        messages.success(request, f"Zona monitoreada creada: {datos['zonaNombre']}.")
    except SpringAdminError as fallo:
        messages.error(request, str(fallo))
    return redirect("clima_admin:usuarios")


@staff_member_required
def usuarios_lista(request):
    cookie = request.headers.get("Cookie", "")
    error = None
    usuarios = []
    try:
        usuarios = spring_admin.listar_usuarios(cookie)
    except SpringAdminError as fallo:
        error = str(fallo)
    return render(request, "clima_admin/usuarios.html", {
        "title": "Usuarios registrados", "usuarios": usuarios, "error": error,
    })


@staff_member_required
@require_http_methods(["GET", "POST"])
def usuario_detalle(request, usuario_id):
    cookie = request.headers.get("Cookie", "")
    try:
        if request.method == "POST":
            spring_admin.editar_usuario(cookie, usuario_id, {
                "nombres": request.POST.get("nombres", ""),
                "email": request.POST.get("email", ""),
                "rol": request.POST.get("rol", ""),
            })
            usuario = spring_admin.detalle_usuario(cookie, usuario_id)
            for preferencia in usuario.get("preferencias", []):
                activa = f"nivel_{preferencia['nivelRiesgoId']}" in request.POST
                if activa != preferencia["activa"]:
                    spring_admin.cambiar_preferencia(cookie, usuario_id,
                                                     preferencia["nivelRiesgoId"], activa)
            messages.success(request, "Cambios guardados.")
            return redirect("clima_admin:usuario_detalle", usuario_id=usuario_id)

        usuario = spring_admin.detalle_usuario(cookie, usuario_id)
    except SpringAdminError as fallo:
        messages.error(request, str(fallo))
        if request.method == "POST":
            return redirect("clima_admin:usuario_detalle", usuario_id=usuario_id)
        return redirect("clima_admin:usuarios")

    return render(request, "clima_admin/usuario_detalle.html", {
        "title": f"Usuario: {usuario.get('nombres') or usuario.get('email')}",
        "usuario": usuario, "roles": ROLES,
    })


@staff_member_required
@require_http_methods(["GET", "POST"])
def usuario_nuevo(request):
    cookie = request.headers.get("Cookie", "")
    if request.method == "POST":
        try:
            spring_admin.crear_usuario(cookie, {
                "nombres": request.POST.get("nombres", ""),
                "email": request.POST.get("email", ""),
                "rol": request.POST.get("rol", ""),
                "password": request.POST.get("password", ""),
            })
            messages.success(request, "Usuario creado.")
            return redirect("clima_admin:usuarios")
        except SpringAdminError as fallo:
            messages.error(request, str(fallo))
    return render(request, "clima_admin/usuario_nuevo.html", {
        "title": "Nuevo usuario", "roles": ROLES, "valores": request.POST,
    })


@staff_member_required
@require_POST
def usuario_eliminar(request, usuario_id):
    cookie = request.headers.get("Cookie", "")
    try:
        spring_admin.eliminar_usuario(cookie, usuario_id)
        messages.success(request, "Usuario eliminado.")
    except SpringAdminError as fallo:
        messages.error(request, str(fallo))
    return redirect("clima_admin:usuarios")
