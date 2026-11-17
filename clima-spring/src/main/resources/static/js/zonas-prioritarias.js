(() => {
    const lista = document.getElementById('zonas-prioritarias-lista');
    const estado = document.getElementById('zonas-prioritarias-estado');
    const priorizadas = new Set();
    let zonas = [];

    function ordenar(items) {
        return [...items].sort((a, b) => {
            const prioridad = Number(priorizadas.has(String(b.id))) - Number(priorizadas.has(String(a.id)));
            return prioridad || Number(a.id) - Number(b.id);
        });
    }

    function renderizar() {
        lista.replaceChildren();
        const ordenadas = ordenar(zonas);
        ordenadas.forEach((zona) => {
            const item = document.createElement('li');
            item.className = 'zona-prioritaria-item';

            const datos = document.createElement('div');
            datos.className = 'zona-prioritaria-datos';
            const nombre = document.createElement('span');
            nombre.className = 'zona-prioritaria-nombre';
            nombre.textContent = zona.nombre ?? `Zona ${zona.id}`;
            const detalle = document.createElement('span');
            detalle.className = 'zona-prioritaria-detalle';
            const temperatura = Number.isFinite(zona.tsm) ? `${zona.tsm.toFixed(1)} °C` : 'Sin lectura';
            detalle.textContent = `${zona.departamento ?? 'Costa norte'} · ${temperatura} · ${zona.nivel?.nombre ?? 'Sin riesgo disponible'}`;
            datos.append(nombre, detalle);

            const marcada = priorizadas.has(String(zona.id));
            const estrella = document.createElement('button');
            estrella.type = 'button';
            estrella.className = 'zona-prioritaria-estrella';
            estrella.setAttribute('aria-pressed', String(marcada));
            estrella.setAttribute('aria-label', `${marcada ? 'Quitar' : 'Marcar'} ${nombre.textContent} ${marcada ? 'de' : 'como'} prioritarias`);
            estrella.textContent = marcada ? '★' : '☆';
            estrella.addEventListener('click', () => {
                if (marcada) priorizadas.delete(String(zona.id));
                else priorizadas.add(String(zona.id));
                renderizar();
            });

            item.append(datos, estrella);
            lista.append(item);
        });

        estado.textContent = zonas.length
            ? `${zonas.length} zonas disponibles. Las prioritarias aparecen primero.`
            : 'No hay zonas disponibles para mostrar.';
    }

    document.addEventListener('clima:zonas-actualizadas', (evento) => {
        zonas = Array.isArray(evento.detail) ? evento.detail : [];
        renderizar();
    });
})();
