(() => {
    const INTERVALO_ACTUALIZACION = 15 * 60 * 1000;
    const ordenNivel = { VERDE: 0, AMARILLO: 1, NARANJA: 2, ROJO: 3 };
    let zonas = [];
    let nivelSeleccionado = 'TODAS';
    let temporizadorId = null;

    const elemento = (id) => document.getElementById(id);

    function formatearTemperatura(valor) {
        return typeof valor === 'number' && Number.isFinite(valor)
            ? `${valor.toFixed(1)} °C`
            : 'Sin dato';
    }

    function formatearHora(valor) {
        if (!valor) return 'Hora no disponible';
        const fecha = new Date(valor);
        if (Number.isNaN(fecha.getTime())) return 'Hora no disponible';
        return fecha.toLocaleString('es-PE', {
            day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
        });
    }

    function formatearUmbral(nivel) {
        const inferior = nivel.umbralInferior;
        const superior = nivel.umbralSuperior;
        if (inferior !== null && superior === null) return `≥ ${inferior.toFixed(1)} °C`;
        if (inferior === null && superior !== null) return `< ${superior.toFixed(1)} °C`;
        if (inferior !== null && superior !== null) return `${inferior.toFixed(1)} a < ${superior.toFixed(1)} °C`;
        return 'Sin umbral';
    }

    function crearResumen(conteos, alertas) {
        const resumen = elemento('alertas-resumen');
        resumen.replaceChildren();

        Object.entries(conteos).forEach(([id, cantidad]) => {
            if (cantidad === 0) return;
            const nivel = alertas.find((alerta) => alerta.nivel.id === id)?.nivel;
            if (!nivel) return;

            const item = document.createElement('li');
            const nombre = document.createElement('span');
            const total = document.createElement('strong');
            const color = /^#[\da-f]{6}$/i.test(nivel.color) ? nivel.color : '#64748b';
            nombre.textContent = nivel.nombre;
            nombre.style.setProperty('--nivel-color', color);
            total.textContent = String(cantidad);
            item.append(nombre, total);
            resumen.append(item);
        });

        resumen.hidden = resumen.childElementCount === 0;
    }

    function actualizarFiltro(alertas) {
        const contenedor = elemento('alertas-filtro-contenedor');
        const filtro = elemento('alertas-filtro');
        const niveles = [...new Map(alertas.map((alerta) => [alerta.nivel.id, alerta.nivel])).values()]
            .sort((a, b) => (ordenNivel[a.id] ?? 0) - (ordenNivel[b.id] ?? 0));

        if (!niveles.some((nivel) => nivel.id === nivelSeleccionado)) nivelSeleccionado = 'TODAS';
        filtro.replaceChildren(new Option('Todos los niveles', 'TODAS'));
        niveles.forEach((nivel) => filtro.add(new Option(nivel.nombre, nivel.id)));
        filtro.value = nivelSeleccionado;
        contenedor.hidden = niveles.length < 2;
    }

    function crearTarjeta(zona) {
        const item = document.createElement('li');
        item.className = 'alerta-termica';
        const color = /^#[\da-f]{6}$/i.test(zona.nivel.color) ? zona.nivel.color : '#94A3B8';
        item.style.setProperty('--alerta-color', color);

        const indicador = document.createElement('span');
        indicador.className = 'alerta-termica-indicador';
        indicador.setAttribute('aria-hidden', 'true');

        const datosZona = document.createElement('div');
        datosZona.className = 'alerta-termica-zona';
        const nombre = document.createElement('strong');
        nombre.textContent = zona.nombre ?? 'Zona sin nombre';
        const departamento = document.createElement('span');
        departamento.textContent = zona.departamento ?? 'Costa norte';
        datosZona.append(nombre, departamento);

        const datosTemperatura = document.createElement('div');
        datosTemperatura.className = 'alerta-termica-temperatura';
        const temperatura = document.createElement('strong');
        temperatura.textContent = formatearTemperatura(zona.tsm);
        const etiquetaTemperatura = document.createElement('span');
        etiquetaTemperatura.textContent = 'Temperatura actual';
        datosTemperatura.append(temperatura, etiquetaTemperatura);

        const datosRiesgo = document.createElement('div');
        datosRiesgo.className = 'alerta-termica-riesgo';
        const nivel = document.createElement('span');
        nivel.className = 'alerta-termica-nivel';
        nivel.textContent = zona.nivel.nombre;
        const descripcion = document.createElement('span');
        descripcion.textContent = zona.nivel.descripcion;
        const umbral = document.createElement('span');
        umbral.className = 'alerta-termica-umbral';
        umbral.textContent = `Umbral: ${formatearUmbral(zona.nivel)}`;
        datosRiesgo.append(nivel, descripcion, umbral);

        const hora = document.createElement('time');
        hora.className = 'alerta-termica-hora';
        if (zona.actualizadoEn) hora.dateTime = zona.actualizadoEn;
        hora.textContent = formatearHora(zona.actualizadoEn);

        item.append(indicador, datosZona, datosTemperatura, datosRiesgo, hora);
        return item;
    }

    function renderizar(zonasActualizadas) {
        zonas = Array.isArray(zonasActualizadas) ? zonasActualizadas : [];
        const alertas = zonas
            .filter((zona) => zona.disponible && zona.nivel?.id && zona.nivel.id !== 'VERDE')
            .sort((a, b) => (ordenNivel[b.nivel.id] ?? 0) - (ordenNivel[a.nivel.id] ?? 0));
        const sinLectura = zonas.filter((zona) => !zona.disponible);
        const conteos = alertas.reduce((totales, alerta) => {
            totales[alerta.nivel.id] = (totales[alerta.nivel.id] ?? 0) + 1;
            return totales;
        }, {});

        const contador = elemento('alertas-contador');
        contador.textContent = `${alertas.length} ${alertas.length === 1 ? 'alerta' : 'alertas'}`;
        contador.classList.toggle('alertas-contador-activo', alertas.length > 0);
        crearResumen(conteos, alertas);
        actualizarFiltro(alertas);

        const lista = elemento('alertas-lista');
        lista.replaceChildren();
        const alertasVisibles = nivelSeleccionado === 'TODAS'
            ? alertas
            : alertas.filter((alerta) => alerta.nivel.id === nivelSeleccionado);
        alertasVisibles.forEach((zona) => lista.append(crearTarjeta(zona)));

        const estado = elemento('alertas-estado');
        if (zonas.length === 0) {
            estado.textContent = 'No se recibieron zonas térmicas.';
        } else if (alertas.length === 0 && sinLectura.length === zonas.length) {
            estado.textContent = 'Sin lecturas térmicas disponibles; no se puede determinar el riesgo.';
        } else if (alertas.length === 0) {
            estado.textContent = 'Sin alertas térmicas activas en las zonas con lectura.';
        } else if (alertasVisibles.length === 0) {
            estado.textContent = 'No hay alertas para el nivel seleccionado.';
        } else {
            estado.textContent = '';
        }

        const sinDatos = elemento('alertas-sin-datos');
        sinDatos.textContent = sinLectura.length
            ? `Sin lectura reciente: ${sinLectura.map((zona) => zona.nombre).join(', ')}.`
            : '';
        sinDatos.hidden = sinLectura.length === 0;
    }

    document.addEventListener('clima:zonas-actualizadas', (evento) => renderizar(evento.detail));
    document.addEventListener('DOMContentLoaded', () => {
        elemento('alertas-filtro').addEventListener('change', (evento) => {
            nivelSeleccionado = evento.target.value;
            renderizar(zonas);
        });
        temporizadorId = window.setInterval(() => window.mapa?.cargarZonas(), INTERVALO_ACTUALIZACION);
    });
    window.addEventListener('beforeunload', () => {
        if (temporizadorId !== null) window.clearInterval(temporizadorId);
    });
})();
