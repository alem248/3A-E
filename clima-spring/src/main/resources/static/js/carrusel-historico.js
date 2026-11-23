/*
 * Presentación del histórico:
 *  1. Agrupa los resultados (estado, resumen, comparación, calendario y gráfico)
 *     en un bloque propio a la derecha del formulario, para evitar huecos.
 *  2. Muestra el calendario como carrusel: un mes a la vez, con flechas a los
 *     lados y deslizamiento con el dedo en celular.
 *  3. Agrega un botón Limpiar que devuelve el formulario a sus valores iniciales
 *     y borra los resultados, sin recargar la página.
 * Solo presentación: no cambia los datos ni el cálculo de los días. Los elementos
 * se mueven de lugar pero conservan sus IDs, así que historico.js sigue funcionando.
 */
(function () {
    'use strict';

    const IDS_RESULTADOS = [
        'estado-historico',
        'historico-resumen',
        'historico-comparacion',
        'historico-calendario',
        'historico-grafico'
    ];

    const IDS_OCULTABLES = [
        'historico-resumen',
        'historico-comparacion',
        'historico-calendario',
        'historico-grafico'
    ];

    const UMBRAL_DESLIZAR = 40; // píxeles mínimos para contar como deslizamiento

    function agruparResultados() {
        const formulario = document.getElementById('formulario-historico');
        if (!formulario || document.querySelector('.historico-resultados')) return;

        const grupo = document.createElement('div');
        grupo.className = 'historico-resultados';
        IDS_RESULTADOS.forEach(id => {
            const elemento = document.getElementById(id);
            if (elemento) grupo.appendChild(elemento);
        });
        formulario.insertAdjacentElement('afterend', grupo);
    }

    function limpiarTodo() {
        const formulario = document.getElementById('formulario-historico');
        if (formulario) formulario.reset(); // vuelve a los valores del HTML

        IDS_OCULTABLES.forEach(id => {
            const elemento = document.getElementById(id);
            if (elemento) {
                elemento.innerHTML = '';
                elemento.hidden = true;
            }
        });

        const estado = document.getElementById('estado-historico');
        if (estado) estado.textContent = '';

        const desde = document.getElementById('historico-desde');
        if (desde) desde.focus();
    }

    function agregarBotonLimpiar() {
        const formulario = document.getElementById('formulario-historico');
        const consultar = document.getElementById('boton-historico');
        if (!formulario || !consultar || formulario.querySelector('.boton-limpiar')) return;

        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'boton-limpiar';
        boton.textContent = 'Limpiar';
        boton.addEventListener('click', limpiarTodo);
        consultar.insertAdjacentElement('afterend', boton);
    }

    function crearCarrusel(meses) {
        const carrusel = document.createElement('div');
        carrusel.className = 'carrusel';
        carrusel.tabIndex = 0;
        carrusel.setAttribute('aria-label', 'Calendario por meses');
        carrusel.innerHTML =
            '<p class="carrusel-titulo" aria-live="polite"></p>' +
            '<div class="carrusel-fila">' +
                '<button type="button" class="carrusel-boton" data-paso="-1" aria-label="Mes anterior">&#8249;</button>' +
                '<div class="carrusel-pista"></div>' +
                '<button type="button" class="carrusel-boton" data-paso="1" aria-label="Mes siguiente">&#8250;</button>' +
            '</div>';

        // Inserta el carrusel donde estaba el primer mes, antes de moverlos.
        meses[0].parentNode.insertBefore(carrusel, meses[0]);

        const pista = carrusel.querySelector('.carrusel-pista');
        const titulo = carrusel.querySelector('.carrusel-titulo');
        const botones = carrusel.querySelectorAll('.carrusel-boton');
        meses.forEach(mes => pista.appendChild(mes));

        // El navegador sigue haciendo scroll vertical; el deslizamiento horizontal lo recibe el script.
        pista.style.touchAction = 'pan-y';

        let actual = 0;

        function mostrar(indice) {
            actual = (indice + meses.length) % meses.length;
            meses.forEach((mes, k) => mes.classList.toggle('activo', k === actual));
            const encabezado = meses[actual].querySelector('h3');
            const nombre = encabezado ? encabezado.textContent : '';
            titulo.textContent = nombre + '  ·  ' + (actual + 1) + ' de ' + meses.length;
        }

        botones.forEach(boton => {
            boton.addEventListener('click', () => mostrar(actual + Number(boton.dataset.paso)));
        });

        carrusel.addEventListener('keydown', evento => {
            if (evento.key === 'ArrowLeft') mostrar(actual - 1);
            if (evento.key === 'ArrowRight') mostrar(actual + 1);
        });

        // Deslizar con el dedo: izquierda = mes siguiente, derecha = mes anterior.
        let inicioX = null;
        let inicioY = null;

        pista.addEventListener('touchstart', evento => {
            const toque = evento.changedTouches[0];
            inicioX = toque.clientX;
            inicioY = toque.clientY;
        }, { passive: true });

        pista.addEventListener('touchend', evento => {
            if (inicioX === null) return;
            const toque = evento.changedTouches[0];
            const dx = toque.clientX - inicioX;
            const dy = toque.clientY - inicioY;
            inicioX = null;
            // Solo cuenta si el movimiento es más horizontal que vertical.
            if (Math.abs(dx) > UMBRAL_DESLIZAR && Math.abs(dx) > Math.abs(dy)) {
                mostrar(dx < 0 ? actual + 1 : actual - 1);
            }
        }, { passive: true });

        if (meses.length < 2) {
            carrusel.classList.add('sin-navegacion');
        }

        mostrar(0);
    }

    function montarCarrusel(contenedor) {
        // Si el carrusel ya existe, no se vuelve a envolver.
        if (contenedor.querySelector('.carrusel')) return;
        const meses = Array.from(contenedor.querySelectorAll('.calendario-mes'));
        if (meses.length === 0) return;
        crearCarrusel(meses);
    }

    agruparResultados();
    agregarBotonLimpiar();

    const contenedor = document.getElementById('historico-calendario');
    if (!contenedor) return;

    // Cuando historico-calendario.js redibuja, el carrusel se vuelve a montar.
    new MutationObserver(() => montarCarrusel(contenedor))
        .observe(contenedor, { childList: true, subtree: true });
    montarCarrusel(contenedor);
})();