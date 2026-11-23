const mapa = (() => {
    let mapaLeaflet = null;
    let grupoFavoritas = null;
    let favoritasActuales = [];
    let marcador = null;
    let capaRiesgo = null;
    let vistaAjustada = false;
    let capaActiva = null;

    // Caja geográfica de Perú (extremos nacionales, con margen visual).
    const limitesPeru = L.latLngBounds([-18.45, -81.45], [-0.02, -68.55]);
    const centroPeru = [-9.19, -75.02];
    const opcionesTiles = { maxZoom: 19, attribution: '&copy; OpenStreetMap contributors' };
    const capasBase = {
        'Normal (calles)': L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', opcionesTiles),
        'Satelital': L.tileLayer(
            'https://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',
            { subdomains: ['mt0', 'mt1', 'mt2', 'mt3'], maxZoom: 20, attribution: 'Imágenes &copy; Google' }
        )
    };

    // Regiones naturales aproximadas. Se dibujan antes del contorno de Perú,
    // así lo que cae fuera del país queda oscurecido por la máscara.
    const regionesPeru = [
        {
            clave: 'costa', nombre: 'Costa', color: '#f97316',
            poligono: [[-3.0, -82.0], [-3.4, -79.6], [-6.0, -79.0], [-8.5, -78.3], [-10.5, -77.5], [-13.0, -76.2], [-15.5, -74.9], [-18.5, -73.4], [-18.5, -82.0]],
            centro: [-10.0, -78.4], temperatura: '18–24 °C', humedad: '75–80%'
        },
        {
            clave: 'sierra', nombre: 'Sierra', color: '#7dd3fc',
            poligono: [[-3.4, -79.6], [-3.5, -77.5], [-6.0, -76.5], [-9.0, -75.2], [-12.5, -73.5], [-15.5, -72.0], [-18.5, -70.3], [-18.5, -73.4], [-15.5, -74.9], [-13.0, -76.2], [-10.5, -77.5], [-8.5, -78.3], [-6.0, -79.0]],
            centro: [-10.5, -76.0], temperatura: '7–15 °C', humedad: '50–65%'
        },
        {
            clave: 'selva', nombre: 'Selva', color: '#ef4444',
            poligono: [[-3.5, -77.5], [-3.5, -69.0], [-18.5, -69.0], [-18.5, -70.3], [-15.5, -72.0], [-12.5, -73.5], [-9.0, -75.2], [-6.0, -76.5]],
            centro: [-8.0, -71.5], temperatura: '30–34 °C', humedad: '85–95%'
        }
    ];
    const etiquetasRegion = {};
    const porcentajesRegion = {};

    // Punto dentro de un polígono [[lat, lng], ...] (algoritmo de rayos)
    function dentroDePoligono(lat, lng, anillo) {
        let dentro = false;
        for (let i = 0, j = anillo.length - 1; i < anillo.length; j = i++) {
            const [latI, lngI] = anillo[i];
            const [latJ, lngJ] = anillo[j];
            const cruza = (latI > lat) !== (latJ > lat)
                && lng < ((lngJ - lngI) * (lat - latI)) / (latJ - latI) + lngI;
            if (cruza) dentro = !dentro;
        }
        return dentro;
    }

    function htmlEtiquetaRegion(region) {
        return `<div class="region-etiqueta" style="--color-region:${region.color}">` +
            `<span class="region-nombre">${region.nombre}</span>` +
            `<span>${region.temperatura} · Humedad ${region.humedad}</span></div>`;
    }

    function dibujarRegiones() {
        regionesPeru.forEach((region) => {
            L.polygon(region.poligono, {
                color: region.color,
                weight: 1,
                fillColor: region.color,
                fillOpacity: 0.3,
                interactive: false
            }).addTo(mapaLeaflet);

            etiquetasRegion[region.clave] = L.marker(region.centro, {
                interactive: false,
                icon: L.divIcon({
                    className: 'region-icono',
                    html: htmlEtiquetaRegion(region, null),
                    iconSize: null
                })
            }).addTo(mapaLeaflet);
        });
    }

    // Porcentaje de intensidad del fenómeno por región: promedio de las zonas cuyo centro cae en ella.
    function actualizarRegiones(zonas) {
        regionesPeru.forEach((region) => {
            const intensidades = zonas
                .filter((zona) => {
                    const puntos = zona.poligono;
                    const latCentro = puntos.reduce((suma, p) => suma + p[0], 0) / puntos.length;
                    const lngCentro = puntos.reduce((suma, p) => suma + p[1], 0) / puntos.length;
                    return dentroDePoligono(latCentro, lngCentro, region.poligono);
                })
                .map((zona) => calcularIntensidad(zona.nivel, zona.tsm));
            const porcentaje = intensidades.length === 0
                ? null
                : Math.round(intensidades.reduce((a, b) => a + b, 0) / intensidades.length);
            porcentajesRegion[region.clave] = porcentaje;
            const marcador = etiquetasRegion[region.clave];
            if (marcador) {
                marcador.setIcon(L.divIcon({
                    className: 'region-icono',
                    html: htmlEtiquetaRegion(region, porcentaje),
                    iconSize: null
                }));
            }
        });
    }

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

    // Region natural de cada departamento: Costa, Sierra o Selva.
    const regionNatural = {
        'Tumbes': 'Costa', 'Piura': 'Costa', 'Lambayeque': 'Costa', 'La Libertad': 'Costa',
        'Áncash': 'Costa', 'Lima': 'Costa', 'Callao': 'Costa', 'Ica': 'Costa',
        'Arequipa': 'Costa', 'Moquegua': 'Costa', 'Tacna': 'Costa',
        'Cajamarca': 'Sierra', 'Huancavelica': 'Sierra', 'Ayacucho': 'Sierra', 'Apurímac': 'Sierra',
        'Cusco': 'Sierra', 'Puno': 'Sierra', 'Junín': 'Sierra', 'Pasco': 'Sierra', 'Huánuco': 'Sierra',
        'Loreto': 'Selva', 'Ucayali': 'Selva', 'Madre de Dios': 'Selva', 'San Martín': 'Selva',
        'Amazonas': 'Selva'
    };

    // Explica que mide el nivel de cada zona: mar en la costa, calor y lluvias en el interior.
    function etiquetaRiesgo(departamento) {
        return regionNatural[departamento] === 'Costa'
            ? 'Riesgo por temperatura del mar'
            : 'Riesgo por calor y lluvias';
    }

    function construirPopup(zona) {
        const intensidad = calcularIntensidad(zona.nivel, zona.tsm);

        return (
            `<div style="font-family:system-ui,sans-serif;font-size:13px;min-width:190px">` +
            `<div style="font-size:15px;font-weight:600">${escapar(zona.nombre)}</div>` +
            `<div style="color:#6b6375;margin-top:2px">${escapar(regionNatural[zona.departamento] || '')}</div>` +
            `<div style="color:#6b6375;margin-top:6px">${escapar(etiquetaRiesgo(zona.departamento))}</div>` +
            `<div style="display:flex;align-items:center;gap:6px;margin-top:4px">` +
            `<span style="display:inline-block;width:12px;height:12px;border-radius:3px;background:${zona.nivel.color}"></span>` +
            `<strong style="color:${zona.nivel.color}">${escapar(zona.nivel.nombre)}</strong>` +
            `</div>` +
            `<div style="color:#6b6375">${escapar(zona.nivel.descripcion)}</div>` +
            `<div style="color:#6b6375;margin-top:6px">TSM: <strong>${escapar(zona.tsm)}</strong></div>` +
            `</div>`
        );
    }

    // Leyenda fija con los tres niveles del diseño (colores de NivelRiesgo en el servidor).
    const nivelesLeyenda = [
        { color: '#EF4444', texto: 'Riesgo Crítico / Alerta Roja' },
        { color: '#F97316', texto: 'Riesgo Alto' },
        { color: '#EAB308', texto: 'Riesgo Medio' },
        { color: '#22C55E', texto: 'Riesgo Bajo' }
    ];

    function renderizarLeyenda() {
        const leyenda = document.getElementById('leyenda');
        if (!leyenda) {
            return;
        }
        const items = nivelesLeyenda
            .map((nivel) =>
                `<li><span class="muestra-color" style="background:${nivel.color}"></span>` +
                `<span>${nivel.texto}</span></li>`
            )
            .join('');
        const regiones = regionesPeru
            .map((region) =>
                `<li><span class="muestra-color" style="background:${region.color}"></span>` +
                `<span>${region.nombre}: ${region.temperatura}</span></li>`
            )
            .join('');
        leyenda.innerHTML = `<h3>Nivel de riesgo térmico</h3><ul>${items}</ul>` +
            `<h3 class="leyenda-seccion">Regiones naturales</h3><ul>${regiones}</ul>`;
        const contenedorNino = document.createElement('div');
        contenedorNino.id = 'leyenda-nino';
        leyenda.appendChild(contenedorNino);
        cargarIndiceNino(contenedorNino);
        leyenda.hidden = false;
    }

    function cargarIndiceNino(contenedor) {
        fetch('/api/nino')
            .then((respuesta) => (respuesta.ok ? respuesta.json() : null))
            .then((datos) => {
                if (!datos) {
                    contenedor.innerHTML = '<h3 class="leyenda-seccion">Fenómeno El Niño</h3><p>Índice no disponible.</p>';
                    return;
                }
                const intensidad = datos.intensidad ? ` ${datos.intensidad}` : '';
                const explicacion = datos.clasificacion === 'Neutral'
                    ? 'Sin El Niño ni La Niña: el Pacífico ecuatorial está en condiciones normales.'
                    : `El Pacífico ecuatorial está ${datos.clasificacion === 'El Niño' ? 'más cálido' : 'más frío'} de lo normal.`;
                contenedor.innerHTML =
                    '<h3 class="leyenda-seccion">Fenómeno El Niño</h3>' +
                    `<p><strong>${escapar(datos.clasificacion + intensidad)}</strong> · índice ${datos.valor.toFixed(1)} (${escapar(datos.periodo)})</p>` +
                    `<p>${explicacion}</p>` +
                    '<p>Mide el calor del Pacífico ecuatorial, no el riesgo de una zona. Fuente: NOAA.</p>';
            })
            .catch(() => {});
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
                mapa.fitBounds(limitesPeru);
            }

            vistaAjustada = true;
        } else if (mapa.hasLayer(capaRiesgo) === false) {
            mapa.addLayer(capaRiesgo);
        }

        renderizarLeyenda(zonas);
        actualizarRegiones(zonas);
    }

    // Cambia entre mapa de calles y satelital desde los botones de la cabecera.
    function cambiarCapa(nombre) {
        const nueva = capasBase[nombre];
        if (mapaLeaflet === null || !nueva || nueva === capaActiva) {
            return;
        }
        if (capaActiva !== null) {
            mapaLeaflet.removeLayer(capaActiva);
        }
        nueva.addTo(mapaLeaflet);
        capaActiva = nueva;
        document.querySelectorAll('.mapa-tipo-btn').forEach((boton) => {
            const activo = boton.dataset.capa === nombre;
            boton.classList.toggle('activo', activo);
            boton.setAttribute('aria-pressed', String(activo));
        });
    }

    function vincularControlesMapa() {
        document.querySelectorAll('.mapa-tipo-btn').forEach((boton) => {
            boton.addEventListener('click', () => cambiarCapa(boton.dataset.capa));
        });
        const botonCentrar = document.getElementById('mapa-localizar');
        if (botonCentrar) {
            botonCentrar.addEventListener('click', () => mapaLeaflet.fitBounds(limitesPeru));
        }
    }

    // Contorno de Perú: se dibuja el borde y se oscurece todo lo que queda fuera.
    async function dibujarContornoPeru() {
        try {
            const respuesta = await fetch('/data/peru.geojson');
            if (!respuesta.ok) {
                return;
            }
            const coleccion = await respuesta.json();
            const pais = coleccion.features[0];
            const poligonos = pais.geometry.type === 'Polygon'
                ? [pais.geometry.coordinates]
                : pais.geometry.coordinates;
            // GeoJSON usa [longitud, latitud]; Leaflet usa [latitud, longitud].
            const aLatLng = (anillo) => anillo.map(([lng, lat]) => [lat, lng]);
            const agujeros = poligonos.flatMap((poligono) => poligono.map(aLatLng));
            const mundo = [[-90, -180], [-90, 180], [90, 180], [90, -180]];

            L.polygon([mundo, ...agujeros], {
                stroke: false,
                fillColor: '#06101f',
                fillOpacity: 0.72,
                interactive: false
            }).addTo(mapaLeaflet);
            L.geoJSON(pais, {
                style: { color: '#60a5fa', weight: 2, fill: false },
                interactive: false
            }).addTo(mapaLeaflet);
        } catch (error) {
            console.error('No se pudo dibujar el contorno de Perú:', error);
        }
    }

    function inicializar() {
        const contenedor = document.getElementById('mapa');
        if (mapaLeaflet === null && contenedor) {
            mapaLeaflet = L.map('mapa', {
                doubleClickZoom: false,
                maxBounds: limitesPeru,
                maxBoundsViscosity: 1.0,
                minZoom: 5
            });
            // Zoom mínimo: encaja todo Perú en la vista; lo que queda fuera se oscurece.
            const zoomMinimo = mapaLeaflet.getBoundsZoom(limitesPeru);
            mapaLeaflet.setMinZoom(zoomMinimo);
            mapaLeaflet.fitBounds(limitesPeru);
            dibujarRegiones();
            dibujarContornoPeru();
            capaActiva = capasBase['Normal (calles)'];
            capaActiva.addTo(mapaLeaflet);
            vincularControlesMapa();
            renderizarLeyenda();

            marcador = L.marker([-4.35, -81.35], { icon: icono }).addTo(mapaLeaflet);

            // Un clic muestra la zona; el doble clic tambien ofrece agregar un favorito.
            let temporizadorClic = null;
            mapaLeaflet.on('click', (evento) => {
                clearTimeout(temporizadorClic);
                temporizadorClic = setTimeout(() => {
                    consultarZonaEnPunto(evento.latlng.lat, evento.latlng.lng, false);
                }, 250);
            });
            mapaLeaflet.on('dblclick', (evento) => {
                clearTimeout(temporizadorClic);
                consultarZonaEnPunto(evento.latlng.lat, evento.latlng.lng, true);
            });
            pintarFavoritas();
        }
    }

    function centrarEnLatLng(latitud, longitud, zoom = 7) {
        if (mapaLeaflet !== null) {
            mapaLeaflet.setView([latitud, longitud], zoom);
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
            document.dispatchEvent(new CustomEvent('clima:zonas-actualizadas', { detail: zonas }));
        } catch (error) {
            console.error('Error al obtener zonas térmicas:', error);
        }
    }

    // Filtro por zona geográfica: al hacer clic en el mapa se consultan solo las
    // zonas cuyo polígono contiene el punto seleccionado.
    // Marcadores verdes de las ubicaciones favoritas del usuario.
    function pintarFavoritas() {
        if (mapaLeaflet === null) return;
        if (grupoFavoritas === null) {
            grupoFavoritas = L.layerGroup().addTo(mapaLeaflet);
        }
        grupoFavoritas.clearLayers();
        favoritasActuales.forEach((favorita) => {
            const icono = L.divIcon({
                className: 'marcador-favorito',
                html: '<svg viewBox="0 0 24 24" width="28" height="28" aria-hidden="true">'
                    + '<path fill="#22c55e" stroke="#ffffff" stroke-width="1.5" '
                    + 'd="M12 2a7 7 0 0 0-7 7c0 5 7 13 7 13s7-8 7-13a7 7 0 0 0-7-7z"/>'
                    + '<circle cx="12" cy="9" r="2.5" fill="#ffffff"/></svg>',
                iconSize: [28, 28],
                iconAnchor: [14, 28]
            });
            const nombre = document.createElement('strong');
            nombre.textContent = favorita.etiqueta;
            L.marker([Number(favorita.latitud), Number(favorita.longitud)], {
                icon: icono,
                title: favorita.etiqueta,
                bubblingMouseEvents: false
            }).bindPopup(nombre).addTo(grupoFavoritas);
        });
    }

    window.mapaFavoritas = {
        mostrar(lista) {
            favoritasActuales = Array.isArray(lista) ? lista : [];
            pintarFavoritas();
        }
    };

    // Abre el popup de un punto y le anade las acciones que registren otros modulos (p. ej. favoritas).
    function abrirPopupPunto(latitud, longitud, contenido, conAcciones) {
        const contenedor = document.createElement('div');
        if (typeof contenido === 'string') {
            contenedor.innerHTML = contenido;
        } else {
            contenedor.appendChild(contenido);
        }
        const acciones = conAcciones ? ((window.mapa && window.mapa.acciones) || []) : [];
        acciones.forEach((accion) => {
            const nodo = accion(latitud, longitud);
            if (nodo) contenedor.appendChild(nodo);
        });
        L.popup().setLatLng([latitud, longitud]).setContent(contenedor).openOn(mapaLeaflet);
    }

    async function consultarZonaEnPunto(latitud, longitud, conAcciones = false) {
        try {
            const respuesta = await fetch(
                `/api/zonas?lat=${latitud}&lon=${longitud}`
            );
            if (!respuesta.ok) {
                return;
            }
            const zonas = await respuesta.json();

            if (zonas.length === 0) {
                abrirPopupPunto(latitud, longitud, 'Sin zona térmica registrada en este punto.', conAcciones);
                return;
            }

            abrirPopupPunto(latitud, longitud, construirPopup(zonas[0]), conAcciones);
        } catch (error) {
            console.error('Error al filtrar por zona geográfica:', error);
        }
    }

    // Región natural de un punto (o null si está fuera de las regiones)
    function regionEn(latitud, longitud) {
        return regionesPeru.find((region) => dentroDePoligono(latitud, longitud, region.poligono)) || null;
    }

    // Intensidad del fenómeno (%) de una región, o null si aún no hay zonas cargadas
    function porcentajeRegion(clave) {
        return porcentajesRegion[clave] ?? null;
    }

    return {
        inicializar,
        regionEn,
        porcentajeRegion,
        cambiarCapa,
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
mapa.acciones = [];
mapa.agregarAccionPunto = (accion) => { mapa.acciones.push(accion); };
