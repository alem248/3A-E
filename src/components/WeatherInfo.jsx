const WeatherInfo = ({ latitude, longitude, temperature, lastUpdated }) => {
  return (
    <div style={{ padding: '1rem' }}>
      <h2>Datos del clima</h2>
      <p>Latitud: {latitude.toFixed(4)}°</p>
      <p>Longitud: {longitude.toFixed(4)}°</p>
      <p>Temperatura: {temperature !== null ? `${temperature}°C` : 'Cargando...'}</p>
      <p>Última actualización: {lastUpdated ?? '—'}</p>
    </div>
  )
}

export default WeatherInfo