export const NIVEL_RIESGO = Object.freeze({
  VERDE: Object.freeze({
    id: 'VERDE',
    nombre: 'Verde',
    color: '#22C55E',
    descripcion: 'Condiciones normales',
    umbralInferior: null,
    umbralSuperior: 24.0
  }),
  AMARILLO: Object.freeze({
    id: 'AMARILLO',
    nombre: 'Amarillo',
    color: '#EAB308',
    descripcion: 'Vigilancia / Moderado',
    umbralInferior: 24.0,
    umbralSuperior: 26.0
  }),
  NARANJA: Object.freeze({
    id: 'NARANJA',
    nombre: 'Naranja',
    color: '#F97316',
    descripcion: 'Riesgo Alto',
    umbralInferior: 26.0,
    umbralSuperior: 28.0
  }),
  ROJO: Object.freeze({
    id: 'ROJO',
    nombre: 'Rojo',
    color: '#EF4444',
    descripcion: 'Peligro Critico / Alerta Roja',
    umbralInferior: 28.0,
    umbralSuperior: null
  })
})

export const NIVEL_SIN_DATOS = Object.freeze({
  id: 'SIN_DATOS',
  nombre: 'Sin datos',
  color: '#94A3B8',
  descripcion: 'Sin lectura de TSM disponible',
  umbralInferior: null,
  umbralSuperior: null
})

export const NIVELES_ORDENADOS = Object.freeze([
  NIVEL_RIESGO.VERDE,
  NIVEL_RIESGO.AMARILLO,
  NIVEL_RIESGO.NARANJA,
  NIVEL_RIESGO.ROJO
])

export function clasificarTemperatura(tsmCelsius) {
  if (typeof tsmCelsius !== 'number' || !Number.isFinite(tsmCelsius)) {
    return NIVEL_SIN_DATOS
  }

  const nivel = NIVELES_ORDENADOS.find(
    (item) =>
      (item.umbralInferior === null || tsmCelsius >= item.umbralInferior) &&
      (item.umbralSuperior === null || tsmCelsius < item.umbralSuperior)
  )

  return nivel ?? NIVEL_SIN_DATOS
}

export function formatearRango(nivel) {
  const { umbralInferior, umbralSuperior } = nivel

  if (umbralInferior !== null && umbralSuperior === null) {
    return `>= ${umbralInferior.toFixed(1)} C`
  }

  if (umbralInferior === null && umbralSuperior !== null) {
    return `< ${umbralSuperior.toFixed(1)} C`
  }

  if (umbralInferior === null && umbralSuperior === null) {
    return 'sin umbral'
  }

  return `${umbralInferior.toFixed(1)} - ${(umbralSuperior - 0.1).toFixed(1)} C`
}

export function formatearTsm(tsmCelsius) {
  if (typeof tsmCelsius !== 'number' || !Number.isFinite(tsmCelsius)) {
    return 'sin dato'
  }

  return `${tsmCelsius.toFixed(1)} C`
}

export function contarZonasPorNivel(zonas) {
  const conteo = new Map()

  NIVELES_ORDENADOS.forEach((nivel) => conteo.set(nivel.id, 0))
  conteo.set(NIVEL_SIN_DATOS.id, 0)

  zonas.forEach((zona) => {
    conteo.set(zona.nivel.id, (conteo.get(zona.nivel.id) ?? 0) + 1)
  })

  return conteo
}

export function obtenerNivelMasCritico(zonas) {
  return zonas.reduce((peor, zona) => {
    if (peor === null) {
      return zona.nivel
    }

    return zona.nivel.umbralInferior !== null &&
      (peor.umbralInferior === null || zona.nivel.umbralInferior > peor.umbralInferior)
      ? zona.nivel
      : peor
  }, null)
}