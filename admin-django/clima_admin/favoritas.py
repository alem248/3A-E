from django.contrib.admin.views.decorators import staff_member_required
from django.shortcuts import render

from . import spring_admin
from .spring_admin import SpringAdminError


@staff_member_required
def favoritas_ranking(request):
    cookie = request.headers.get("Cookie", "")
    ranking, error = [], None
    try:
        ranking = spring_admin.ranking_favoritas(cookie)
    except SpringAdminError as fallo:
        error = str(fallo)
    return render(request, "clima_admin/favoritas.html", {
        "title": "Lugares favoritos", "ranking": ranking, "error": error,
    })
