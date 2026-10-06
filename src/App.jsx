import { useEffect, useState, useRef } from 'react'
import Map from './components/Map'
import WeatherInfo from './components/WeatherInfo'
import LeyendaRiesgo from './components/LeyendaRiesgo'
import { useAutoRefresh } from './hooks/useAutoRefresh'
import { consultarTsmPorZonas } from './domain/zonasTermicas'
import './App.css'

const API_URL = 'https://api.open-meteo.com/v1/forecast'
const INTERVALO_REFRESCO_MS = 15 * 60 * 1000

const App = () => {
  const [weather, setWeather] = useState({
    latitude: -4.35,
    longitude: -81.35,
    temperature: null
  })
  const [zonas, setZonas] = useState([])
  const [lastUpdated, setLastUpdated] = useState(null)
  const zonasTimerRef = useRef(null)

  const fetchWeather = async () => {
    try {
      const res = await fetch(`${API_URL}?latitude=${weather.latitude}&longitude=${weather.longitude}&current=temperature_2m`)
      const data = await res.json()
      setWeather(prev => ({
        ...prev,
        latitude: data.latitude,
        longitude: data.longitude,
        temperature: data.current?.temperature_2m ?? null
      }))
      setLastUpdated(new Date().toLocaleTimeString())
    } catch (error) {
      console.error('Error al obtener datos:', error)
    }
  }

  useAutoRefresh(fetchWeather, INTERVALO_REFRESCO_MS)

  useEffect(() => {
    fetchWeather()
  }, [])

  useEffect(() => {
    let vigente = true

    const cargarZonas = async () => {
      const resultado = await consultarTsmPorZonas()

      if (vigente) {
        setZonas(resultado)
      }
    }

    cargarZonas()
    zonasTimerRef.current = setInterval(cargarZonas, INTERVALO_REFRESCO_MS)

    return () => {
      vigente = false

      if (zonasTimerRef.current) {
        clearInterval(zonasTimerRef.current)
        zonasTimerRef.current = null
      }
    }
  }, [])

  return (
    <div className="app">
      <h1>Clima Zona Costera</h1>
      <WeatherInfo latitude={weather.latitude} longitude={weather.longitude} temperature={weather.temperature} lastUpdated={lastUpdated} />
      <div className="mapa-contenedor">
        <Map latitude={weather.latitude} longitude={weather.longitude} zoom={7} zonas={zonas} />
        <LeyendaRiesgo zonas={zonas} />
      </div>
    </div>
  )
}

export default App