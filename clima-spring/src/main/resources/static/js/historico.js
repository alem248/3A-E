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
    const comparacionEl = document.getElementById('historico-comparacion');

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
        calendarioEl.hidden = false;
        graficoEl.hidden = false;
        HistoricoCalendario.renderizar(calendarioEl, datos.registros);
        HistoricoGrafico.renderizar(graficoEl, datos.registros);
        renderizarComparacion(datos.resumen);
    }

    /** Compara la temperatura actual del punto con el promedio de máximas del histórico consultado. */
    async function renderizarComparacion(resumen) {
        if (typeof resumen.temperaturaMaxPromedioC !== 'number') {
            return;
        }
        try {
            const respuesta = await fetch('/api/weather');
            if (!respuesta.ok) {
                return;
            }
            const clima = await respuesta.json();
            if (typeof clima.temperature !== 'number') {
                return;
            }
            const promedio = resumen.temperaturaMaxPromedioC;
            const diferencia = clima.temperature - promedio;
            const posicion = Math.abs(diferencia) < 0.05
                ? 'igual al promedio histórico'
                : diferencia > 0 ? 'por encima del promedio histórico' : 'por debajo del promedio histórico';
            const signo = diferencia > 0 ? '+' : '';

            const titulo = document.createElement('strong');
            titulo.textContent = 'Situación actual frente al histórico';
            const detalle = document.createElement('p');
            detalle.textContent = `Temperatura actual: ${clima.temperature.toFixed(1)} °C · ` +
                `Promedio histórico de máximas: ${promedio.toFixed(1)} °C · ` +
                `Diferencia: ${signo}${diferencia.toFixed(1)} °C (${posicion}).`;
            comparacionEl.replaceChildren(titulo, detalle);
            comparacionEl.hidden = false;
        } catch (fallo) {
            comparacionEl.replaceChildren();
            comparacionEl.hidden = true;
        }
    }

    /** Oculta y vacía los resultados de una consulta anterior para no mostrar datos de otras fechas. */
    function limpiarResultados() {
        resumenEl.replaceChildren();
        resumenEl.hidden = true;
        comparacionEl.replaceChildren();
        comparacionEl.hidden = true;
        calendarioEl.replaceChildren();
        calendarioEl.hidden = true;
        graficoEl.replaceChildren();
        graficoEl.hidden = true;
    }

    async function consultar(evento) {
        evento.preventDefault();
        const desde = document.getElementById('historico-desde').value;
        const hasta = document.getElementById('historico-hasta').value;
        const meses = mesesSeleccionados();

        const error = validarFormulario(desde, hasta, meses);
        if (error) {
            limpiarResultados();
            mostrarEstado(error, true);
            return;
        }

        boton.disabled = true;
        mostrarEstado('Consultando el histórico...');
        try {
            const respuesta = await fetch(construirUrl(desde, hasta, meses));
            const datos = await respuesta.json().catch(() => null);
            if (!respuesta.ok) {
                limpiarResultados();
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