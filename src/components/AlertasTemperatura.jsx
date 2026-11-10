import { formatearTsm } from '../domain/nivelRiesgo'
import { obtenerAlertasActivas } from '../domain/alertasTermicas'

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
  const alertas = obtenerAlertasActivas(zonas)
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

      {alertas.length === 0 ? (
        <div className="alertas__vacio" role="status">
          <span className="alertas__vacio-icono" aria-hidden="true">✓</span>
          <div>
            <strong>{zonas.length ? 'Sin alertas térmicas activas' : 'Cargando datos térmicos'}</strong>
            <p>{zonas.length ? 'Las zonas disponibles se encuentran por debajo del umbral de vigilancia.' : 'Las lecturas aparecerán aquí cuando estén disponibles.'}</p>
          </div>
        </div>
      ) : (
        <ul className="alertas__lista">
          {alertas.map((zona) => (
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
              </div>
              <time className="alerta__hora" dateTime={zona.actualizadoEn ?? undefined}>
                {formatearHora(zona.actualizadoEn)}
              </time>
            </li>
          ))}
        </ul>
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
