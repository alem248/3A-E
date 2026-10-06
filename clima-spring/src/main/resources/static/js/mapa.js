const mapa = (() => {
    let mapaLeaflet = null;
    let marcador = null;
    let capaRiesgo = null;
    let vistaAjustada = false;

    const icono = L.icon({
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
    });

    function escapar(texto) {
        return String(texto).replace(
            /[&<>"']/g,
            (caracter) =>
                ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[caracter]
        );
    }

    function calcularIntensidad(nivel, tsm) {
        if (typeof tsm !== 'number' || !Number.isFinite(tsm)) {
            return 0;
        }

        const umbralInferior = nivel.umbralInferior;
        const umbralSuperior = nivel.umbralSuperior;
        let porcentaje = 100;

        if (umbralInferior === null && umbralSuperior !== null) {
            porcentaje = (tsm / 24) * 100;
        } else if (umbralInferior !== null && umbralSuperior !== null) {
            porcentaje = ((tsm - umbralInferior) / (umbralSuperior - umbralInferior)) * 100;
        }

        return Math.min(100, Math.max(0, porcentaje));
    }

    function construirPopup(zona) {
        const intensidad = calcularIntensidad(zona.nivel, zona.tsm);

        return (
            `<div style="font-family:system-ui,sans-serif;font-size:13px;min-width:190px">` +
            `<div style="font-size:15px;font-weight:600">${escapar(zona.nombre)}</div>` +
            `<div style="color:#6b6375;margin-bottom:6px">${escapar(zona.departamento)} · Costa norte</div>` +
            `<div>TSM: <strong>${escapar(zona.tsm)}</strong></div>` +
            `<div style="display:flex;align-items:center;gap:6px;margin-top:6px">` +
            `<span style="display:inline-block;width:12px;height:12px;border-radius:3px;background:${zona.nivel.color}"></span>` +
            `<strong style="color:${zona.nivel.color}">${escapar(zona.nivel.nombre)}</strong>` +
            `</div>` +
            `<div style="color:#6b6375">${escapar(zona.nivel.descripcion)}</div>` +
            `</div>`
        );
    }

    function renderizarLeyenda(zonas) {
        const leyenda = document.getElementById('leyenda');
        if (!zonas || zonas.length === 0) {
            leyenda.hidden = true;
            return;
        }

        const niveles = [...new Map(
            zonas.map((zona) => [zona.nivel.nombre, zona.nivel])
        ).values()];

        const items = niveles
            .map((nivel) =>
                `<li><span class="muestra-color" style="background:${nivel.color}"></span>` +
                `<span>${escapar(nivel.nombre)}: ${escapar(nivel.descripcion)}</span></li>`
            )
            .join('');

        leyenda.innerHTML = `<h3>Nivel de riesgo térmico</h3><ul>${items}</ul>`;
        leyenda.hidden = false;
    }

    function renderizarZonas(zonas) {
        const contenedorMapa = document.getElementById('mapa');
        const mapa = mapaLeaflet;

        if (mapa === null || zonas.length === 0) {
            return;
        }

        if (capaRiesgo === null) {
            capaRiesgo = L.featureGroup();
        }

        capaRiesgo.clearLayers();

        zonas.forEach((zona) => {
            const poligono = L.polygon(zona.poligono, {
                color: zona.nivel.color,
                weight: 2,
                opacity: 0.95,
                fillColor: zona.nivel.color,
                fillOpacity: 0.45
            });

            poligono.bindPopup(construirPopup(zona));
            poligono.addTo(capaRiesgo);
        });

        if (vistaAjustada === false) {
            mapa.addLayer(capaRiesgo);
            const limites = capaRiesgo.getBounds();

            if (limites.isValid()) {
                mapa.fitBounds(limites, { padding: [20, 20] });
            }

            vistaAjustada = true;
        } else if (mapa.hasLayer(capaRiesgo) === false) {
            mapa.addLayer(capaRiesgo);
        }

        renderizarLeyenda(zonas);
    }

    function inicializar() {
        const contenedor = document.getElementById('mapa');
        if (mapaLeaflet === null && contenedor) {
            mapaLeaflet = L.map('mapa').setView([-4.35, -81.35], 7);

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(mapaLeaflet);

            marcador = L.marker([-4.35, -81.35], { icon: icono }).addTo(mapaLeaflet);

            mapaLeaflet.on('click', (evento) => {
                consultarZonaEnPunto(evento.latlng.lat, evento.latlng.lng);
            });
        }
    }

    function centrarEnLatLng(latitud, longitud) {
        if (mapaLeaflet !== null) {
            mapaLeaflet.setView([latitud, longitud], 7);
            if (marcador !== null) {
                marcador.setLatLng([latitud, longitud]);
            }
        }
    }

    async function cargarZonas() {
        try {
            const respuesta = await fetch('/api/zonas');
            if (!respuesta.ok) {
                return;
            }
            const zonas = await respuesta.json();
            renderizarZonas(zonas);
        } catch (error) {
            console.error('Error al obtener zonas térmicas:', error);
        }
    }

    // Filtro por zona geográfica: al hacer clic en el mapa se consultan solo las
    // zonas cuyo polígono contiene el punto seleccionado.
    async function consultarZonaEnPunto(latitud, longitud) {
        try {
            const respuesta = await fetch(
                `/api/zonas?lat=${latitud}&lon=${longitud}`
            );
            if (!respuesta.ok) {
                return;
            }
            const zonas = await respuesta.json();

            if (zonas.length === 0) {
                L.popup()
                    .setLatLng([latitud, longitud])
                    .setContent('Sin zona térmica registrada en este punto.')
                    .openOn(mapaLeaflet);
                return;
            }

            L.popup()
                .setLatLng([latitud, longitud])
                .setContent(construirPopup(zonas[0]))
                .openOn(mapaLeaflet);
        } catch (error) {
            console.error('Error al filtrar por zona geográfica:', error);
        }
    }

    return {
        inicializar,
        centrarEnLatLng,
        cargarZonas,
        consultarZonaEnPunto
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    mapa.inicializar();
    mapa.cargarZonas();
});

window.mapa = mapa;