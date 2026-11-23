/*
 * Panel de administracion: el boton Salir pide confirmacion antes de cerrar
 * la sesion. El boton atras del navegador funciona normal.
 */
(function () {
    'use strict';

    const formulario = document.getElementById('form-salir');
    if (!formulario) return;

    formulario.addEventListener('submit', (evento) => {
        if (!window.confirm('¿Quieres salir del panel de administración?')) {
            evento.preventDefault();
        }
    });
})();
