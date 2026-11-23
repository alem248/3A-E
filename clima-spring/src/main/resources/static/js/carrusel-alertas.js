/*
 * Muestra las alertas térmicas como carrusel horizontal con flechas.
 * Solo presentación: mueve #alertas-lista dentro de un contenedor; el script
 * que llena la lista sigue usando el mismo ID.
 */
(function () {
    'use strict';

    const lista = document.getElementById('alertas-lista');
    if (!lista || lista.parentElement.classList.contains('alertas-carrusel')) return;

    const contenedor = document.createElement('div');
    contenedor.className = 'alertas-carrusel';

    function crearBoton(texto, direccion, etiqueta) {
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'alertas-carrusel-boton';
        boton.textContent = texto;
        boton.setAttribute('aria-label', etiqueta);
        boton.addEventListener('click', () => desplazar(direccion));
        return boton;
    }

    const anterior = crearBoton('‹', -1, 'Alertas anteriores');
    const siguiente = crearBoton('›', 1, 'Alertas siguientes');

    lista.parentNode.insertBefore(contenedor, lista);
    contenedor.append(anterior, lista, siguiente);
    lista.tabIndex = 0;

    function desplazar(direccion) {
        const tarjeta = lista.querySelector('.alerta-termica');
        const paso = tarjeta ? tarjeta.getBoundingClientRect().width + 14 : 220;
        lista.scrollBy({ left: direccion * paso, behavior: 'smooth' });
    }

    function actualizarBotones() {
        const maximo = lista.scrollWidth - lista.clientWidth;
        anterior.disabled = lista.scrollLeft <= 2;
        siguiente.disabled = maximo <= 2 || lista.scrollLeft >= maximo - 2;
        contenedor.classList.toggle('sin-desplazamiento', maximo <= 2);
    }

    lista.addEventListener('scroll', actualizarBotones, { passive: true });
    new MutationObserver(actualizarBotones).observe(lista, { childList: true });
    window.addEventListener('resize', actualizarBotones);
    actualizarBotones();
})();
