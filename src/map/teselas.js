export const TESELAS_BASE = Object.freeze([
  Object.freeze({
    id: 'calles',
    nombre: 'Calles (OpenStreetMap)',
    url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    opciones: Object.freeze({
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    })
  }),
  Object.freeze({
    id: 'satelite',
    nombre: 'Satelital (Esri)',
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    opciones: Object.freeze({
      attribution: 'Tiles &copy; Esri &mdash; Source: Esri, Maxar, Earthstar Geographics',
      maxZoom: 19
    })
  })
])