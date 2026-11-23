package com.tecsup.clima.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.HistorialNotificacionRepository;
import com.tecsup.clima.persistence.PreferenciaAlertaRepository;
import com.tecsup.clima.persistence.RolEntity;
import com.tecsup.clima.persistence.RolRepository;
import com.tecsup.clima.persistence.UbicacionUsuarioEntity;
import com.tecsup.clima.service.DepartamentoResolver;
import com.tecsup.clima.persistence.UbicacionUsuarioRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.service.PreferenciaAlertaService;
import com.tecsup.clima.service.ZonasTermicasService;
import com.tecsup.clima.web.dto.ActualizarPreferenciaRequest;

/**
 * Gestion de usuarios para el administrador (solo rol ADMIN, ver SeguridadConfig).
 * Lo consume el panel de Django.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuariosController {

    public record EditarUsuarioRequest(String nombres, String email, String rol) {
    }

    public record CrearUsuarioRequest(String nombres, String email, String rol, String password) {
    }

    private static final String ROL_ADMIN = "ADMIN";
    private static final int LONGITUD_MINIMA_PASSWORD = 8;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UbicacionUsuarioRepository ubicacionRepository;
    private final PreferenciaAlertaService preferenciaService;
    private final PreferenciaAlertaRepository preferenciaRepository;
    private final HistorialNotificacionRepository historialRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private ZonasTermicasService zonasService;

    public AdminUsuariosController(UsuarioRepository usuarioRepository,
                                   RolRepository rolRepository,
                                   UbicacionUsuarioRepository ubicacionRepository,
                                   PreferenciaAlertaService preferenciaService,
                                   PreferenciaAlertaRepository preferenciaRepository,
                                   HistorialNotificacionRepository historialRepository,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.preferenciaService = preferenciaService;
        this.preferenciaRepository = preferenciaRepository;
        this.historialRepository = historialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return usuarioRepository.findAll().stream().map(this::resumen).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Integer id) {
        Optional<UsuarioEntity> usuario = usuarioRepository.findById(id);
        if (usuario.isEmpty()) {
            return noEncontrado();
        }
        Map<String, Object> datos = resumen(usuario.get());
        datos.put("preferencias", preferenciaService.listar(id));
        return ResponseEntity.ok(datos);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody CrearUsuarioRequest solicitud) {
        if (solicitud == null || solicitud.email() == null || solicitud.email().isBlank()) {
            return error(400, "El correo es obligatorio.");
        }
        if (solicitud.password() == null || solicitud.password().length() < LONGITUD_MINIMA_PASSWORD) {
            return error(400, "La contraseña debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres.");
        }

        String email = solicitud.email().trim().toLowerCase();
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return error(400, "Ya existe un usuario con ese correo.");
        }

        Optional<RolEntity> rol = rolRepository.findByNombre(solicitud.rol() == null ? "" : solicitud.rol());
        if (rol.isEmpty()) {
            return error(400, "El rol indicado no existe.");
        }

        int siguienteId = usuarioRepository.findAll().stream()
            .map(UsuarioEntity::getId)
            .max(Integer::compare)
            .orElse(0) + 1;

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(siguienteId);
        usuario.setNombres(solicitud.nombres() == null ? null : solicitud.nombres().trim());
        usuario.setEmail(email);
        usuario.setRol(rol.get());
        usuario.setPasswordHash(passwordEncoder.encode(solicitud.password()));
        return ResponseEntity.status(201).body(resumen(usuarioRepository.save(usuario)));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody EditarUsuarioRequest solicitud) {
        Optional<UsuarioEntity> encontrado = usuarioRepository.findById(id);
        if (encontrado.isEmpty()) {
            return noEncontrado();
        }
        if (solicitud == null || solicitud.email() == null || solicitud.email().isBlank()) {
            return error(400, "El correo es obligatorio.");
        }

        String email = solicitud.email().trim().toLowerCase();
        Optional<UsuarioEntity> otro = usuarioRepository.findByEmail(email);
        if (otro.isPresent() && !otro.get().getId().equals(id)) {
            return error(400, "Ya existe un usuario con ese correo.");
        }

        Optional<RolEntity> rol = rolRepository.findByNombre(solicitud.rol() == null ? "" : solicitud.rol());
        if (rol.isEmpty()) {
            return error(400, "El rol indicado no existe.");
        }

        UsuarioEntity usuario = encontrado.get();
        if (solicitud.nombres() != null && !solicitud.nombres().isBlank()) {
            usuario.setNombres(solicitud.nombres().trim());
        }
        usuario.setEmail(email);
        usuario.setRol(rol.get());
        return ResponseEntity.ok(resumen(usuarioRepository.save(usuario)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        Optional<UsuarioEntity> encontrado = usuarioRepository.findById(id);
        if (encontrado.isEmpty()) {
            return noEncontrado();
        }
        UsuarioEntity usuario = encontrado.get();
        boolean esAdmin = ROL_ADMIN.equals(usuario.getRol().getNombre());
        long administradores = usuarioRepository.findAll().stream()
            .filter(u -> ROL_ADMIN.equals(u.getRol().getNombre()))
            .count();
        if (esAdmin && administradores <= 1) {
            return error(400, "No se puede eliminar al último administrador.");
        }

        historialRepository.deleteAll(historialRepository.findByUsuarioOrderByIdDesc(usuario));
        ubicacionRepository.deleteAll(ubicacionRepository.findByUsuarioOrderByIdDesc(usuario));
        preferenciaRepository.deleteAll(preferenciaRepository.findByUsuario(usuario));
        usuarioRepository.delete(usuario);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/preferencias/{nivelId}")
    public ResponseEntity<?> cambiarPreferencia(@PathVariable Integer id,
                                                @PathVariable Integer nivelId,
                                                @RequestBody(required = false) ActualizarPreferenciaRequest solicitud) {
        if (solicitud == null || solicitud.activa() == null) {
            return error(400, "El campo 'activa' es obligatorio.");
        }
        try {
            return ResponseEntity.ok(preferenciaService.actualizar(id, nivelId, solicitud.activa()));
        } catch (IllegalArgumentException error) {
            return error(400, error.getMessage());
        }
    }

    @GetMapping("/ubicaciones")
    public List<Map<String, Object>> ubicaciones() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (UsuarioEntity usuario : usuarioRepository.findAll()) {
            ubicacionRepository.findFirstByUsuarioOrderByFechaRegistroDescIdDesc(usuario).ifPresent(registro -> {
                completarDistrito(registro);
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("id", usuario.getId());
                fila.put("nombre", usuario.getNombres());
                fila.put("latitud", registro.getLatitud());
                fila.put("longitud", registro.getLongitud());
                fila.put("zonaNombre", nombreZona(registro));
                fila.put("departamento", registro.getDepartamento());
                fila.put("distrito", registro.getDistrito());
                lista.add(fila);
            });
        }
        return lista;
    }

    @Autowired(required = false)
    private DepartamentoResolver departamentoResolver;

    /** Registros guardados antes de existir el distrito: se completa una vez y se guarda. */
    private void completarDistrito(UbicacionUsuarioEntity registro) {
        if (registro.getDistrito() != null || departamentoResolver == null
            || registro.getLatitud() == null || registro.getLongitud() == null) {
            return;
        }
        departamentoResolver.resolverDistrito(registro.getLatitud().doubleValue(), registro.getLongitud().doubleValue())
            .ifPresent(distrito -> {
                registro.setDistrito(distrito);
                ubicacionRepository.save(registro);
            });
    }

    private Map<String, Object> resumen(UsuarioEntity usuario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", usuario.getId());
        datos.put("nombres", usuario.getNombres());
        datos.put("email", usuario.getEmail());
        datos.put("rol", usuario.getRol().getNombre());

        Optional<UbicacionUsuarioEntity> ubicacion = ubicacionRepository
            .findFirstByUsuarioOrderByFechaRegistroDescIdDesc(usuario);
        datos.put("tieneUbicacion", ubicacion.isPresent());
        if (ubicacion.isPresent()) {
            UbicacionUsuarioEntity registro = ubicacion.get();
            datos.put("latitud", registro.getLatitud());
            datos.put("longitud", registro.getLongitud());
            datos.put("fechaUbicacion", registro.getFechaRegistro());
            datos.put("zonaId", registro.getZona() != null ? registro.getZona().getId() : null);
            datos.put("zonaNombre", nombreZona(registro));
            datos.put("nivelZona", registro.getZona() != null && registro.getZona().getNivelRiesgo() != null
                ? registro.getZona().getNivelRiesgo().getNombre() : null);
            datos.put("departamento", registro.getDepartamento());
            datos.put("distrito", registro.getDistrito());
        }
        return datos;
    }

    @PostMapping("/{id}/monitorear")
    @Transactional
    public ResponseEntity<?> monitorear(@PathVariable Integer id) {
        Optional<UsuarioEntity> usuario = usuarioRepository.findById(id);
        if (usuario.isEmpty()) {
            return noEncontrado();
        }
        Optional<UbicacionUsuarioEntity> encontrada = ubicacionRepository
            .findFirstByUsuarioOrderByFechaRegistroDescIdDesc(usuario.get());
        if (encontrada.isEmpty()) {
            return error(400, "El usuario no tiene ubicación registrada.");
        }
        UbicacionUsuarioEntity ubicacion = encontrada.get();
        if (ubicacion.getZona() != null) {
            return error(400, "Esta ubicación ya está en una zona monitoreada.");
        }
        String nombre = ubicacion.getDistrito() != null ? ubicacion.getDistrito() : ubicacion.getDepartamento();
        if (nombre == null || nombre.isBlank()) {
            nombre = "Zona sin nombre";
        }
        try {
            double latitud = ubicacion.getLatitud().doubleValue();
            double longitud = ubicacion.getLongitud().doubleValue();
            Optional<ZonaVulnerableEntity> existente = zonasService
                .buscarZonaExistente(nombre, ubicacion.getDepartamento(), latitud, longitud);
            ZonaVulnerableEntity zona = existente.isPresent()
                ? existente.get()
                : zonasService.crearZonaParaUbicacion(nombre, ubicacion.getDepartamento(), latitud, longitud);
            ubicacion.setZona(zona);
            ubicacionRepository.save(ubicacion);
            zonasService.consultarTsmZona(zona.getId());
            return ResponseEntity.ok(Map.of("zonaId", zona.getId(), "zonaNombre", nombre));
        } catch (IllegalStateException fallo) {
            return error(422, fallo.getMessage());
        }
    }

    private String nombreZona(UbicacionUsuarioEntity registro) {
        if (registro.getZona() == null) {
            return null;
        }
        ZonasTermicas.Zona zona = ZonasTermicas.getZona(registro.getZona().getId());
        if (zona != null) {
            return zona.getNombre();
        }
        String nombrePropio = registro.getZona().getNombre();
        return nombrePropio != null ? nombrePropio : String.valueOf(registro.getZona().getId());
    }

    private static ResponseEntity<?> noEncontrado() {
        return error(404, "Usuario no encontrado.");
    }

    private static ResponseEntity<?> error(int estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}
