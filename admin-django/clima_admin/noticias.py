from django.contrib import messages
from django.http import Http404, HttpResponse
from django.contrib.admin.views.decorators import staff_member_required
from django.shortcuts import redirect, render
from django.views.decorators.http import require_http_methods, require_POST

from . import spring_admin
from .spring_admin import SpringAdminError

DEPARTAMENTOS = [
    "Amazonas", "Áncash", "Apurímac", "Arequipa", "Ayacucho", "Cajamarca", "Callao",
    "Cusco", "Huancavelica", "Huánuco", "Ica", "Junín", "La Libertad", "Lambayeque",
    "Lima", "Loreto", "Madre de Dios", "Moquegua", "Pasco", "Piura", "Puno",
    "San Martín", "Tacna", "Tumbes", "Ucayali",
]

FILTROS = [
    ("", "Todas"),
    ("PENDIENTE", "Pendientes de revisión"),
    ("APROBADA", "Aprobadas"),
    ("DESCARTADA", "Descartadas"),
]


TIPOS_IMAGEN = {"image/jpeg", "image/png", "image/webp"}
MAX_IMAGEN = 2 * 1024 * 1024


def _imagen_subida(request):
    """Devuelve el archivo subido si es valido; si no, lanza SpringAdminError."""
    archivo = request.FILES.get("imagen")
    if archivo is None:
        return None
    if archivo.content_type not in TIPOS_IMAGEN or archivo.size > MAX_IMAGEN:
        raise SpringAdminError("La imagen debe ser JPG, PNG o WEBP de máximo 2 MB.")
    return archivo


def _guardar_imagen(cookie, noticia_id, archivo):
    if archivo is not None:
        spring_admin.subir_imagen_noticia(cookie, noticia_id, archivo.name,
                                          archivo.read(), archivo.content_type)


def _datos(request):
    return {
        "titulo": request.POST.get("titulo", ""),
        "resumen": request.POST.get("resumen", ""),
        "categoria": request.POST.get("categoria", ""),
        "departamento": request.POST.get("departamento", ""),
    }


@staff_member_required
def noticias_lista(request):
    cookie = request.headers.get("Cookie", "")
    estado = request.GET.get("estado", "").upper()
    if estado not in {valor for valor, _ in FILTROS}:
        estado = ""
    noticias, error = [], None
    try:
        noticias = spring_admin.listar_noticias(cookie, estado)
    except SpringAdminError as fallo:
        error = str(fallo)
    return render(request, "clima_admin/noticias.html", {
        "title": "Noticias", "noticias": noticias, "error": error,
        "filtros": FILTROS, "filtro_activo": estado,
    })


@staff_member_required
@require_http_methods(["GET", "POST"])
def noticia_nueva(request):
    cookie = request.headers.get("Cookie", "")
    valores = request.POST if request.method == "POST" else {}
    if request.method == "POST":
        try:
            archivo = _imagen_subida(request)
            creada = spring_admin.crear_noticia(cookie, _datos(request))
            _guardar_imagen(cookie, creada["id"], archivo)
            messages.success(request, "Noticia publicada.")
            return redirect("clima_admin:noticias")
        except SpringAdminError as fallo:
            messages.error(request, str(fallo))
    return render(request, "clima_admin/noticia_form.html", {
        "title": "Nueva noticia", "valores": valores, "departamentos": DEPARTAMENTOS,
    })


@staff_member_required
@require_http_methods(["GET", "POST"])
def noticia_editar(request, noticia_id):
    cookie = request.headers.get("Cookie", "")
    try:
        if request.method == "POST":
            archivo = _imagen_subida(request)
            spring_admin.editar_noticia(cookie, noticia_id, _datos(request))
            _guardar_imagen(cookie, noticia_id, archivo)
            messages.success(request, "Noticia actualizada.")
            return redirect("clima_admin:noticias")
        noticia = next((n for n in spring_admin.listar_noticias(cookie)
                        if n["id"] == noticia_id), None)
    except SpringAdminError as fallo:
        messages.error(request, str(fallo))
        return redirect("clima_admin:noticias")
    if noticia is None:
        messages.error(request, "Noticia no encontrada.")
        return redirect("clima_admin:noticias")
    return render(request, "clima_admin/noticia_form.html", {
        "title": "Editar noticia", "noticia": noticia, "valores": noticia,
        "departamentos": DEPARTAMENTOS,
    })


@staff_member_required
def noticia_imagen(request, noticia_id):
    cookie = request.headers.get("Cookie", "")
    try:
        contenido, tipo = spring_admin.obtener_imagen_noticia(cookie, noticia_id)
    except SpringAdminError:
        raise Http404
    return HttpResponse(contenido, content_type=tipo)


@staff_member_required
@require_POST
def noticia_eliminar(request, noticia_id):
    cookie = request.headers.get("Cookie", "")
    try:
        spring_admin.eliminar_noticia(cookie, noticia_id)
        messages.success(request, "Noticia eliminada.")
    except SpringAdminError as fallo:
        messages.error(request, str(fallo))
    return redirect("clima_admin:noticias")


@staff_member_required
@require_POST
def noticia_cambiar_estado(request, noticia_id, accion):
    cookie = request.headers.get("Cookie", "")
    try:
        spring_admin.cambiar_estado_noticia(cookie, noticia_id, accion)
        messages.success(request, "Noticia aprobada." if accion == "aprobar" else "Noticia descartada.")
    except SpringAdminError as fallo:
        messages.error(request, str(fallo))
    return redirect("clima_admin:noticias")
