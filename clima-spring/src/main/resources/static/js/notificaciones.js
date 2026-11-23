/*
 * Campanita de avisos personalizados. Solo aparece con sesion iniciada (el
 * bloque .sesion-usuario se oculta sin sesion). Consulta /api/notificaciones.
 */
(function () {
    'use strict';

    const boton = document.getElementById('boton-notificaciones');
    const contador = document.getElementById('contador-notificaciones');
    const panel = document.getElementById('panel-notificaciones');
    const lista = document.getElementById('lista-notificaciones');
    if (!boton || !panel || !lista) return;

    let temporizador = null;

    function formatearFecha(fecha) {
        const d = new Date(fecha);
        return isNaN(d) ? '' : d.toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' });
    }

    function pintar(datos) {
        const noLeidas = datos.noLeidas || 0;
        contador.textContent = noLeidas;
        contador.hidden = noLeidas === 0;
        lista.replaceChildren();

        if (!datos.items || datos.items.length === 0) {
            const vacia = document.createElement('li');
            vacia.className = 'notificacion-vacia';
            vacia.textContent = 'Aún no tienes avisos.';
            lista.append(vacia);
            return;
        }

        datos.items.forEach((aviso) => {
            const item = document.createElement('li');
            item.className = aviso.leida ? 'notificacion' : 'notificacion no-leida';

            const titulo = document.createElement('strong');
            titulo.textContent = `Alerta ${aviso.nivel}${aviso.zona ? ' · ' + aviso.zona : ''}`;

            const mensaje = document.createElement('p');
            mensaje.textContent = aviso.mensaje || 'Se registró una alerta en tu zona.';

            const fecha = document.createElement('small');
            fecha.textContent = formatearFecha(aviso.fecha);

            item.append(titulo, mensaje, fecha);
            lista.append(item);
        });
    }

    async function cargar() {
        try {
            const respuesta = await fetch('/api/notificaciones', { credentials: 'same-origin' });
            if (!respuesta.ok) return;
            pintar(await respuesta.json());
        } catch (error) {
            // Sin conexion: se vuelve a intentar en el siguiente ciclo.
        }
    }

    function iniciar() {
        if (temporizador) return;
        cargar();
        temporizador = setInterval(cargar, 60000);
    }

    function detener() {
        clearInterval(temporizador);
        temporizador = null;
        panel.hidden = true;
        boton.setAttribute('aria-expanded', 'false');
        lista.replaceChildren();
        contador.hidden = true;
    }

    boton.addEventListener('click', async () => {
        const abrir = panel.hidden;
        panel.hidden = !abrir;
        boton.setAttribute('aria-expanded', String(abrir));
        if (abrir) {
            await cargar();
            // Al abrir, los avisos pasan a leidos en el servidor.
            fetch('/api/notificaciones/leidas', { method: 'POST', credentials: 'same-origin' })
                .then(() => { contador.hidden = true; });
        }
    });

    document.addEventListener('sesion-cambio', (evento) => {
        if (evento.detail && evento.detail.autenticado) {
            iniciar();
        } else {
            detener();
        }
    });
})();
