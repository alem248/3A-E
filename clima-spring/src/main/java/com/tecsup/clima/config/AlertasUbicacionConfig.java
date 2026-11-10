package com.tecsup.clima.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra la configuracion de reglas de alerta y de ubicacion GPS, y un
 * {@link Clock} inyectable para que las pruebas usen una hora fija.
 */
@Configuration
@EnableConfigurationProperties({ReglasAlertaProperties.class, UbicacionProperties.class})
public class AlertasUbicacionConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
