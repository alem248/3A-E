const esAlertaActiva = (zona) => zona.disponible && zona.nivel.id !== 'VERDE'

export function obtenerAlertasActivas(zonas = []) {
  return zonas
    .filter(esAlertaActiva)
    .slice()
    .sort((a, b) => (b.nivel.umbralInferior ?? 0) - (a.nivel.umbralInferior ?? 0))
}

export function contarAlertasPorNivel(alertas = []) {
  return alertas.reduce((conteos, alerta) => {
    conteos.set(alerta.nivel.id, (conteos.get(alerta.nivel.id) ?? 0) + 1)
    return conteos
  }, new Map())
}
