const REFRESCO_MINUTOS = 15;
const REFRESCO_MILISEGUNDOS = REFRESCO_MINUTOS * 60 * 1000;

let temporizadorId = null;

function renderizarClima(snapshot) {
    const ultima = document.getElementById('ultima-actualizacion');
    const latitud = document.getElementById('latitud');
    const longitud = document.getElementById('longitud');
    const temperatura = document.getElementById('temperatura');

    latitud.textContent = snapshot.latitude.toFixed(4) + '°';
    longitud.textContent = snapshot.longitude.toFixed(4) + '°';

    if (typeof snapshot.temperature === 'number') {
        temperatura.textContent = snapshot.temperature.toFixed(1) + '°C';
    } else {
        temperatura.textContent = 'Cargando...';
    }

    ultima.textContent = snapshot.lastUpdated ? snapshot.lastUpdated : '—';
}

async function cargarClima() {
    try {
        const respuesta = await fetch('/api/weather');
        if (!respuesta.ok) {
            return;
        }
        const datos = await respuesta.json();
        renderizarClima(datos);

        if (window.mapa && window.mapa.centrarEnLatLng) {
            window.mapa.centrarEnLatLng(datos.latitude, datos.longitude);
        }
    } catch (error) {
        console.error('Error al obtener datos del clima:', error);
    }
}

function iniciarRefrescoAutomatico() {
    limpiarRefrescoAutomatico();
    cargarClima();
    temporizadorId = setInterval(cargarClima, REFRESCO_MILISEGUNDOS);
}

function limpiarRefrescoAutomatico() {
    if (temporizadorId !== null) {
        clearInterval(temporizadorId);
        temporizadorId = null;
    }
}

document.addEventListener('DOMContentLoaded', iniciarRefrescoAutomatico);
window.addEventListener('beforeunload', limpiarRefrescoAutomatico);