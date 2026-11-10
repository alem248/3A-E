/**
 * Conecta el formulario del histórico con GET /api/historico/clima y reparte la respuesta
 * entre el resumen, el calendario y el gráfico.
 */
const HistoricoPagina = (() => {
    const formulario = document.getElementById('formulario-historico');
    const boton = document.getElementById('boton-historico');
    const estado = document.getElementById('estado-historico');
    const resumenEl = document.getElementById('historico-resumen');
    const calendarioEl = document.getElementById('historico-calendario');
    const graficoEl = document.getElementById('historico-grafico');

    /** Devuelve un mensaje de error, o null si el formulario es valido. Las fechas ISO se comparan como texto. */
    function validarFormulario(desde, hasta, meses) {
        if (!desde || !hasta) {
            return 'Indica las fechas de inicio y fin.';
        }
        if (desde > hasta) {
            return 'La fecha de inicio no puede ser posterior a la de fin.';
        }
        if (meses.length === 0) {
            return 'Selecciona al menos un mes de temporada.';
        }
        return null;
    }

    function construirUrl(desde, hasta, meses) {
        const params = new URLSearchParams({ desde, hasta, meses: meses.join(',') });
        return '/api/historico/clima?' + params.toString();
    }

    function mesesSeleccionados() {
        return [...formulario.querySelectorAll('input[name="mes"]:checked')].map((casilla) => Number(casilla.value));
    }

    function mostrarEstado(texto, esError = false) {
        estado.textContent = texto;
        estado.classList.toggle('estado-error', esError);
    }

    function formatear(valor, unidad) {
        return valor === null || valor === undefined ? '—' : valor.toFixed(1) + unidad;
    }

    function renderizarResumen(resumen) {
        const datos = [
            ['Días considerados', String(resumen.diasConsiderados)],
            ['Temp. máxima promedio', formatear(resumen.temperaturaMaxPromedioC, ' °C')],
            ['Temp. mínima promedio', formatear(resumen.temperaturaMinPromedioC, ' °C')],
            ['Lluvia total', formatear(resumen.precipitacionTotalMm, ' mm')]
        ];
        resumenEl.replaceChildren(...datos.map(([etiqueta, valor]) => {
            const caja = document.createElement('div');
            const titulo = document.createElement('span');
            const dato = document.createElement('strong');
            titulo.textContent = etiqueta;
            dato.textContent = valor;
            caja.append(titulo, dato);
            return caja;
        }));
        resumenEl.hidden = false;
    }

    function renderizarResultado(datos) {
        renderizarResumen(datos.resumen);
        HistoricoCalendario.renderizar(calendarioEl, datos.registros);
        HistoricoGrafico.renderizar(graficoEl, datos.registros);
    }

    async function consultar(evento) {
        evento.preventDefault();
        const desde = document.getElementById('historico-desde').value;
        const hasta = document.getElementById('historico-hasta').value;
        const meses = mesesSeleccionados();

        const error = validarFormulario(desde, hasta, meses);
        if (error) {
            mostrarEstado(error, true);
            return;
        }

        boton.disabled = true;
        mostrarEstado('Consultando el histórico...');
        try {
            const respuesta = await fetch(construirUrl(desde, hasta, meses));
            const datos = await respuesta.json().catch(() => null);
            if (!respuesta.ok) {
                mostrarEstado(datos && datos.mensaje ? datos.mensaje : 'No se pudo obtener el histórico.', true);
                return;
            }
            renderizarResultado(datos);
            mostrarEstado(`Se encontraron ${datos.resumen.diasConsiderados} días.`);
        } catch (fallo) {
            mostrarEstado('No se pudo conectar con el servidor. Intenta de nuevo.', true);
        } finally {
            boton.disabled = false;
        }
    }

    formulario.addEventListener('submit', consultar);

    return { validarFormulario, construirUrl };
})();
