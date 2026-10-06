package com.tecsup.clima.domain;

public class WeatherData {

    private final double latitude;
    private final double longitude;
    private final Double temperature;

    public WeatherData(double latitude, double longitude, Double temperature) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperature = temperature;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Double getTemperature() {
        return temperature;
    }
}