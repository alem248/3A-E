/*
 * Panel de usuario: saludo con su nombre, resumen de alertas nuevas,
 * zona de su ultima ubicacion y ubicaciones favoritas (se agregan desde el mapa).
 * Solo visible con sesion iniciada.
 */
(() => {
    'use strict';

    const saludo = document.querySelector('.saludo');
    const ancla = document.querySelector('.tarjeta-mapa');
    if (!saludo || !ancla) return;

    const SALUDO_ANONIMO = saludo.innerHTML;
    const resumen = document.createElement('p');
    resumen.className = 'resumen-saludo';
    resumen.hidden = true;
    saludo.insertAdjacentElement('afterend', resumen);

    const panel = document.createElement('section');
    panel.id = 'panel-usuario';
    panel.className = 'panel-usuario';
    panel.hidden = true;
    ancla.parentNode.insertBefore(panel, ancla);

    const bloqueZona = elemento('div', 'panel-usuario-zona');
    const listaFavoritas = elemento('ul', 'panel-usuario-lista');
    const mensaje = elemento('p', 'panel-usuario-mensaje');
    mensaje.setAttribute('role', 'status');

    panel.append(
        elemento('h2', 'panel-usuario-titulo', 'Mi zona guardada'),
        bloqueZona,
        elemento('h3', 'panel-usuario-subtitulo', 'Ubicaciones favoritas'),
        listaFavoritas,
        mensaje
    );

    let usuarioId = null;

    function elemento(etiqueta, clase, texto) {
        const nodo = document.createElement(etiqueta);
        if (clase) nodo.className = clase;
        if (texto !== undefined) nodo.textContent = texto;
        return nodo;
    }

    async function pedir(url, opciones = {}) {
        const respuesta = await fetch(url, {
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            ...opciones
        });
        const datos = await respuesta.json().catch(() => null);
        if (!respuesta.ok) {
            throw new Error((datos && datos.error) || 'No se pudo completar la acción.');
        }
        return datos;
    }

    function pintarMensaje(texto, esError = false) {
        mensaje.textContent = texto;
        mensaje.classList.toggle('error', esError);
    }

    function coordenadas(latitud, longitud) {
        return `${Number(latitud).toFixed(4)}°, ${Number(longitud).toFixed(4)}°`;
    }

    async function actualizarResumen() {
        try {
            const avisos = await pedir('/api/notificaciones');
            const nuevas = avisos.filter((aviso) => !aviso.leida).length;
            resumen.textContent = nuevas === 0
                ? 'No tienes alertas nuevas en tu zona.'
                : `Tienes ${nuevas} alerta${nuevas === 1 ? '' : 's'} nueva${nuevas === 1 ? '' : 's'} en tu zona.`;
        } catch (error) {
            resumen.textContent = '';
        }
        resumen.hidden = !resumen.textContent;
    }

    async function cargarZona() {
        const nodos = [];
        try {
            const registrada = await pedir(`/api/usuarios/${usuarioId}/ubicacion`);
            const enZona = registrada.enZonaVulnerable && registrada.zona;
            nodos.push(elemento('strong', 'panel-usuario-zona-nombre',
                enZona ? `Estás en ${registrada.zona.nombre}` : 'Fuera de las zonas vulnerables'));
            if (enZona && registrada.zona.nivelRiesgo) {
                const nivel = elemento('span', 'panel-usuario-nivel', registrada.zona.nivelRiesgo);
                nivel.style.setProperty('--nivel-color', registrada.zona.colorHex || '#94a3b8');
                nodos.push(nivel);
            }
            nodos.push(elemento('p', 'panel-usuario-detalle',
                `Última ubicación guardada: ${coordenadas(registrada.latitud, registrada.longitud)}`));
        } catch (error) {
            nodos.push(elemento('p', 'panel-usuario-detalle',
                'Aún no has guardado tu ubicación. Usa el botón de GPS para registrarla.'));
        }
        bloqueZona.replaceChildren(...nodos);
    }

    async function temperaturaEn(latitud, longitud) {
        const url = `https://api.open-meteo.com/v1/forecast?latitude=${latitud}&longitude=${longitud}`
            + '&current=temperature_2m&timezone=America%2FLima';
        const respuesta = await fetch(url);
        if (!respuesta.ok) throw new Error('Sin datos');
        const datos = await respuesta.json();
        return datos.current.temperature_2m;
    }

    function pintarFavorita(favorita) {
        const item = elemento('li', 'panel-usuario-favorita');
        const nombre = elemento('strong', 'panel-usuario-favorita-nombre', favorita.etiqueta);
        const temperatura = elemento('span', 'panel-usuario-temperatura', 'Consultando...');
        temperaturaEn(favorita.latitud, favorita.longitud)
            .then((valor) => { temperatura.textContent = `${Number(valor).toFixed(1)} °C`; })
            .catch(() => { temperatura.textContent = 'Sin datos'; });
        const quitar = elemento('button', 'panel-usuario-quitar', 'Quitar');
        quitar.type = 'button';
        quitar.addEventListener('click', async () => {
            try {
                await pedir(`/api/usuarios/${usuarioId}/favoritas/${favorita.id}`, { method: 'DELETE' });
                await cargarFavoritas();
            } catch (error) {
                pintarMensaje(error.message, true);
            }
        });
        item.append(nombre, temperatura, quitar);
        return item;
    }

    async function cargarFavoritas() {
        try {
            const favoritas = await pedir(`/api/usuarios/${usuarioId}/favoritas`);
            const nodos = favoritas.map(pintarFavorita);
            if (favoritas.length === 0) {
                nodos.push(elemento('li', 'panel-usuario-vacio', 'Todavía no guardas ubicaciones favoritas.'));
            }
            listaFavoritas.replaceChildren(...nodos);
            if (window.mapaFavoritas) window.mapaFavoritas.mostrar(favoritas);
        } catch (error) {
            pintarMensaje(error.message, true);
        }
    }

    async function alCambiarSesion(datos) {
        if (!datos || !datos.autenticado) {
            usuarioId = null;
            saludo.innerHTML = SALUDO_ANONIMO;
            resumen.hidden = true;
            panel.hidden = true;
            if (window.mapaFavoritas) window.mapaFavoritas.mostrar([]);
            return;
        }
        usuarioId = datos.id;
        saludo.textContent = `¡Hola, ${datos.nombres || datos.email}! 👋`;
        panel.hidden = false;
        pintarMensaje('');
        await Promise.all([actualizarResumen(), cargarZona(), cargarFavoritas()]);
    }

    document.addEventListener('sesion-cambio', (evento) => {
        alCambiarSesion(evento.detail);
    });

    document.addEventListener('ubicacion-cambio', () => {
        if (usuarioId !== null) cargarZona();
    });

    // Accion que el mapa anade al popup de cada punto: guardar el punto como sitio favorito.
    function crearAccionFavorita(latitud, longitud) {
        const caja = elemento('div', 'popup-favorita');
        if (usuarioId === null) {
            caja.append(elemento('p', 'popup-favorita-aviso', 'Inicia sesión para guardar tus sitios favoritos.'));
            return caja;
        }

        const formularioPopup = elemento('form', 'popup-favorita-form');
        const nombre = elemento('input', 'popup-favorita-nombre');
        nombre.type = 'text';
        nombre.required = true;
        nombre.maxLength = 60;
        nombre.placeholder = 'Nombre del sitio';
        const boton = elemento('button', 'popup-favorita-boton', 'Agregar como mi sitio favorito');
        boton.type = 'submit';
        const estado = elemento('p', 'popup-favorita-aviso');
        formularioPopup.append(nombre, boton, estado);

        formularioPopup.addEventListener('submit', async (evento) => {
            evento.preventDefault();
            const etiqueta = nombre.value.trim();
            if (!etiqueta) return;
            boton.disabled = true;
            estado.textContent = 'Guardando...';
            try {
                await pedir(`/api/usuarios/${usuarioId}/favoritas`, {
                    method: 'POST',
                    body: JSON.stringify({
                        etiqueta,
                        latitud: Number(latitud.toFixed(6)),
                        longitud: Number(longitud.toFixed(6))
                    })
                });
                estado.textContent = 'Guardado en Ubicaciones favoritas.';
                nombre.value = '';
                await cargarFavoritas();
            } catch (error) {
                estado.textContent = error.message;
            } finally {
                boton.disabled = false;
            }
        });

        caja.append(formularioPopup);
        return caja;
    }

    if (window.mapa && typeof window.mapa.agregarAccionPunto === 'function') {
        window.mapa.agregarAccionPunto(crearAccionFavorita);
    }

})();
