package com.tecsup.clima.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeatherProperties {

    @Value("${weather.open-meteo.url:https://api.open-meteo.com/v1/forecast}")
    private String apiUrl;

    @Value("${weather.open-meteo.latitude:-4.35}")
    private double latitude;

    @Value("${weather.open-meteo.longitude:-81.35}")
    private double longitude;

    public String getApiUrl() {
        return apiUrl;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}