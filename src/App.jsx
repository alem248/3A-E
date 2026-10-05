import { useEffect, useState, useRef } from 'react'
import Map from './components/Map'
import WeatherInfo from './components/WeatherInfo'
import './App.css'

const API_URL = 'https://api.open-meteo.com/v1/forecast'

const App = () => {
  const [weather, setWeather] = useState({
    latitude: -12.0653,
    longitude: -77.0428,
    temperature: null
  })
  const timerRef = useRef(null)

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
    } catch (error) {
      console.error('Error al obtener datos:', error)
    }
  }

  useEffect(() => {
    fetchWeather()
    timerRef.current = setInterval(fetchWeather, 15 * 60 * 1000)

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [])

  return (
    <div className="app">
      <h1>Clima Zona Costera</h1>
      <WeatherInfo latitude={weather.latitude} longitude={weather.longitude} temperature={weather.temperature} />
      <Map latitude={weather.latitude} longitude={weather.longitude} />
    </div>
  )
}

export default App
