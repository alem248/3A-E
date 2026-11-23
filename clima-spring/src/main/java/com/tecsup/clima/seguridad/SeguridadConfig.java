package com.tecsup.clima.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de seguridad. Las rutas existentes del mapa y los datos siguen
 * abiertas; solo /api/auth gestiona la sesion. Las notificaciones personalizadas
 * exigiran sesion cuando se agreguen.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http
            // Las llamadas JSON de /api se hacen desde la misma pagina; el CSRF se
            // desactiva solo para la API. Revisar antes de produccion.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            // Permite mostrar la consola H2 en desarrollo dentro de un marco.
            .headers(cabeceras -> cabeceras.frameOptions(marco -> marco.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll());
        return http.build();
    }
}
