(() => {
    const API_PRIORIDADES = '/api/zonas-prioritarias';
    const lista = document.getElementById('zonas-prioritarias-lista');
    const estado = document.getElementById('zonas-prioritarias-estado');
    const priorizadas = new Set();
    const operacionesPendientes = new Set();
    let zonas = [];
    let ordenServidor = [];
    let versionEvento = 0;

    function ordenar(items) {
        const posiciones = new Map(ordenServidor.map((id, indice) => [String(id), indice]));
        return [...items].sort((a, b) => {
            const idA = String(a.id);
            const idB = String(b.id);
            const posicionA = posiciones.get(idA);
            const posicionB = posiciones.get(idB);
            if (posicionA !== undefined && posicionB !== undefined) return posicionA - posicionB;

            const prioridad = Number(priorizadas.has(idB)) - Number(priorizadas.has(idA));
            return prioridad || idA.localeCompare(idB, 'es', { numeric: true });
        });
    }

    function renderizar(mensajeEstado = null) {
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
            estrella.setAttribute('aria-label', marcada
                ? `Quitar ${nombre.textContent} de la lista prioritaria`
                : `Marcar ${nombre.textContent} como prioritaria`);
            estrella.textContent = marcada ? '★' : '☆';
            estrella.disabled = operacionesPendientes.has(String(zona.id));
            estrella.addEventListener('click', () => actualizarPrioridad(zona, estrella));

            item.append(datos, estrella);
            lista.append(item);
        });

        estado.textContent = mensajeEstado ?? (zonas.length
            ? `${zonas.length} zonas disponibles. Las prioritarias aparecen primero.`
            : 'No hay zonas disponibles para mostrar.');
    }

    async function cargarPrioridades() {
        const respuesta = await fetch(API_PRIORIDADES, { headers: { Accept: 'application/json' } });
        if (!respuesta.ok) throw new Error('No se pudo consultar el estado de las prioridades.');
        const datos = await respuesta.json();
        if (!Array.isArray(datos)) throw new Error('La respuesta de prioridades no tiene el formato esperado.');

        priorizadas.clear();
        ordenServidor = [];
        datos.forEach((prioridad) => {
            const id = String(prioridad.zonaId);
            ordenServidor.push(id);
            if (prioridad.prioritaria === true) priorizadas.add(id);
        });
    }

    async function actualizarPrioridad(zona, boton) {
        const id = String(zona.id);
        if (operacionesPendientes.has(id)) return;

        operacionesPendientes.add(id);
        boton.disabled = true;
        estado.textContent = `Guardando prioridad para ${zona.nombre ?? `zona ${id}`}...`;

        try {
            const respuesta = await fetch(`${API_PRIORIDADES}/${encodeURIComponent(id)}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({ prioritaria: !priorizadas.has(id) })
            });
            if (!respuesta.ok) throw new Error('No se pudo guardar la prioridad.');

            const guardada = await respuesta.json();
            if (String(guardada.zonaId) !== id || typeof guardada.prioritaria !== 'boolean') {
                throw new Error('La respuesta de prioridad no coincide con la zona seleccionada.');
            }
            if (guardada.prioritaria) priorizadas.add(id);
            else priorizadas.delete(id);

            ordenServidor = [...zonas]
                .sort((a, b) => Number(priorizadas.has(String(b.id))) - Number(priorizadas.has(String(a.id)))
                    || String(a.id).localeCompare(String(b.id), 'es', { numeric: true }))
                .map((elemento) => String(elemento.id));
            const nombreZona = zona.nombre ?? `Zona ${id}`;
            const mensaje = `${nombreZona} ${guardada.prioritaria ? 'marcada como prioritaria' : 'quitada de las prioritarias'}.`;
            try {
                await cargarPrioridades();
                renderizar(mensaje);
            } catch (error) {
                console.error('La prioridad se guardó, pero no se pudo actualizar el orden desde el servidor:', error);
                renderizar(`${mensaje} El orden se actualizará cuando se recarguen las zonas.`);
            }
        } catch (error) {
            console.error('Error al actualizar la prioridad de la zona:', error);
            renderizar(`No se pudo guardar la prioridad de ${zona.nombre ?? `zona ${id}`}. Inténtalo de nuevo.`);
        } finally {
            operacionesPendientes.delete(id);
            renderizar(estado.textContent);
        }
    }

    document.addEventListener('clima:zonas-actualizadas', async (evento) => {
        const versionActual = ++versionEvento;
        zonas = Array.isArray(evento.detail) ? evento.detail : [];
        try {
            await cargarPrioridades();
            if (versionActual !== versionEvento) return;
            renderizar();
        } catch (error) {
            console.error('Error al cargar prioridades de zonas:', error);
            if (versionActual !== versionEvento) return;
            ordenServidor = [];
            renderizar('No se pudo cargar el orden guardado; puedes volver a intentarlo con las estrellas.');
        }
    });
})();
