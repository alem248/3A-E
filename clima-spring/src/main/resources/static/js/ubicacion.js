// Solicita el permiso de ubicacion (GPS), captura las coordenadas del dispositivo
// y las registra en el backend: POST /api/usuarios/{id}/ubicacion
const ubicacion = (() => {
    // Usuario demo sembrado por DatosInicialesLoader (hasta que exista login)
    const USUARIO_ID = 1;

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
        const respuesta = await fetch(`/api/usuarios/${USUARIO_ID}/ubicacion`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
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

    // Debe llamarse desde un clic: los navegadores bloquean el diálogo de permiso sin gesto del usuario
    async function solicitarYRegistrar() {
        const boton = document.getElementById('boton-ubicacion');
        boton.disabled = true;
        mostrarEstado('Solicitando ubicación...');

        try {
            const posicion = await obtenerPosicionActual();
            const registrada = await registrarEnServidor(posicion);
            mostrarEstado(describir(registrada));

            if (window.mapa && window.mapa.centrarEnLatLng) {
                window.mapa.centrarEnLatLng(posicion.latitud, posicion.longitud);
            }
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
    }

    return { inicializar, solicitarYRegistrar };
})();

document.addEventListener('DOMContentLoaded', ubicacion.inicializar);

window.ubicacion = ubicacion;
