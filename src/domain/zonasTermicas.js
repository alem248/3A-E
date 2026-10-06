import { clasificarTemperatura, NIVEL_SIN_DATOS } from './nivelRiesgo'

const API_MARINA_URL = 'https://marine-api.open-meteo.com/v1/marine'

export const ZONAS_TERMICAS = Object.freeze([
  Object.freeze({
    id: 'tumbes',
    nombre: 'Tumbes',
    departamento: 'Tumbes',
    puntoReferencia: Object.freeze({ latitude: -3.62, longitude: -81.0 }),
    poligono: Object.freeze([
      Object.freeze([-3.27, -80.75]),
      Object.freeze([-3.27, -81.25]),
      Object.freeze([-3.97, -81.25]),
      Object.freeze([-3.97, -80.75])
    ])
  }),
  Object.freeze({
    id: 'piura-norte',
    nombre: 'Piura Norte',
    departamento: 'Piura',
    puntoReferencia: Object.freeze({ latitude: -4.35, longitude: -81.35 }),
    poligono: Object.freeze([
      Object.freeze([-4.0, -81.1]),
      Object.freeze([-4.0, -81.6]),
      Object.freeze([-4.7, -81.6]),
      Object.freeze([-4.7, -81.1])
    ])
  }),
  Object.freeze({
    id: 'piura-sur',
    nombre: 'Piura Sur (Paita)',
    departamento: 'Piura',
    puntoReferencia: Object.freeze({ latitude: -5.25, longitude: -81.6 }),
    poligono: Object.freeze([
      Object.freeze([-4.9, -81.35]),
      Object.freeze([-4.9, -81.85]),
      Object.freeze([-5.6, -81.85]),
      Object.freeze([-5.6, -81.35])
    ])
  }),
  Object.freeze({
    id: 'lambayeque',
    nombre: 'Lambayeque',
    departamento: 'Lambayeque',
    puntoReferencia: Object.freeze({ latitude: -6.6, longitude: -80.6 }),
    poligono: Object.freeze([
      Object.freeze([-6.25, -80.35]),
      Object.freeze([-6.25, -80.85]),
      Object.freeze([-6.95, -80.85]),
      Object.freeze([-6.95, -80.35])
    ])
  }),
  Object.freeze({
    id: 'la-libertad-norte',
    nombre: 'La Libertad Norte',
    departamento: 'La Libertad',
    puntoReferencia: Object.freeze({ latitude: -7.35, longitude: -79.95 }),
    poligono: Object.freeze([
      Object.freeze([-7.0, -79.7]),
      Object.freeze([-7.0, -80.2]),
      Object.freeze([-7.7, -80.2]),
      Object.freeze([-7.7, -79.7])
    ])
  }),
  Object.freeze({
    id: 'la-libertad-sur',
    nombre: 'La Libertad Sur (Chimbote)',
    departamento: 'La Libertad',
    puntoReferencia: Object.freeze({ latitude: -9.1, longitude: -79.1 }),
    poligono: Object.freeze([
      Object.freeze([-8.75, -78.85]),
      Object.freeze([-8.75, -79.35]),
      Object.freeze([-9.45, -79.35]),
      Object.freeze([-9.45, -78.85])
    ])
  })
])

export async function consultarTsmPorZonas(zonas = ZONAS_TERMICAS) {
  const resultados = await Promise.allSettled(
    zonas.map(async (zona) => {
      const { latitude, longitude } = zona.puntoReferencia
      const url = `${API_MARINA_URL}?latitude=${latitude}&longitude=${longitude}&current=sea_surface_temperature`

      const respuesta = await fetch(url)

      if (!respuesta.ok) {
        throw new Error(`HTTP ${respuesta.status} al consultar ${zona.nombre}`)
      }

      const datos = await respuesta.json()

      return {
        tsm: datos?.current?.sea_surface_temperature ?? null,
        actualizadoEn: datos?.current?.time ?? null
      }
    })
  )

  return resultados.map((resultado, indice) => {
    const zona = zonas[indice]

    if (resultado.status === 'rejected') {
      return {
        ...zona,
        tsm: null,
        actualizadoEn: null,
        nivel: NIVEL_SIN_DATOS,
        disponible: false
      }
    }

    const { tsm, actualizadoEn } = resultado.value

    return {
      ...zona,
      tsm,
      actualizadoEn,
      nivel: clasificarTemperatura(tsm),
      disponible: tsm !== null
    }
  })
}