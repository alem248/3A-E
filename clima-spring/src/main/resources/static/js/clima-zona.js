// Resumen de tu zona: se activa con la posición del GPS (no requiere sesión),
// consulta Open-Meteo para hoy y 7 días, y cruza la región natural y el riesgo El Niño.
const climaZona = (() => {
    const URL_CLIMA = 'https://api.open-meteo.com/v1/forecast';
    const URL_LUGAR = 'https://nominatim.openstreetmap.org/reverse';
    let ultimo = null; // { latitud, longitud, lugar, datos }
    let solicitudId = 0;

    function nivelUV(uv) {
        if (uv < 3) return 'Bajo';
        if (uv < 6) return 'Moderado';
        if (uv < 8) return 'Alto';
        if (uv < 11) return 'Muy alto';
        return 'Extremo';
    }

    function descripcionCalor(maxima) {
        if (maxima < 18) return 'Fresco';
        if (maxima < 25) return 'Templado';
        if (maxima < 30) return 'Cálido';
        return 'Caluroso';
    }

    function descripcionHumedad(humedad) {
        if (humedad < 50) return 'Seca';
        if (humedad < 70) return 'Moderada';
        return 'Húmeda';
    }

    function descripcionLluvia(probabilidad) {
        if (probabilidad < 20) return 'No va a llover';
        if (probabilidad < 50) return 'Posible lluvia';
        return 'Va a llover';
    }

    function descripcionViento(kmh) {
        if (kmh < 12) return 'Calma';
        if (kmh < 20) return 'Brisa';
        if (kmh < 30) return 'Viento fuerte';
        return 'Viento muy fuerte';
    }

    function descripcionNino(porcentaje) {
        if (porcentaje === null) return 'Sin datos aún';
        if (porcentaje < 25) return 'Bajo';
        if (porcentaje < 50) return 'Moderado';
        if (porcentaje < 75) return 'Alto';
        return 'Muy alto';
    }

    const TONOS = {
        'Calor': '#fb923c',
        'Humedad': '#38bdf8',
        'Viento': '#34d399',
        'Lluvia': '#60a5fa',
        'Sol': '#facc15',
        'Intensidad de calor': '#fb923c',
        'Fenómeno El Niño': '#f87171'
    };

    // Indice del Niño 3.4 (NOAA), independiente del calor de la region.
    let ninoDatos = null;

    function textoNinoPrincipal() {
        if (!ninoDatos) return 'Sin datos';
        const intensidad = ninoDatos.intensidad ? ` ${ninoDatos.intensidad}` : '';
        return `${ninoDatos.clasificacion}${intensidad}`;
    }

    function textoNinoDetalle() {
        if (!ninoDatos) return 'Índice Niño 3.4 no disponible';
        return `Índice ${ninoDatos.valor.toFixed(1)} · ${ninoDatos.periodo}`;
    }

    function tarjeta(titulo, principal, detalle, tono) {
        const color = tono || TONOS[titulo] || '#94a3b8';
        return `<div class="zona-dato" style="--tono:${color}">` +
            `<span class="zona-icono">${window.iconosClima.para(titulo, principal)}</span>` +
            `<span class="zona-titulo">${titulo}</span>` +
            `<strong>${principal}</strong><small>${detalle}</small></div>`;
    }

    function nombreDia(fecha, indice) {
        if (indice === 0) return 'Hoy';
        return new Date(`${fecha}T12:00:00`).toLocaleDateString('es-PE', { weekday: 'short' });
    }

    function renderizarPronostico(diaria) {
        return '<div class="zona-pronostico">' + diaria.time.map((fecha, i) => {
            const lluvia = diaria.precipitation_probability_max[i];
            return `<div class="zona-dia"><span class="zona-titulo">${nombreDia(fecha, i)}</span>` +
                `<span class="zona-dia-icono">${window.iconosClima.pronostico(lluvia)}</span>` +
                `<strong>${Math.round(diaria.temperature_2m_max[i])}° / ${Math.round(diaria.temperature_2m_min[i])}°</strong>` +
                `<small>${Math.round(lluvia)}% lluvia</small>` +
                `<small>${Math.round(diaria.wind_speed_10m_max[i])} km/h</small></div>`;
        }).join('') + '</div>';
    }

    function renderizar() {
        if (ultimo === null || ultimo.datos === null) {
            return;
        }
        const contenedor = document.getElementById('clima-zona');
        const diaria = ultimo.datos.daily;
        const mapa = window.mapa;
        const region = mapa && mapa.regionEn ? mapa.regionEn(ultimo.latitud, ultimo.longitud) : null;
        const porcentaje = region && mapa.porcentajeRegion ? mapa.porcentajeRegion(region.clave) : null;
        const detalleNino = porcentaje === null
            ? 'Sin zonas registradas en tu región'
            : `Intensidad ${porcentaje}% en tu región`;
        const lugar = ultimo.lugar || (region ? region.nombre : 'Tu ubicación');

        contenedor.innerHTML =
            `<div class="zona-cabecera"><h3>Tu zona hoy · ${lugar}</h3>` +
            '<button type="button" class="boton-minimizar" aria-expanded="true">Minimizar</button></div>' +
            '<div class="zona-contenido"><div class="zona-grid">' +
            tarjeta('Región', region ? region.nombre : 'Fuera de Perú', region ? `${region.temperatura} · Humedad ${region.humedad}` : 'Sin región natural', region ? region.color : '#94a3b8') +
            tarjeta('Calor', descripcionCalor(diaria.temperature_2m_max[0]), `${Math.round(diaria.temperature_2m_min[0])}–${Math.round(diaria.temperature_2m_max[0])} °C`) +
            tarjeta('Humedad', descripcionHumedad(diaria.relative_humidity_2m_mean[0]), `${Math.round(diaria.relative_humidity_2m_mean[0])}% promedio`) +
            tarjeta('Viento', descripcionViento(diaria.wind_speed_10m_max[0]), `Hasta ${Math.round(diaria.wind_speed_10m_max[0])} km/h`) +
            tarjeta('Lluvia', descripcionLluvia(diaria.precipitation_probability_max[0]), `${Math.round(diaria.precipitation_probability_max[0])}% de probabilidad`) +
            tarjeta('Sol', nivelUV(diaria.uv_index_max[0]), `Índice UV máx. ${Math.round(diaria.uv_index_max[0])}`) +
            tarjeta('Intensidad de calor', descripcionNino(porcentaje), detalleNino) +
            tarjeta('Fenómeno El Niño', textoNinoPrincipal(), textoNinoDetalle()) +
            '</div>' +
            '<h4 class="zona-subtitulo">Pronóstico 7 días</h4>' +
            renderizarPronostico(diaria) + '</div>';
        contenedor.hidden = false;
        actualizarBotonesMinimizar();
    }

    async function obtenerLugar(latitud, longitud) {
        try {
            const parametros = new URLSearchParams({ format: 'jsonv2', zoom: '14', lat: latitud, lon: longitud });
            const respuesta = await fetch(`${URL_LUGAR}?${parametros}`, { headers: { 'Accept-Language': 'es' } });
            if (!respuesta.ok) return null;
            const direccion = (await respuesta.json()).address || {};
            const localidad = direccion.suburb || direccion.neighbourhood || direccion.city_district || direccion.town || direccion.village || direccion.city;
            const departamento = direccion.state;
            return [localidad, departamento].filter(Boolean).join(', ') || null;
        } catch (error) {
            return null;
        }
    }

    async function cargar(latitud, longitud) {
        const id = ++solicitudId;
        ultimo = { latitud, longitud, lugar: null, datos: null };
        renderizar();

        const parametros = new URLSearchParams({
            latitude: latitud,
            longitude: longitud,
            daily: 'uv_index_max,temperature_2m_max,temperature_2m_min,relative_humidity_2m_mean,precipitation_probability_max,wind_speed_10m_max',
            timezone: 'America/Lima',
            forecast_days: '7'
        });
        const [respuesta, lugar] = await Promise.all([
            fetch(`${URL_CLIMA}?${parametros}`),
            obtenerLugar(latitud, longitud)
        ]);
        if (!respuesta.ok) {
            throw new Error(`Open-Meteo respondió ${respuesta.status}`);
        }
        if (id !== solicitudId) return;
        ultimo = { latitud, longitud, lugar, datos: await respuesta.json() };
        renderizar();
    }

    fetch('/api/nino')
        .then((respuesta) => (respuesta.ok ? respuesta.json() : null))
        .then((datos) => {
            ninoDatos = datos;
            if (ultimo !== null) renderizar();
        })
        .catch(() => {});

    document.addEventListener('ubicacion-gps', (evento) => {
        const { latitud, longitud } = evento.detail;
        cargar(latitud, longitud).catch((error) => console.error('Error al obtener el clima de tu zona:', error));
    });

    // Cuando llegan las zonas, el porcentaje de El Niño de tu región ya está disponible
    document.addEventListener('clima:zonas-actualizadas', renderizar);

    function actualizarBotonesMinimizar() {
        const minimizado = document.body.dataset.climaMinimizado === 'true';
        document.querySelectorAll('.boton-minimizar').forEach((boton) => {
            boton.textContent = minimizado ? 'Ver detalles' : 'Minimizar';
            boton.setAttribute('aria-expanded', String(!minimizado));
        });
    }

    document.addEventListener('click', (evento) => {
        if (!evento.target.closest('.boton-minimizar')) return;
        document.body.dataset.climaMinimizado = document.body.dataset.climaMinimizado === 'true' ? 'false' : 'true';
        actualizarBotonesMinimizar();
    });

    return { cargar };
})();
