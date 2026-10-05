const WeatherInfo = ({ latitude, longitude, temperature }) => {
  return (
    <div style={{ padding: '1rem' }}>
      <h2>Datos del clima</h2>
      <p>Latitud: {latitude.toFixed(4)}°</p>
      <p>Longitud: {longitude.toFixed(4)}°</p>
      <p>Temperatura: {temperature !== null ? `${temperature}°C` : 'Cargando...'}</p>
    </div>
  )
}

export default WeatherInfo
