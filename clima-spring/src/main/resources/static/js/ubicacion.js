// Solicita el permiso de ubicación (GPS), captura las coordenadas del dispositivo
// y las registra en el backend para el usuario que tiene la sesión iniciada.
const ubicacion = (() => {
    const OPCIONES_GPS = {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 60000
    };

    const MENSAJES_ERROR_GPS = {
        1: 'Permiso de ubicación denegado. Actívalo en la configuración del navegador.',
        2: 'No se pudo determinar la ubicación (GPS sin señal).',
        3: 'Se agotó el tiempo de espera del GPS.'
    };

    function mostrarEstado(texto) {
        document.getElementById('estado-ubicacion').textContent = texto;
    }

    // 'granted' | 'denied' | 'prompt'. Algunos navegadores no exponen la Permissions API.
    async function consultarPermiso() {
        if (!navigator.permissions || !navigator.permissions.query) {
            return 'prompt';
        }

        try {
            const estado = await navigator.permissions.query({ name: 'geolocation' });
            return estado.state;
        } catch (error) {
            return 'prompt';
        }
    }

    // Devuelve el id del usuario con sesión iniciada, o null si no hay sesión
    async function usuarioActual() {
        try {
            const respuesta = await fetch('/api/auth/yo', { credentials: 'same-origin' });
            const datos = await respuesta.json();
            return datos.autenticado ? datos.id : null;
        } catch (error) {
            return null;
        }
    }

    // La primera llamada hace que el navegador muestre el diálogo de permiso
    function obtenerPosicionActual() {
        return new Promise((resolve, reject) => {
            if (!('geolocation' in navigator)) {
                reject(new Error('Este navegador no soporta geolocalización.'));
                return;
            }

            navigator.geolocation.getCurrentPosition(
                (posicion) => resolve({
                    latitud: posicion.coords.latitude,
                    longitud: posicion.coords.longitude,
                    precisionMetros: posicion.coords.accuracy,
                    fechaCaptura: new Date(posicion.timestamp).toISOString()
                }),
                (error) => reject(new Error(MENSAJES_ERROR_GPS[error.code] || error.message)),
                OPCIONES_GPS
            );
        });
    }

    async function registrarEnServidor(posicion) {
        const usuarioId = await usuarioActual();
        if (usuarioId === null) {
            throw new Error('Inicia sesión para guardar tu ubicación.');
        }

        const respuesta = await fetch(`/api/usuarios/${usuarioId}/ubicacion`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(posicion)
        });
        const cuerpo = await respuesta.json();

        if (!respuesta.ok) {
            throw new Error(cuerpo.mensaje || `Error ${respuesta.status} al guardar la ubicación.`);
        }

        return cuerpo;
    }

    function describir(registrada) {
        const coordenadas = `${Number(registrada.latitud).toFixed(4)}°, ${Number(registrada.longitud).toFixed(4)}°`;

        if (!registrada.enZonaVulnerable) {
            return `Ubicación guardada (${coordenadas}). Fuera de las zonas vulnerables.`;
        }

        const alertas = registrada.alertasVigentes.length > 0
            ? ` Alertas vigentes: ${registrada.alertasVigentes.map((alerta) => alerta.mensaje).join(' | ')}`
            : '';
        return `Ubicación guardada (${coordenadas}). Estás en ${registrada.zona.nombre}.${alertas}`;
    }

    // Clasifica la última ubicación frente a las zonas del fenómeno
    function describirComparacion(comparacion) {
        if (comparacion.estado === 'EN_ZONA') {
            return `Comparación: estás dentro de ${comparacion.zonaNombre} (nivel ${comparacion.nivelRiesgo}).`;
        }

        if (comparacion.estado === 'CERCA') {
            return `Comparación: estás a ${Number(comparacion.distanciaKm).toFixed(1)} km de ${comparacion.zonaNombre} (nivel ${comparacion.nivelRiesgo}).`;
        }

        return 'Comparación: estás fuera de las zonas vulnerables.';
    }

    async function cargarComparacion() {
        const contenedor = document.getElementById('comparacion-ubicacion');

        try {
            const usuarioId = await usuarioActual();
            if (usuarioId === null) {
                contenedor.textContent = '';
                return;
            }

            const respuesta = await fetch(`/api/usuarios/${usuarioId}/comparacion`, { credentials: 'same-origin' });
            if (!respuesta.ok) {
                contenedor.textContent = '';
                return;
            }
            contenedor.textContent = describirComparacion(await respuesta.json());
        } catch (error) {
            contenedor.textContent = '';
        }
    }

    // Debe llamarse desde un clic: los navegadores bloquean el diálogo de permiso sin gesto del usuario
    async function solicitarYRegistrar() {
        const boton = document.getElementById('boton-ubicacion');
        boton.disabled = true;
        mostrarEstado('Solicitando ubicación...');

        try {
            const posicion = await obtenerPosicionActual();

            // Se centra antes de guardar: así el mapa va a tu ubicación aunque el registro falle.
            if (window.mapa && window.mapa.centrarEnLatLng) {
                window.mapa.centrarEnLatLng(posicion.latitud, posicion.longitud, 18);
            }
            document.dispatchEvent(new CustomEvent('ubicacion-gps', {
                detail: { latitud: posicion.latitud, longitud: posicion.longitud }
            }));

            const registrada = await registrarEnServidor(posicion);
            mostrarEstado(describir(registrada));
            await cargarComparacion();

            document.dispatchEvent(new CustomEvent('ubicacion-cambio', { detail: registrada }));

        } catch (error) {
            mostrarEstado(error.message);
        } finally {
            boton.disabled = false;
        }
    }

    async function inicializar() {
        document.getElementById('boton-ubicacion').addEventListener('click', solicitarYRegistrar);

        if (await consultarPermiso() === 'denied') {
            mostrarEstado(MENSAJES_ERROR_GPS[1]);
        }

        cargarComparacion();
    }

    return { inicializar, solicitarYRegistrar };
})();

document.addEventListener('DOMContentLoaded', ubicacion.inicializar);

window.ubicacion = ubicacion;