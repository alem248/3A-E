package com.tecsup.clima.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecsup.clima.domain.WeatherData;
import com.tecsup.clima.service.WeatherCacheService;
import com.tecsup.clima.service.WeatherService;

@Component
public class AutoRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoRefreshScheduler.class);

    private static final long QUINCE_MINUTOS_MILISEGUNDOS = 15 * 60 * 1000L;

    private final WeatherService weatherService;
    private final WeatherCacheService weatherCacheService;

    public AutoRefreshScheduler(WeatherService weatherService, WeatherCacheService weatherCacheService) {
        this.weatherService = weatherService;
        this.weatherCacheService = weatherCacheService;
    }

    @Scheduled(fixedDelay = QUINCE_MINUTOS_MILISEGUNDOS, initialDelay = 0)
    public void refrescarDatosDelClima() {
        try {
            WeatherData data = weatherService.fetchWeather();
            weatherCacheService.update(data);
            log.info("Datos del clima refrescados: {} grados C en ({}, {})",
                data.getTemperature(), data.getLatitude(), data.getLongitude());
        } catch (Exception error) {
            log.error("Error al refrescar los datos del clima", error);
        }
    }
}