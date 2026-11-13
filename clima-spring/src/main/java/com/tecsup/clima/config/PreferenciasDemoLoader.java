package com.tecsup.clima.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.tecsup.clima.persistence.RolEntity;
import com.tecsup.clima.persistence.RolRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;

/**
 * Crea un usuario demo con token FCM para poder probar la pantalla de
 * preferencias y el envio simulado sin depender de datos externos.
 *
 * <p>Si el usuario ya existe no se modifica. Se puede desactivar con
 * {@code app.demo.preferencias=false}.</p>
 */
@Component
@Order(2)
public class PreferenciasDemoLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PreferenciasDemoLoader.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Value("${app.demo.preferencias:true}")
    private boolean habilitado;

    public PreferenciasDemoLoader(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    @Override
    public void run(String... args) {
        if (!habilitado || usuarioRepository.existsById(1)) {
            return;
        }

        RolEntity rol = rolRepository.findById(1).orElse(null);
        if (rol == null) {
            log.warn("No se creo el usuario demo: falta el rol base con id 1.");
            return;
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(1);
        usuario.setRol(rol);
        usuario.setNombres("Usuario Demo");
        usuario.setEmail("demo@tecsup.edu.pe");
        usuario.setFcmToken("fcm-token-demo");
        usuarioRepository.save(usuario);

        log.info("Usuario demo creado (id=1) con fcm_token para probar alertas.");
    }
}
