from django.conf import settings


def spring_web_url(request):
    """Expone la URL de la pagina principal (Spring) a las plantillas."""
    return {"spring_web_url": settings.SPRING_WEB_URL}


def nav_sidebar(request):
    """Activa la barra lateral del admin de Django en las vistas propias del panel."""
    return {"is_nav_sidebar_enabled": True}
