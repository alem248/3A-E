package com.tecsup.clima.web;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tecsup.clima.persistence.NoticiaEntity;
import com.tecsup.clima.persistence.NoticiaEntity.Estado;
import com.tecsup.clima.persistence.NoticiaEntity.Origen;
import com.tecsup.clima.persistence.NoticiaRepository;

/** Noticias para el administrador (solo ADMIN). Lo consume el panel de Django. */
@RestController
@RequestMapping("/api/admin/noticias")
public class AdminNoticiasController {

    public record NoticiaRequest(String titulo, String resumen, String categoria, String departamento) {
    }

    private static final int MAX_TITULO = 200;
    private static final int MAX_RESUMEN = 600;

    private final NoticiaRepository repository;

    public AdminNoticiasController(NoticiaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(required = false) String estado) {
        List<NoticiaEntity> noticias;
        if (estado == null || estado.isBlank()) {
            noticias = repository.findAllByOrderByFechaCreacionDesc();
        } else {
            try {
                noticias = repository.findByEstadoOrderByFechaCreacionDesc(Estado.valueOf(estado.toUpperCase()));
            } catch (IllegalArgumentException error) {
                return List.of();
            }
        }
        return noticias.stream().map(this::resumen).toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody NoticiaRequest solicitud) {
        String error = validar(solicitud);
        if (error != null) {
            return error(400, error);
        }
        NoticiaEntity noticia = new NoticiaEntity();
        aplicar(noticia, solicitud);
        noticia.setOrigen(Origen.MANUAL);
        noticia.setEstado(Estado.APROBADA);
        noticia.setFechaCreacion(OffsetDateTime.now());
        return ResponseEntity.status(201).body(resumen(repository.save(noticia)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody NoticiaRequest solicitud) {
        Optional<NoticiaEntity> encontrada = repository.findById(id);
        if (encontrada.isEmpty()) {
            return noEncontrada();
        }
        String error = validar(solicitud);
        if (error != null) {
            return error(400, error);
        }
        NoticiaEntity noticia = encontrada.get();
        aplicar(noticia, solicitud);
        return ResponseEntity.ok(resumen(repository.save(noticia)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        if (!repository.existsById(id)) {
            return noEncontrada();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Integer id) {
        return cambiarEstado(id, Estado.APROBADA);
    }

    @PostMapping("/{id}/descartar")
    public ResponseEntity<?> descartar(@PathVariable Integer id) {
        return cambiarEstado(id, Estado.DESCARTADA);
    }

    private ResponseEntity<?> cambiarEstado(Integer id, Estado estado) {
        Optional<NoticiaEntity> encontrada = repository.findById(id);
        if (encontrada.isEmpty()) {
            return noEncontrada();
        }
        NoticiaEntity noticia = encontrada.get();
        noticia.setEstado(estado);
        return ResponseEntity.ok(resumen(repository.save(noticia)));
    }

    private static String validar(NoticiaRequest solicitud) {
        if (solicitud == null || solicitud.titulo() == null || solicitud.titulo().isBlank()) {
            return "El título es obligatorio.";
        }
        if (solicitud.titulo().trim().length() > MAX_TITULO) {
            return "El título no puede superar " + MAX_TITULO + " caracteres.";
        }
        if (solicitud.resumen() == null || solicitud.resumen().isBlank()) {
            return "El resumen es obligatorio.";
        }
        if (solicitud.resumen().trim().length() > MAX_RESUMEN) {
            return "El resumen no puede superar " + MAX_RESUMEN + " caracteres.";
        }
        return null;
    }

    private static void aplicar(NoticiaEntity noticia, NoticiaRequest solicitud) {
        noticia.setTitulo(solicitud.titulo().trim());
        noticia.setResumen(solicitud.resumen().trim());
        noticia.setCategoria(solicitud.categoria() == null || solicitud.categoria().isBlank()
            ? "General" : solicitud.categoria().trim());
        noticia.setDepartamento(solicitud.departamento() == null || solicitud.departamento().isBlank()
            ? null : solicitud.departamento().trim());
    }

    private static final long MAX_IMAGEN = 2L * 1024 * 1024;
    private static final java.util.Set<String> TIPOS_IMAGEN =
        java.util.Set.of("image/jpeg", "image/png", "image/webp");

    @PostMapping(value = "/{id}/imagen", consumes = "multipart/form-data")
    public ResponseEntity<?> subirImagen(@PathVariable Integer id,
                                         @RequestParam("imagen") MultipartFile archivo) throws java.io.IOException {
        Optional<NoticiaEntity> encontrada = repository.findById(id);
        if (encontrada.isEmpty()) {
            return noEncontrada();
        }
        String tipo = archivo.getContentType();
        if (archivo.isEmpty() || archivo.getSize() > MAX_IMAGEN || tipo == null || !TIPOS_IMAGEN.contains(tipo)) {
            return error(400, "La imagen debe ser JPG, PNG o WEBP de máximo 2 MB.");
        }
        NoticiaEntity noticia = encontrada.get();
        noticia.setImagen(archivo.getBytes());
        noticia.setImagenTipo(tipo);
        return ResponseEntity.ok(resumen(repository.save(noticia)));
    }

    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> imagen(@PathVariable Integer id) {
        Optional<NoticiaEntity> encontrada = repository.findById(id);
        if (encontrada.isEmpty() || encontrada.get().getImagen() == null) {
            return ResponseEntity.notFound().build();
        }
        NoticiaEntity noticia = encontrada.get();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(noticia.getImagenTipo()))
            .body(noticia.getImagen());
    }

    private Map<String, Object> resumen(NoticiaEntity noticia) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", noticia.getId());
        datos.put("titulo", noticia.getTitulo());
        datos.put("resumen", noticia.getResumen());
        datos.put("categoria", noticia.getCategoria());
        datos.put("departamento", noticia.getDepartamento());
        datos.put("origen", noticia.getOrigen().name());
        datos.put("estado", noticia.getEstado().name());
        datos.put("fechaCreacion", noticia.getFechaCreacion());
        datos.put("tieneImagen", noticia.getImagen() != null);
        return datos;
    }

    private static ResponseEntity<?> noEncontrada() {
        return error(404, "Noticia no encontrada.");
    }

    private static ResponseEntity<?> error(int estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}
