package com.tecsup.clima.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.domain.WeatherData;
import com.tecsup.clima.domain.WeatherSnapshot;
import com.tecsup.clima.service.WeatherCacheService;
import com.tecsup.clima.service.WeatherService;

@RestController
@RequestMapping("/api")
public class WeatherController {

    private final WeatherService weatherService;
    private final WeatherCacheService weatherCacheService;

    public WeatherController(WeatherService weatherService, WeatherCacheService weatherCacheService) {
        this.weatherService = weatherService;
        this.weatherCacheService = weatherCacheService;
    }

    @GetMapping("/weather")
    public ResponseEntity<?> getWeather() {
        WeatherSnapshot snapshot = weatherCacheService.getSnapshot();

        if (snapshot == null) {
            return ResponseEntity.status(503).build();
        }

        return ResponseEntity.ok(new WeatherResponse(
            snapshot.getWeather(),
            snapshot.getRefreshedAt() != null ? snapshot.getRefreshedAt().toString() : null
        ));
    }

    private static class WeatherResponse {
        private final double latitude;
        private final double longitude;
        private final Double temperature;
        private final String lastUpdated;

        WeatherResponse(WeatherData weather, String lastUpdated) {
            this.latitude = weather.getLatitude();
            this.longitude = weather.getLongitude();
            this.temperature = weather.getTemperature();
            this.lastUpdated = lastUpdated;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public Double getTemperature() { return temperature; }
        public String getLastUpdated() { return lastUpdated; }
    }
}