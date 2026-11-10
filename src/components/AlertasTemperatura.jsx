import { useState } from 'react'
import { formatearRango, formatearTsm } from '../domain/nivelRiesgo'
import { contarAlertasPorNivel, obtenerAlertasActivas } from '../domain/alertasTermicas'

const formatearHora = (iso) => {
  if (!iso) return 'Hora no disponible'

  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return 'Hora no disponible'

  return fecha.toLocaleString('es-PE', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const AlertasTemperatura = ({ zonas = [] }) => {
  const [nivelSeleccionado, setNivelSeleccionado] = useState('TODAS')
  const alertas = obtenerAlertasActivas(zonas)
  const alertasVisibles = nivelSeleccionado === 'TODAS'
    ? alertas
    : alertas.filter((alerta) => alerta.nivel.id === nivelSeleccionado)
  const conteos = contarAlertasPorNivel(alertas)
  const sinDatos = zonas.filter((zona) => !zona.disponible)

  return (
    <section className="alertas" aria-labelledby="alertas-titulo" aria-live="polite">
      <div className="alertas__encabezado">
        <div>
          <p className="alertas__sobrelinea">Monitoreo de temperatura superficial del mar</p>
          <h2 id="alertas-titulo">Alertas térmicas</h2>
          <p className="alertas__descripcion">
            Temperatura observada y nivel de riesgo por zona costera.
          </p>
        </div>
        <span className={`alertas__contador${alertas.length ? ' alertas__contador--activo' : ''}`}>
          {alertas.length} {alertas.length === 1 ? 'alerta' : 'alertas'}
        </span>
      </div>

      {alertas.length > 0 && (
        <label className="alertas__filtro">
          Filtrar por nivel
          <select value={nivelSeleccionado} onChange={(event) => setNivelSeleccionado(event.target.value)}>
            <option value="TODAS">Todos los niveles</option>
            {[...conteos].map(([id]) => (
              <option key={id} value={id}>{alertas.find((alerta) => alerta.nivel.id === id).nivel.nombre}</option>
            ))}
          </select>
        </label>
      )}

      {alertas.length > 0 && (
        <ul className="alertas__resumen" aria-label="Cantidad de alertas por nivel">
          {[...conteos].map(([id, cantidad]) => {
            const nivel = alertas.find((alerta) => alerta.nivel.id === id)?.nivel
            return (
              <li key={id}>
                <span style={{ '--nivel-color': nivel.color }}>{nivel.nombre}</span>
                <strong>{cantidad}</strong>
              </li>
            )
          })}
        </ul>
      )}

      {alertas.length === 0 ? (
        <div className="alertas__vacio" role="status">
          <span className="alertas__vacio-icono" aria-hidden="true">✓</span>
          <div>
            <strong>{zonas.length === 0 ? 'Cargando datos térmicos' : sinDatos.length === zonas.length ? 'Sin lecturas térmicas disponibles' : 'Sin alertas térmicas activas'}</strong>
            <p>{zonas.length === 0 ? 'Las lecturas aparecerán aquí cuando estén disponibles.' : sinDatos.length === zonas.length ? 'No se puede determinar el riesgo hasta recibir una lectura válida.' : 'Las zonas con lectura se encuentran por debajo del umbral de vigilancia.'}</p>
          </div>
        </div>
      ) : (
        <ul className="alertas__lista">
          {alertasVisibles.map((zona) => (
            <li className="alerta" key={zona.id} style={{ '--alerta-color': zona.nivel.color }}>
              <span className="alerta__indicador" aria-hidden="true" />
              <div className="alerta__zona">
                <strong>{zona.nombre}</strong>
                <span>{zona.departamento}</span>
              </div>
              <div className="alerta__temperatura">
                <strong>{formatearTsm(zona.tsm).replace(' C', ' °C')}</strong>
                <span>Temperatura actual</span>
              </div>
              <div className="alerta__riesgo">
                <span className="alerta__nivel">{zona.nivel.nombre}</span>
                <span>{zona.nivel.descripcion}</span>
                <span className="alerta__umbral">Umbral: {formatearRango(zona.nivel).replace(' C', ' °C')}</span>
              </div>
              <time className="alerta__hora" dateTime={zona.actualizadoEn ?? undefined}>
                {formatearHora(zona.actualizadoEn)}
              </time>
            </li>
          ))}
        </ul>
      )}

      {alertas.length > 0 && alertasVisibles.length === 0 && (
        <p className="alertas__sin-coincidencias" role="status">No hay alertas para el nivel seleccionado.</p>
      )}

      {sinDatos.length > 0 && (
        <p className="alertas__sin-datos" role="status">
          Sin lectura reciente: {sinDatos.map((zona) => zona.nombre).join(', ')}.
        </p>
      )}
      <p className="alertas__nota">Umbrales TSM: vigilancia desde 24 °C · alto desde 26 °C · crítico desde 28 °C.</p>
    </section>
  )
}

export default AlertasTemperatura
