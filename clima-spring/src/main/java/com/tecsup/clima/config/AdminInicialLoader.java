package com.tecsup.clima.config;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.persistence.RolEntity;
import com.tecsup.clima.persistence.RolRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;

/**
 * Crea la cuenta de administrador (rol ADMIN) al arrancar, para que pueda
 * iniciar sesion desde la pagina principal. Si ya existe, no hace nada.
 *
 * <p>Credenciales: se toman de clima.admin.email y clima.admin.password.
 * Si no se definen, se usan valores de desarrollo local.</p>
 */
@Component
@Order(2)
public class AdminInicialLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialLoader.class);
    private static final int ROL_ADMIN = 3;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInicialLoader(UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${clima.admin.email:admin@clima.local}") String email,
                              @Value("${clima.admin.password:Admin12345}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return;
        }

        RolEntity rol = rolRepository.findById(ROL_ADMIN).orElseThrow();
        int id = usuarioRepository.findAll().stream()
            .map(UsuarioEntity::getId)
            .max(Comparator.naturalOrder())
            .orElse(0) + 1;

        UsuarioEntity admin = new UsuarioEntity();
        admin.setId(id);
        admin.setRol(rol);
        admin.setNombres("Administrador");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        usuarioRepository.save(admin);

        log.info("Cuenta de administrador creada: {}", email);
    }
}
