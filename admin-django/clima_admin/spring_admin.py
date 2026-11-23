"""
Llamadas de administracion a Spring. Reenvian la cookie de la sesion del
administrador, asi Spring sabe que quien pide los datos es ADMIN.
"""
import httpx
from django.conf import settings


class SpringAdminError(Exception):
    pass


def _pedir(metodo, ruta, cookie, **kwargs):
    try:
        with httpx.Client(base_url=settings.SPRING_WEB_URL, timeout=20,
                          headers={"Cookie": cookie}) as cliente:
            respuesta = cliente.request(metodo, ruta, **kwargs)
    except httpx.HTTPError as error:
        raise SpringAdminError("No se pudo conectar con Spring.") from error

    if respuesta.status_code >= 400:
        try:
            mensaje = respuesta.json().get("error", "Spring rechazo la operacion.")
        except ValueError:
            mensaje = "Spring rechazo la operacion."
        raise SpringAdminError(mensaje)
    if respuesta.status_code == 204 or not respuesta.content:
        return None
    return respuesta.json()


def subir_imagen_noticia(cookie, noticia_id, nombre, contenido, tipo):
    return _pedir("POST", f"/api/admin/noticias/{noticia_id}/imagen", cookie,
                  files={"imagen": (nombre, contenido, tipo)})


def obtener_imagen_noticia(cookie, noticia_id):
    try:
        with httpx.Client(base_url=settings.SPRING_WEB_URL, timeout=20,
                          headers={"Cookie": cookie}) as cliente:
            respuesta = cliente.get(f"/api/admin/noticias/{noticia_id}/imagen")
    except httpx.HTTPError as error:
        raise SpringAdminError("No se pudo conectar con Spring.") from error
    if respuesta.status_code >= 400:
        raise SpringAdminError("Imagen no encontrada.")
    return respuesta.content, respuesta.headers.get("content-type", "image/jpeg")


def monitorear_usuario(cookie, usuario_id):
    return _pedir("POST", f"/api/admin/usuarios/{usuario_id}/monitorear", cookie)


def listar_usuarios(cookie):
    return _pedir("GET", "/api/admin/usuarios", cookie)


def detalle_usuario(cookie, usuario_id):
    return _pedir("GET", f"/api/admin/usuarios/{usuario_id}", cookie)


def editar_usuario(cookie, usuario_id, datos):
    return _pedir("PUT", f"/api/admin/usuarios/{usuario_id}", cookie, json=datos)


def cambiar_preferencia(cookie, usuario_id, nivel_id, activa):
    return _pedir("PUT", f"/api/admin/usuarios/{usuario_id}/preferencias/{nivel_id}",
                  cookie, json={"activa": activa})


def crear_usuario(cookie, datos):
    return _pedir("POST", "/api/admin/usuarios", cookie, json=datos)


def eliminar_usuario(cookie, usuario_id):
    return _pedir("DELETE", f"/api/admin/usuarios/{usuario_id}", cookie)


def listar_noticias(cookie, estado=""):
    parametros = {"estado": estado} if estado else None
    return _pedir("GET", "/api/admin/noticias", cookie, params=parametros)


def crear_noticia(cookie, datos):
    return _pedir("POST", "/api/admin/noticias", cookie, json=datos)


def editar_noticia(cookie, noticia_id, datos):
    return _pedir("PUT", f"/api/admin/noticias/{noticia_id}", cookie, json=datos)


def eliminar_noticia(cookie, noticia_id):
    return _pedir("DELETE", f"/api/admin/noticias/{noticia_id}", cookie)


def cambiar_estado_noticia(cookie, noticia_id, accion):
    return _pedir("POST", f"/api/admin/noticias/{noticia_id}/{accion}", cookie)


def ranking_favoritas(cookie):
    return _pedir("GET", "/api/admin/favoritas/ranking", cookie)


def listar_ubicaciones(cookie):
    return _pedir("GET", "/api/admin/usuarios/ubicaciones", cookie)
