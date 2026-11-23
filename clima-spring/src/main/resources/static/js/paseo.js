/*
 * Ajusta la altura de la capa del paseo para que cubra la franja de cabecera
 * y las dos tarjetas de clima. Solo presentación.
 */
(function () {
    'use strict';

    const fila = document.querySelector('.paseo-zona');
    const capa = fila && fila.querySelector(':scope > .paseo');
    if (!fila || !capa) return;

    function ajustar() {
        const base = fila.getBoundingClientRect().top;
        const tarjetas = fila.querySelectorAll(':scope > .col-xl-6');
        let fondo = 0;
        tarjetas.forEach((t) => {
            fondo = Math.max(fondo, t.getBoundingClientRect().bottom - base);
        });
        if (fondo > 0) capa.style.height = Math.ceil(fondo) + 'px';
    }

    window.addEventListener('resize', ajustar);
    window.addEventListener('load', ajustar);
    ajustar();
})();
