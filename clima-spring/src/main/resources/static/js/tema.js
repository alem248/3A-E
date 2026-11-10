(() => {
    const CLAVE_TEMA = 'clima-tema';
    const boton = document.getElementById('boton-tema');

    function aplicar(tema) {
        const oscuro = tema === 'oscuro';
        document.documentElement.dataset.theme = oscuro ? 'dark' : 'light';
        boton.setAttribute('aria-pressed', String(oscuro));
        boton.innerHTML = oscuro
            ? '<span aria-hidden="true">☀</span> Modo claro'
            : '<span aria-hidden="true">☾</span> Modo oscuro';
    }

    let preferencia = 'claro';
    try {
        preferencia = localStorage.getItem(CLAVE_TEMA) || 'claro';
    } catch (error) {
        // El cambio de tema sigue funcionando aunque el navegador bloquee storage.
    }
    aplicar(preferencia);

    boton.addEventListener('click', () => {
        const nuevoTema = document.documentElement.dataset.theme === 'dark' ? 'claro' : 'oscuro';
        aplicar(nuevoTema);
        try {
            localStorage.setItem(CLAVE_TEMA, nuevoTema);
        } catch (error) {
            // La preferencia queda aplicada para esta sesión.
        }
    });
})();
