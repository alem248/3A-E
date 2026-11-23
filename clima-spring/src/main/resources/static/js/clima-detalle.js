// Detalle del clima de tu ubicación GPS: próximas 12 horas, anillos y curva.
// Aparece solo después de presionar GPS. Solo presentación.
(() => {
    const contenedor = document.getElementById('clima-detalle');
    if (!contenedor) return;

    const URL_HORAS = 'https://api.open-meteo.com/v1/forecast';
    const REFRESCO_MS = 15 * 60 * 1000;
    let coordenadas = null;
    let temporizador = null;

    function anillo(valor, maximo, color, etiqueta, unidad, icono) {
        const radio = 26;
        const circunferencia = 2 * Math.PI * radio;
        const fraccion = Math.min(1, Math.max(0, valor / maximo));
        return `<div class="anillo-caja">` +
            `<div class="anillo">` +
            `<svg viewBox="0 0 64 64" aria-hidden="true">` +
            `<circle class="anillo-base" cx="32" cy="32" r="${radio}"/>` +
            `<circle class="anillo-valor" cx="32" cy="32" r="${radio}" stroke="${color}" ` +
            `style="--c:${circunferencia.toFixed(1)};--f:${(circunferencia * (1 - fraccion)).toFixed(1)}"/>` +
            `</svg>` +
            `<span class="anillo-centro">${Math.round(valor)}${unidad}</span></div>` +
            `<div class="anillo-texto" style="color:${color}">${icono}<small>${etiqueta}</small></div></div>`;
    }

    function curva(temperaturas, horas) {
        const ancho = 300;
        const alto = 90;
        const margen = 8;
        const minimo = Math.min(...temperaturas);
        const maximo = Math.max(...temperaturas);
        const rango = maximo - minimo || 1;
        const puntos = temperaturas.map((t, i) => [
            margen + (i * (ancho - 2 * margen)) / (temperaturas.length - 1),
            alto - margen - ((t - minimo) / rango) * (alto - 2 * margen)
        ]);
        const linea = puntos.map(([px, py], i) => `${i ? 'L' : 'M'}${px.toFixed(1)} ${py.toFixed(1)}`).join(' ');
        const area = `${linea} L${puntos[puntos.length - 1][0].toFixed(1)} ${alto} L${puntos[0][0].toFixed(1)} ${alto} Z`;
        const etiquetas = horas.map((hora, i) =>
            `<div><small>${hora}</small><strong>${Math.round(temperaturas[i])}°</strong></div>`
        ).join('');
        return `<svg class="curva" viewBox="0 0 ${ancho} ${alto}" preserveAspectRatio="none" aria-hidden="true">` +
            `<defs><linearGradient id="curva-relleno" x1="0" y1="0" x2="0" y2="1">` +
            `<stop offset="0" stop-color="#fb923c" stop-opacity=".45"/><stop offset="1" stop-color="#fb923c" stop-opacity="0"/>` +
            `</linearGradient></defs>` +
            `<path class="curva-relleno" d="${area}" fill="url(#curva-relleno)"/>` +
            `<path class="curva-linea" pathLength="1" d="${linea}"/></svg>` +
            `<div class="curva-horas">${etiquetas}</div>`;
    }

    async function cargar() {
        if (coordenadas === null) return;
        try {
            const parametros = new URLSearchParams({
                latitude: coordenadas.latitud,
                longitude: coordenadas.longitud,
                hourly: 'temperature_2m,relative_humidity_2m,wind_speed_10m,uv_index',
                timezone: 'America/Lima',
                forecast_days: '2'
            });
            const respuesta = await fetch(`${URL_HORAS}?${parametros}`);
            if (!respuesta.ok) return;
            const h = (await respuesta.json()).hourly;

            const horaActual = new Date().getHours();
            let inicio = h.time.findIndex((t) => new Date(t).getHours() === horaActual);
            if (inicio < 0) inicio = 0;
            const indices = [...Array(12).keys()].map((k) => inicio + k).filter((i) => i < h.time.length);
            const horas = indices.map((i) => new Date(h.time[i]).getHours() + 'h');

            contenedor.innerHTML =
                '<div class="clima-detalle-contenido">' +
                '<div class="anillos">' +
                anillo(h.relative_humidity_2m[inicio], 100, '#38bdf8', 'Humedad', '%', window.iconosClima.svg('gota')) +
                anillo(h.wind_speed_10m[inicio], 40, '#34d399', 'Viento km/h', '', window.iconosClima.svg('viento')) +
                anillo(h.uv_index[inicio], 12, '#facc15', 'Índice UV', '', window.iconosClima.svg('sol')) +
                '</div>' +
                '<h4 class="clima-detalle-titulo">Próximas 12 horas</h4>' +
                curva(indices.map((i) => h.temperature_2m[i]), horas) +
                '</div>';
            contenedor.hidden = false;
        } catch (error) {
            console.error('Error al cargar el detalle del clima:', error);
        }
    }

    document.addEventListener('ubicacion-gps', (evento) => {
        coordenadas = { latitud: evento.detail.latitud, longitud: evento.detail.longitud };
        cargar();
        if (temporizador !== null) clearInterval(temporizador);
        temporizador = setInterval(cargar, REFRESCO_MS);
    });
})();
