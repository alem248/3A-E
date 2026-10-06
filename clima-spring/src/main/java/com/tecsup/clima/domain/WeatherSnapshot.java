package com.tecsup.clima.domain;

import java.time.LocalDateTime;

public class WeatherSnapshot {

    private final WeatherData weather;
    private final LocalDateTime refreshedAt;

    public WeatherSnapshot(WeatherData weather, LocalDateTime refreshedAt) {
        this.weather = weather;
        this.refreshedAt = refreshedAt;
    }

    public WeatherData getWeather() {
        return weather;
    }

    public LocalDateTime getRefreshedAt() {
        return refreshedAt;
    }
}