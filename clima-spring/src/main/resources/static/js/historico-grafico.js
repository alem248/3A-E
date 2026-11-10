/**
 * Grafico comparativo del histórico, en SVG sin librerias:
 * arriba, temperatura maxima y minima (lineas); abajo, precipitacion diaria (barras).
 * Expone HistoricoGrafico.renderizar(contenedor, registros).
 */
const HistoricoGrafico = (() => {
    const ANCHO = 800;
    const ALTO_TEMPERATURA = 200;
    const ALTO_LLUVIA = 110;
    const MARGEN_IZQ = 48;
    const MARGEN_DER = 12;
    const ESPACIO_PANELES = 36;
    const SVG_NS = 'http://www.w3.org/2000/svg';

    /** Devuelve una funcion que lleva un valor del dominio [dMin, dMax] al rango [rMin, rMax]. */
    function escalaLineal(dMin, dMax, rMin, rMax) {
        const pendiente = dMax === dMin ? 0 : (rMax - rMin) / (dMax - dMin);
        return (valor) => rMin + (valor - dMin) * pendiente;
    }

    /** Dominio con margen para que las lineas no toquen los bordes. */
    function dominioTemperatura(registros) {
        const valores = registros.flatMap((r) => [r.temperaturaMaxC, r.temperaturaMinC])
            .filter((v) => v !== null && v !== undefined);
        if (valores.length === 0) {
            return [0, 40];
        }
        return [Math.floor(Math.min(...valores)) - 1, Math.ceil(Math.max(...valores)) + 1];
    }

    function dominioLluvia(registros) {
        const valores = registros.map((r) => r.precipitacionMm).filter((v) => v !== null && v !== undefined);
        const maximo = valores.length ? Math.max(...valores) : 0;
        return [0, Math.max(1, Math.ceil(maximo))];
    }

    /** Construye el atributo "d" de una linea. Un valor nulo corta la linea, sin unirla con el siguiente. */
    function trazado(valores, escalaX, escalaY) {
        let d = '';
        let abierto = false;
        valores.forEach((valor, indice) => {
            if (valor === null || valor === undefined) {
                abierto = false;
                return;
            }
            const x = escalaX(indice).toFixed(1);
            const y = escalaY(valor).toFixed(1);
            d += (abierto ? ' L ' : ' M ') + x + ' ' + y;
            abierto = true;
        });
        return d.trim();
    }

    function crearElemento(nombre, atributos = {}) {
        const el = document.createElementNS(SVG_NS, nombre);
        Object.entries(atributos).forEach(([clave, valor]) => el.setAttribute(clave, String(valor)));
        return el;
    }

    function dibujarEjeY(svg, escala, [dMin, dMax], x0, alto, unidad) {
        const pasos = 4;
        for (let i = 0; i <= pasos; i++) {
            const valor = dMin + ((dMax - dMin) * i) / pasos;
            const y = escala(valor);
            svg.append(crearElemento('line', { x1: x0, x2: ANCHO - MARGEN_DER, y1: y, y2: y, class: 'grafico-rejilla' }));
            const texto = crearElemento('text', { x: x0 - 6, y: y + 4, 'text-anchor': 'end', class: 'grafico-eje' });
            texto.textContent = Math.round(valor) + unidad;
            svg.append(texto);
        }
    }

    function renderizar(contenedor, registros) {
        const svg = crearElemento('svg', {
            viewBox: `0 0 ${ANCHO} ${ALTO_TEMPERATURA + ALTO_LLUVIA + ESPACIO_PANELES + 40}`,
            role: 'img',
            class: 'grafico-svg',
            preserveAspectRatio: 'xMidYMid meet'
        });
        const titulo = crearElemento('title');
        titulo.textContent = 'Temperatura máxima, mínima y precipitación diaria';
        svg.append(titulo);

        const n = registros.length;
        const areaX = (indice) => MARGEN_IZQ + (n <= 1 ? 0 : (indice * (ANCHO - MARGEN_IZQ - MARGEN_DER)) / (n - 1));
        const anchoBarra = Math.max(1, (ANCHO - MARGEN_IZQ - MARGEN_DER) / Math.max(n, 1) - 1);

        // Panel superior: temperatura
        const yTemp = 10;
        const dominioTemp = dominioTemperatura(registros);
        const escalaTemp = escalaLineal(dominioTemp[0], dominioTemp[1], yTemp + ALTO_TEMPERATURA, yTemp);
        dibujarEjeY(svg, escalaTemp, dominioTemp, MARGEN_IZQ, ALTO_TEMPERATURA, ' °C');
        svg.append(crearElemento('path', {
            d: trazado(registros.map((r) => r.temperaturaMaxC), areaX, escalaTemp),
            class: 'grafico-linea grafico-linea-max', fill: 'none'
        }));
        svg.append(crearElemento('path', {
            d: trazado(registros.map((r) => r.temperaturaMinC), areaX, escalaTemp),
            class: 'grafico-linea grafico-linea-min', fill: 'none'
        }));

        // Panel inferior: lluvia
        const yLluvia = yTemp + ALTO_TEMPERATURA + ESPACIO_PANELES;
        const dominioLl = dominioLluvia(registros);
        const escalaLl = escalaLineal(dominioLl[0], dominioLl[1], yLluvia + ALTO_LLUVIA, yLluvia);
        dibujarEjeY(svg, escalaLl, dominioLl, MARGEN_IZQ, ALTO_LLUVIA, ' mm');
        registros.forEach((registro, indice) => {
            if (registro.precipitacionMm === null || registro.precipitacionMm === undefined) {
                return;
            }
            const yTope = escalaLl(registro.precipitacionMm);
            svg.append(crearElemento('rect', {
                x: (areaX(indice) - anchoBarra / 2).toFixed(1),
                y: yTope.toFixed(1),
                width: anchoBarra.toFixed(1),
                height: (yLluvia + ALTO_LLUVIA - yTope).toFixed(1),
                class: 'grafico-barra'
            }));
        });

        contenedor.replaceChildren(svg);
        contenedor.hidden = n === 0;
    }

    return { escalaLineal, dominioTemperatura, dominioLluvia, trazado, renderizar };
})();
