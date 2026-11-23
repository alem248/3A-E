/*
 * Agrega una fotografía a cada alerta térmica.
 * Solo presentación: no modifica temperaturas, niveles ni umbrales, y no
 * depende del código de alertas-temperatura.js.
 *
 * Las fotos van en static/img/zonas/. Si un archivo no existe, la alerta
 * se muestra sin foto.
 */
(function () {
    'use strict';

    const CARPETA = '/img/zonas/';

    // Por ciudad o zona: se busca dentro del nombre de la alerta.
    // El orden importa: la primera coincidencia gana.
    const FOTOS_ZONA = [
        { clave: 'tumbes', archivo: 'tumbes.jpg', texto: 'Playas de Tumbes' },
        { clave: 'paita', archivo: 'paita.jpg', texto: 'Puerto de Paita, Piura' },
        { clave: 'chimbote', archivo: 'chimbote.jpg', texto: 'Bahía de Chimbote, La Libertad' },
        { clave: 'piura norte', archivo: 'piura-norte.jpg', texto: 'Costa norte de Piura' },
        { clave: 'lambayeque', archivo: 'lambayeque.jpg', texto: 'Lambayeque' },
        { clave: 'la libertad norte', archivo: 'trujillo.jpg', texto: 'Centro histórico de Trujillo' }
    ];

    // Por departamento: se usa si la zona no tiene foto propia.
    const FOTOS_DEPARTAMENTO = [
        { clave: 'tumbes', archivo: 'dep-tumbes.jpg', texto: 'Tumbes' },
        { clave: 'piura', archivo: 'dep-piura.jpg', texto: 'Piura' },
        { clave: 'lambayeque', archivo: 'dep-lambayeque.jpg', texto: 'Lambayeque' },
        { clave: 'la libertad', archivo: 'dep-la-libertad.jpg', texto: 'La Libertad' }
    ];

    function normalizar(texto) {
        return (texto || '')
            .normalize('NFD')
            .replace(/[̀-ͯ]/g, '')
            .toLowerCase()
            .trim();
    }

    function buscarFoto(nombreZona, departamento) {
        const zona = normalizar(nombreZona);
        const depto = normalizar(departamento);
        const porZona = FOTOS_ZONA.find(f => zona.includes(f.clave));
        if (porZona) return porZona;
        return FOTOS_DEPARTAMENTO.find(f => depto.includes(f.clave)) || null;
    }

    function agregarFoto(item) {
        if (item.dataset.fotoZona === '1') return;
        item.dataset.fotoZona = '1';

        const nombre = item.querySelector('.alerta-termica-zona strong');
        const departamento = item.querySelector('.alerta-termica-zona span');
        const foto = buscarFoto(
            nombre ? nombre.textContent : '',
            departamento ? departamento.textContent : ''
        );
        if (!foto) return;

        const img = document.createElement('img');
        img.className = 'alerta-foto';
        img.src = CARPETA + foto.archivo;
        img.alt = foto.texto;
        img.loading = 'lazy';
        img.onerror = function () {
            img.remove();
            item.classList.remove('con-foto');
        };

        item.classList.add('con-foto');
        item.prepend(img);
    }

    function procesarTodas() {
        document.querySelectorAll('#alertas-lista .alerta-termica').forEach(agregarFoto);
    }

    // Observa todo el documento: si la lista se reemplaza por completo, sigue funcionando.
    // Las alertas ya procesadas se marcan con data-foto-zona, así que no hay bucle.
    new MutationObserver(procesarTodas).observe(document.body, { childList: true, subtree: true });
    procesarTodas();
})();