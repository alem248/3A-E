import {
  NIVELES_ORDENADOS,
  NIVEL_SIN_DATOS,
  contarZonasPorNivel,
  formatearRango,
  obtenerNivelMasCritico
} from '../domain/nivelRiesgo'

const formatearHora = (iso) => {
  if (typeof iso !== 'string' || iso.length === 0) {
    return 'sin dato'
  }

  const fecha = new Date(iso)

  if (Number.isNaN(fecha.getTime())) {
    return 'sin dato'
  }

  return fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })
}

const LeyendaRiesgo = ({ zonas = [] }) => {
  const conteo = contarZonasPorNivel(zonas)
  const nivelCritico = obtenerNivelMasCritico(zonas)
  const conLectura = zonas.filter((zona) => zona.disponible)
  const sinLectura = conteo.get(NIVEL_SIN_DATOS.id) ?? 0

  return (
    <aside className="leyenda-riesgo" aria-label="Leyenda de semaforo termico TSM">
      <h2 className="leyenda-riesgo__titulo">Semaforo termico TSM</h2>

      {nivelCritico !== null && (
        <div className="leyenda-riesgo__resumen">
          <span>Nivel mas alto</span>
          <strong style={{ color: nivelCritico.color }}>{nivelCritico.nombre}</strong>
        </div>
      )}

      <ul className="leyenda-riesgo__lista">
        {NIVELES_ORDENADOS.map((nivel) => (
          <li key={nivel.id} className="leyenda-riesgo__item">
            <span
              className="leyenda-riesgo__color"
              style={{ background: nivel.color }}
              aria-hidden="true"
            />
            <span className="leyenda-riesgo__nombre">{nivel.nombre}</span>
            <span className="leyenda-riesgo__rango">{formatearRango(nivel)}</span>
            <span className="leyenda-riesgo__conteo">{conteo.get(nivel.id) ?? 0}</span>
          </li>
        ))}

        {sinLectura > 0 && (
          <li className="leyenda-riesgo__item">
            <span
              className="leyenda-riesgo__color"
              style={{ background: NIVEL_SIN_DATOS.color }}
              aria-hidden="true"
            />
            <span className="leyenda-riesgo__nombre">{NIVEL_SIN_DATOS.nombre}</span>
            <span className="leyenda-riesgo__rango">-</span>
            <span className="leyenda-riesgo__conteo">{sinLectura}</span>
          </li>
        )}
      </ul>

      <p className="leyenda-riesgo__pie">
        {conLectura.length}/{zonas.length} zonas con lectura · {formatearHora(zonas[0]?.actualizadoEn)}
      </p>
    </aside>
  )
}

export default LeyendaRiesgo