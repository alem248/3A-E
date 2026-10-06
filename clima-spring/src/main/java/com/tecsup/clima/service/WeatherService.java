package com.tecsup.clima.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tecsup.clima.config.WeatherProperties;
import com.tecsup.clima.domain.WeatherData;

@Service
public class WeatherService {

    private final RestTemplate restTemplate;
    private final WeatherProperties properties;

    public WeatherService(WeatherProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    public WeatherData fetchWeather() {
        String url = String.format(
            "%s?latitude=%s&longitude=%s&current=temperature_2m",
            properties.getApiUrl(),
            properties.getLatitude(),
            properties.getLongitude()
        );

        JsonNode root = restTemplate.getForObject(url, JsonNode.class);

        double latitude = root.path("latitude").asDouble(properties.getLatitude());
        double longitude = root.path("longitude").asDouble(properties.getLongitude());
        double temperature = root.path("current").path("temperature_2m").asDouble(Double.NaN);

        return new WeatherData(latitude, longitude, temperature);
    }
}