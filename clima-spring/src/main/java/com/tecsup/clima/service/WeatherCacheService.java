package com.tecsup.clima.service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tecsup.clima.domain.WeatherData;
import com.tecsup.clima.domain.WeatherSnapshot;

@Service
public class WeatherCacheService {

    private final AtomicReference<WeatherSnapshot> snapshot = new AtomicReference<>();

    public synchronized void update(WeatherData weather) {
        snapshot.set(new WeatherSnapshot(weather, LocalDateTime.now()));
    }

    public WeatherSnapshot getSnapshot() {
        return snapshot.get();
    }
}