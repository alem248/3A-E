/*
 * Sesion de usuario: iniciar sesion, registrarse y cerrar sesion.
 * Habla con /api/auth. Al cargar, pregunta si ya hay una sesion abierta.
 */
(function () {
    'use strict';

    const dialogo = document.getElementById('dialogo-sesion');
    const formularioLogin = document.getElementById('formulario-login');
    const formularioRegistro = document.getElementById('formulario-registro');
    const bloqueAnonimo = document.getElementById('sesion-anonima');
    const bloqueUsuario = document.getElementById('sesion-usuario');
    const nombre = document.getElementById('sesion-nombre');
    const enlaceAdmin = document.getElementById('enlace-admin');
    // Al iniciar sesion como administrador, Django abre el panel sin pedir otra contrasena.
    const URL_PANEL_ADMIN = 'http://localhost:8000/cuentas/desde-spring/?next=/';
    if (!dialogo || !formularioLogin || !formularioRegistro) return;

    function pintar(datos) {
        const conectado = Boolean(datos && datos.autenticado);
        bloqueAnonimo.hidden = conectado;
        bloqueUsuario.hidden = !conectado;
        nombre.textContent = conectado ? (datos.nombres || datos.email) : '';
        // Solo los administradores ven el enlace al panel de Django.
        if (enlaceAdmin) enlaceAdmin.hidden = !(conectado && datos.rol === 'ADMIN');
        document.dispatchEvent(new CustomEvent('sesion-cambio', { detail: datos || {} }));
    }

    async function pedir(url, cuerpo, metodo) {
        const respuesta = await fetch(url, {
            method: metodo || 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: cuerpo ? JSON.stringify(cuerpo) : undefined
        });
        if (respuesta.status === 204) return {};
        const datos = await respuesta.json().catch(() => ({}));
        if (!respuesta.ok) {
            const error = new Error(datos.mensaje || mensajeEstado(respuesta.status));
            error.estado = respuesta.status;
            throw error;
        }
        return datos;
    }

    function mensajeEstado(estado) {
        if (estado === 401) return 'Correo o contrasena incorrectos.';
        if (estado === 400) return 'Revisa los datos ingresados.';
        return 'No se pudo completar la accion. Intenta de nuevo.';
    }

    function abrir(modo) {
        const esLogin = modo !== 'registro';
        formularioLogin.hidden = !esLogin;
        formularioRegistro.hidden = esLogin;
        document.getElementById('titulo-dialogo').textContent = esLogin ? 'Iniciar sesion' : 'Crear cuenta';
        limpiarErrores();
        if (!dialogo.open) dialogo.showModal();
    }

    function limpiarErrores() {
        dialogo.querySelectorAll('.error-sesion').forEach((p) => { p.textContent = ''; });
    }

    function mostrarError(formulario, error) {
        formulario.querySelector('.error-sesion').textContent = error.message;
    }

    function datosDe(formulario) {
        const datos = new FormData(formulario);
        return Object.fromEntries(datos.entries());
    }

    document.getElementById('boton-iniciar').addEventListener('click', () => abrir('login'));
    document.getElementById('boton-registrarse').addEventListener('click', () => abrir('registro'));

    dialogo.querySelectorAll('.enlace-cambio').forEach((boton) => {
        boton.addEventListener('click', () => abrir(boton.dataset.ir));
    });

    formularioLogin.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limpiarErrores();
        try {
            const datos = await pedir('/api/auth/login', datosDe(formularioLogin));
            if (datos.rol === 'ADMIN') {
                // Navegacion normal: la flecha atras vuelve a esta pagina principal.
                window.location.href = URL_PANEL_ADMIN;
                return;
            }
            pintar(datos);
            formularioLogin.reset();
            dialogo.close();
        } catch (error) {
            mostrarError(formularioLogin, error);
        }
    });

    formularioRegistro.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limpiarErrores();
        try {
            const datos = await pedir('/api/auth/registro', datosDe(formularioRegistro));
            pintar(datos);
            formularioRegistro.reset();
            dialogo.close();
        } catch (error) {
            mostrarError(formularioRegistro, error);
        }
    });

    document.getElementById('boton-cerrar-sesion').addEventListener('click', async () => {
        try {
            await pedir('/api/auth/logout', null);
        } finally {
            pintar({ autenticado: false });
        }
    });

    function iniciarSesionActual() {
        return pedir('/api/auth/yo', null, 'GET')
            .then((datos) => {
                pintar(datos);
            })
            .catch(() => pintar({ autenticado: false }));
    }

    // Volver a esta pagina con la flecha atras o adelante (desde el panel de administracion)
    // cierra la sesion: el administrador sale del panel y ve la pagina principal sin sesion.
    function esNavegacionAtras() {
        const entrada = performance.getEntriesByType('navigation')[0];
        return Boolean(entrada && entrada.type === 'back_forward');
    }

    function cerrarSesionAlVolver() {
        pedir('/api/auth/logout', null)
            .catch(() => {})
            .finally(() => pintar({ autenticado: false }));
    }

    window.addEventListener('pageshow', (evento) => {
        if (evento.persisted || esNavegacionAtras()) cerrarSesionAlVolver();
    });

    // Con ?salir=1 el administrador cerro sesion desde el panel: cerramos la sesion de Spring.
    if (new URLSearchParams(window.location.search).has('salir')) {
        pedir('/api/auth/logout', null)
            .catch(() => {})
            .finally(() => {
                history.replaceState(null, '', window.location.pathname + window.location.hash);
                pintar({ autenticado: false });
            });
    } else {
        iniciarSesionActual();
    }
})();
