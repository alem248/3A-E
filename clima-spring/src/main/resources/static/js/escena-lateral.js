/*
 * Efecto 2.5D de la escena lateral: cada capa se desplaza según su profundidad
 * (data-prof). Sigue el cursor dentro de la escena; sin cursor hace un vaivén suave.
 * Solo presentación.
 */
(() => {
    'use strict';

    const escena = document.querySelector('.escena-lateral');
    if (!escena) return;

    const capas = Array.from(escena.querySelectorAll('[data-prof]'));
    const AMPLITUD = 9;
    const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const objetivo = { x: 0, y: 0 };
    const actual = { x: 0, y: 0 };
    let dentro = false;
    const inicio = performance.now();

    escena.addEventListener('pointermove', (evento) => {
        const rect = escena.getBoundingClientRect();
        objetivo.x = ((evento.clientX - rect.left) / rect.width - 0.5) * 2;
        objetivo.y = ((evento.clientY - rect.top) / rect.height - 0.5) * 2;
        dentro = true;
    });

    escena.addEventListener('pointerleave', () => {
        dentro = false;
    });

    function cuadro(ahora) {
        const t = (ahora - inicio) / 1000;
        const destinoX = dentro ? objetivo.x : Math.sin(t * 0.4) * 0.6;
        const destinoY = dentro ? objetivo.y : Math.sin(t * 0.3) * 0.25;

        actual.x += (destinoX - actual.x) * 0.06;
        actual.y += (destinoY - actual.y) * 0.06;

        capas.forEach((capa) => {
            const profundidad = Number(capa.dataset.prof);
            const x = -actual.x * AMPLITUD * profundidad;
            const y = -actual.y * AMPLITUD * 0.4 * profundidad;
            capa.setAttribute('transform', `translate(${x.toFixed(2)} ${y.toFixed(2)})`);
        });

        requestAnimationFrame(cuadro);
    }

    if (!sinMovimiento) requestAnimationFrame(cuadro);
})();
