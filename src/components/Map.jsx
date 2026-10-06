import { useEffect, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { formatearTsm } from '../domain/nivelRiesgo'

const UMBRAL_VERDE = 24

const escapar = (texto) =>
  String(texto).replace(
    /[&<>"']/g,
    (caracter) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[caracter]
  )

const calcularIntensidad = (nivel, tsm) => {
  if (typeof tsm !== 'number' || !Number.isFinite(tsm)) {
    return 0
  }

  const { umbralInferior, umbralSuperior } = nivel
  let porcentaje = 100

  if (umbralInferior === null && umbralSuperior !== null) {
    porcentaje = (tsm / UMBRAL_VERDE) * 100
  } else if (umbralInferior !== null && umbralSuperior !== null) {
    porcentaje = ((tsm - umbralInferior) / (umbralSuperior - umbralInferior)) * 100
  }

  return Math.min(100, Math.max(0, porcentaje))
}

const construirPopup = (zona) => {
  const intensidad = calcularIntensidad(zona.nivel, zona.tsm)

  return (
    `<div style="font-family:system-ui,sans-serif;font-size:13px;min-width:190px">` +
    `<div style="font-size:15px;font-weight:600;color:#08060d">${escapar(zona.nombre)}</div>` +
    `<div style="color:#6b6375;margin-bottom:6px">${escapar(zona.departamento)} · Costa norte</div>` +
    `<div>TSM: <strong>${escapar(formatearTsm(zona.tsm))}</strong></div>` +
    `<div style="display:flex;align-items:center;gap:6px;margin-top:6px">` +
    `<span style="display:inline-block;width:12px;height:12px;border-radius:3px;background:${zona.nivel.color}"></span>` +
    `<strong style="color:${zona.nivel.color}">${escapar(zona.nivel.nombre)}</strong>` +
    `</div>` +
    `<div style="color:#6b6375">${escapar(zona.nivel.descripcion)}</div>` +
    `<div style="margin-top:8px;height:7px;border-radius:4px;background:rgba(128,128,128,0.25);overflow:hidden">` +
    `<div style="width:${intensidad.toFixed(0)}%;height:100%;background:${zona.nivel.color}"></div>` +
    `</div>` +
    `<div style="color:#6b6375;font-size:11px;margin-top:3px">Intensidad dentro del umbral: ${intensidad.toFixed(0)}%</div>` +
    `</div>`
  )
}

const Map = ({ latitude, longitude, zoom = 13, zonas = [] }) => {
  const mapRef = useRef(null)
  const markerRef = useRef(null)
  const capaRiesgoRef = useRef(null)
  const vistaAjustadaRef = useRef(false)
  const [mapaListo, setMapaListo] = useState(0)

  useEffect(() => {
    if (mapRef.current === null) {
      mapRef.current = L.map('map').setView([latitude, longitude], zoom)

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(mapRef.current)

      const markerIcon = new L.Icon({
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2px.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
      })

      markerRef.current = L.marker([latitude, longitude], { icon: markerIcon }).addTo(mapRef.current)
      setMapaListo((valor) => valor + 1)
    } else {
      mapRef.current.setView([latitude, longitude], zoom)
      if (markerRef.current) {
        markerRef.current.setLatLng([latitude, longitude])
      }
    }

    return () => {
      if (mapRef.current !== null) {
        mapRef.current.remove()
        mapRef.current = null
        markerRef.current = null
        capaRiesgoRef.current = null
        vistaAjustadaRef.current = false
      }
    }
  }, [latitude, longitude, zoom])

  useEffect(() => {
    const mapa = mapRef.current

    if (mapa === null || zonas.length === 0) {
      return undefined
    }

    if (capaRiesgoRef.current === null) {
      capaRiesgoRef.current = L.featureGroup()
    }

    const capa = capaRiesgoRef.current
    capa.clearLayers()

    zonas.forEach((zona) => {
      const poligono = L.polygon(zona.poligono, {
        color: zona.nivel.color,
        weight: 2,
        opacity: 0.95,
        fillColor: zona.nivel.color,
        fillOpacity: 0.45
      })

      poligono.bindPopup(construirPopup(zona))
      poligono.addTo(capa)
    })

    if (vistaAjustadaRef.current === false) {
      mapa.addLayer(capa)
      const limites = capa.getBounds()

      if (limites.isValid()) {
        mapa.fitBounds(limites, { padding: [20, 20] })
      }

      vistaAjustadaRef.current = true
    } else if (mapa.hasLayer(capa) === false) {
      mapa.addLayer(capa)
    }

    return undefined
  }, [mapaListo, zonas])

  return <div id="map" style={{ height: '400px', width: '100%' }} />
}

export default Map