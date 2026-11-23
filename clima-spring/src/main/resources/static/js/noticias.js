/*
 * Panel "Noticias del Clima" dentro del histórico.
 *  - Ocupa el espacio libre a la derecha del formulario.
 *  - Se oculta al pulsar Consultar (aparecen los resultados).
 *  - Vuelve a mostrarse al pulsar Limpiar.
 *
 * Las noticias se editan en la lista NOTICIAS de abajo. Por ahora son contenido de
 * ejemplo; cuando exista el panel de administración, reemplazar esta lista por una
 * carga desde el backend (solo presentación, no cambia la lógica del histórico).
 */
(function () {
    'use strict';

    const NOTICIAS = [
        {
            categoria: 'Clima costero',
            tipo: 'clima',
            titulo: 'Temperaturas en la costa peruana se mantendrán altas esta semana',
            resumen: 'Se esperan temperaturas superiores a lo habitual en la franja costera, con posibles impactos en la salud y en el mar.',
            fecha: '16 nov. 2026',
            imagen: '/img/zonas/tumbes.jpg',
            alt: 'Playa de la costa norte'
        },
        {
            categoria: 'Alerta climática',
            tipo: 'alerta',
            titulo: 'Se activa alerta de riesgo alto en varias zonas costeras',
            resumen: 'Se recomienda tomar precauciones ante el incremento de la temperatura del mar y posibles olas de calor.',
            fecha: '15 nov. 2026',
            imagen: '/img/zonas/paita.jpg',
            alt: 'Muelle de Paita'
        },
        {
            categoria: 'Pronóstico',
            tipo: 'pronostico',
            titulo: 'Pronóstico: cielo despejado en la costa central y norte',
            resumen: 'Se espera un clima estable con temperaturas entre 24 °C y 30 °C durante los próximos días.',
            fecha: '14 nov. 2026',
            imagen: '/img/zonas/chimbote.jpg',
            alt: 'Costa de Chimbote'
        }
    ];

    const ICONO_MEGAFONO = '<svg class="icono" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 10v4h4l7 4V6L7 10z"/><path d="M18 8a5 5 0 0 1 0 8"/></svg>';

    function crearElemento(etiqueta, clase, texto) {
        const el = document.createElement(etiqueta);
        if (clase) el.className = clase;
        if (texto !== undefined) el.textContent = texto;
        return el;
    }

    function crearNoticia(noticia) {
        const articulo = crearElemento('article', 'noticia noticia-' + noticia.tipo);

        const imagen = document.createElement('img');
        imagen.className = 'noticia-imagen';
        imagen.src = noticia.imagen;
        imagen.alt = noticia.alt || '';
        imagen.loading = 'lazy';
        imagen.onerror = () => imagen.remove();
        articulo.appendChild(imagen);

        const cuerpo = crearElemento('div', 'noticia-cuerpo');

        const linea = crearElemento('div', 'noticia-linea');
        linea.appendChild(crearElemento('span', 'noticia-etiqueta', noticia.categoria));
        linea.appendChild(crearElemento('time', 'noticia-fecha', noticia.fecha));
        cuerpo.appendChild(linea);

        cuerpo.appendChild(crearElemento('h3', 'noticia-titulo', noticia.titulo));

        const resumen = crearElemento('p', 'noticia-resumen', noticia.resumen);
        cuerpo.appendChild(resumen);

        const boton = crearElemento('button', 'noticia-leer', 'Leer más');
        boton.type = 'button';
        boton.setAttribute('aria-expanded', 'false');
        boton.addEventListener('click', () => {
            const expandida = articulo.classList.toggle('expandida');
            boton.setAttribute('aria-expanded', String(expandida));
            boton.textContent = expandida ? 'Leer menos' : 'Leer más';
        });
        cuerpo.appendChild(boton);

        articulo.appendChild(cuerpo);
        return articulo;
    }

    function crearPanel() {
        const panel = crearElemento('section', 'noticias');
        panel.setAttribute('aria-labelledby', 'titulo-noticias');

        const encabezado = crearElemento('div', 'noticias-encabezado');
        const titulo = document.createElement('h2');
        titulo.id = 'titulo-noticias';
        titulo.className = 'noticias-titulo';
        titulo.innerHTML = ICONO_MEGAFONO + ' Noticias del Clima';
        encabezado.appendChild(titulo);
        panel.appendChild(encabezado);

        const lista = crearElemento('div', 'noticias-lista');
        NOTICIAS.forEach(noticia => lista.appendChild(crearNoticia(noticia)));
        panel.appendChild(lista);

        return panel;
    }

    function ubicarPanel(panel) {
        const formulario = document.getElementById('formulario-historico');
        const grupo = document.querySelector('.historico-resultados');
        if (grupo) {
            grupo.insertBefore(panel, grupo.firstChild);
        } else if (formulario) {
            formulario.insertAdjacentElement('afterend', panel);
        }
    }

    function iniciar() {
        const formulario = document.getElementById('formulario-historico');
        if (!formulario || document.querySelector('.noticias')) return;

        const panel = crearPanel();
        ubicarPanel(panel);

        // Al consultar, las noticias se ocultan y dan paso a los resultados.
        formulario.addEventListener('submit', () => { panel.hidden = true; });

        // Al limpiar (botón creado en carrusel-historico.js), vuelven las noticias.
        document.addEventListener('click', evento => {
            if (evento.target.closest && evento.target.closest('.boton-limpiar')) {
                panel.hidden = false;
            }
        });
    }

    iniciar();
})();