/**
 * Noticias para ti: muestra las noticias aprobadas del departamento del usuario.
 * Pide /api/noticias/para-ti; sin sesion la seccion queda oculta.
 */
(function () {
    'use strict';

    const seccion = document.getElementById('noticias-para-ti');
    const zona = document.getElementById('noticias-para-ti-zona');
    const lista = document.getElementById('noticias-para-ti-lista');
    if (!seccion || !zona || !lista) return;

    // Iconos del banner de cada noticia segun su categoria.
    const ICONOS = {
        alerta: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 2 20h20L12 3z"/><path d="M12 10v4M12 17h.01"/></svg>',
        pronostico: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="8" cy="8" r="3.5"/><path d="M8 1.5v1.5M2.5 8H1M3.9 3.9l1 1M12.1 3.9l-1 1M7 19h10a4 4 0 0 0 0-8 5.5 5.5 0 0 0-10.5 1.5A3 3 0 0 0 7 19z"/></svg>',
        actualidad: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 9h10M7 13h6"/></svg>',
        general: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 9h10M7 13h6"/></svg>'
    };

    // Fotos de cada categoria (archivos en static/img/noticias). Si falta una, se muestra el icono.
    const FOTOS = {
        alerta: '/img/noticias/alerta-oleaje.jpg',
        pronostico: '/img/noticias/costa-despejada.png',
        actualidad: '/img/noticias/lima-malecon.png',
        general: '/img/noticias/lima-malecon.png'
    };

    function crearElemento(etiqueta, clase, texto) {
        const el = document.createElement(etiqueta);
        if (clase) el.className = clase;
        if (texto !== undefined) el.textContent = texto;
        return el;
    }

    function formatearFecha(valor) {
        if (!valor) return '';
        const fecha = new Date(valor);
        return isNaN(fecha) ? '' : fecha.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' });
    }

    function categoriaClave(categoria) {
        const base = (categoria || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
        if (base.includes('alerta')) return 'alerta';
        if (base.includes('pronostico')) return 'pronostico';
        if (base.includes('actualidad')) return 'actualidad';
        return 'general';
    }

    // Texto completo de la noticia: un parrafo por cada salto de linea doble.
    function crearDetalle(contenido) {
        const detalle = crearElemento('div', 'noticia-detalle');
        (contenido || '').split(/\n\s*\n/).filter(Boolean).forEach(parrafo => {
            detalle.appendChild(crearElemento('p', '', parrafo.trim()));
        });
        return detalle;
    }

    function crearNoticia(noticia) {
        const clave = categoriaClave(noticia.categoria);
        const articulo = crearElemento('article', 'noticia-para-ti noticia-' + clave);

        const banner = crearElemento('div', 'noticia-banner');
        const iconoBanner = () => { banner.innerHTML = ICONOS[clave]; };
        if (FOTOS[clave]) {
            const foto = document.createElement('img');
            foto.src = FOTOS[clave];
            foto.alt = noticia.titulo || '';
            foto.loading = 'lazy';
            foto.addEventListener('error', () => { foto.remove(); iconoBanner(); });
            banner.appendChild(foto);
        } else {
            iconoBanner();
        }

        const cuerpo = crearElemento('div', 'noticia-cuerpo');
        const linea = crearElemento('div', 'noticia-linea');
        linea.appendChild(crearElemento('span', 'noticia-etiqueta', noticia.categoria || 'General'));
        linea.appendChild(crearElemento('span', 'noticia-alcance',
            noticia.departamento ? noticia.departamento : 'Todo el país'));
        linea.appendChild(crearElemento('time', 'noticia-fecha', formatearFecha(noticia.fechaCreacion)));

        const pie = crearElemento('div', 'noticia-pie');
        const boton = crearElemento('button', 'noticia-leer-mas', 'Leer más →');
        boton.type = 'button';
        boton.addEventListener('click', () => {
            const abierta = articulo.classList.toggle('expandida');
            boton.textContent = abierta ? 'Leer menos ←' : 'Leer más →';
        });
        pie.appendChild(boton);

        cuerpo.append(
            linea,
            crearElemento('h3', 'noticia-titulo', noticia.titulo),
            crearElemento('p', 'noticia-resumen', noticia.resumen),
            crearDetalle(noticia.contenido),
            pie
        );
        articulo.append(banner, cuerpo);
        return articulo;
    }

    function pintar(datos) {
        zona.textContent = datos.departamento
            ? 'Noticias de ' + datos.departamento + ' y de alcance nacional.'
            : 'Activa tu ubicación para ver noticias de tu departamento.';
        lista.replaceChildren();
        if (!datos.noticias.length) {
            lista.appendChild(crearElemento('p', 'noticias-para-ti-vacio', 'Por ahora no hay noticias para tu zona.'));
            return;
        }
        datos.noticias.forEach(noticia => lista.appendChild(crearNoticia(noticia)));
    }

    async function cargar() {
        try {
            const respuesta = await fetch('/api/noticias/para-ti', { credentials: 'same-origin' });
            if (!respuesta.ok) {
                seccion.hidden = true;
                return;
            }
            pintar(await respuesta.json());
            seccion.hidden = false;
        } catch (error) {
            seccion.hidden = true;
        }
    }

    document.addEventListener('sesion-cambio', cargar);
    document.addEventListener('ubicacion-cambio', cargar);
    cargar();
})();
