package com.tecsup.clima.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tecsup.clima.persistence.RolEntity;
import com.tecsup.clima.persistence.RolRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;
import com.tecsup.clima.service.DatoInvalidoException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registro, inicio y cierre de sesion de usuarios. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ROL_POBLADOR = "POBLADOR";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthController(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public record RegistroRequest(
            @NotBlank @Size(max = 120) String nombres,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrar(@Valid @RequestBody RegistroRequest solicitud,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        String email = solicitud.email().trim().toLowerCase();
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new DatoInvalidoException("Ya existe una cuenta con ese correo.");
        }

        RolEntity rol = rolRepository.findByNombre(ROL_POBLADOR)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + ROL_POBLADOR));

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(siguienteId());
        usuario.setRol(rol);
        usuario.setNombres(solicitud.nombres().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(solicitud.password()));
        usuarioRepository.save(usuario);

        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, solicitud.password()));
        guardarSesion(autenticacion, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(datosSesion(usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> iniciar(@Valid @RequestBody LoginRequest solicitud,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        String email = solicitud.email().trim().toLowerCase();
        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, solicitud.password()));
        } catch (AuthenticationException error) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contrasena incorrectos.");
        }

        guardarSesion(autenticacion, request, response);
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no encontrada."));
        return ResponseEntity.ok(datosSesion(usuario));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrar(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/yo")
    public ResponseEntity<Map<String, Object>> yo(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getName())) {
            Map<String, Object> anonimo = new LinkedHashMap<>();
            anonimo.put("autenticado", false);
            return ResponseEntity.ok(anonimo);
        }
        return usuarioRepository.findByEmail(autenticacion.getName())
                .map(usuario -> ResponseEntity.ok(datosSesion(usuario)))
                .orElseGet(() -> {
                    Map<String, Object> anonimo = new LinkedHashMap<>();
                    anonimo.put("autenticado", false);
                    return ResponseEntity.ok(anonimo);
                });
    }

    private void guardarSesion(Authentication autenticacion, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        new HttpSessionSecurityContextRepository().saveContext(contexto, request, response);
    }

    private Map<String, Object> datosSesion(UsuarioEntity usuario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("autenticado", true);
        datos.put("id", usuario.getId());
        datos.put("nombres", usuario.getNombres());
        datos.put("email", usuario.getEmail());
        datos.put("rol", usuario.getRol().getNombre());
        return datos;
    }

    private Integer siguienteId() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioEntity::getId)
                .filter(Objects::nonNull)
                .max(Integer::compare)
                .orElse(0) + 1;
    }
}
