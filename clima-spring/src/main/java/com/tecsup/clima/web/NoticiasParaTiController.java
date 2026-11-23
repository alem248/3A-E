package com.tecsup.clima.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.persistence.NoticiaEntity;
import com.tecsup.clima.persistence.NoticiaEntity.Estado;
import com.tecsup.clima.persistence.NoticiaRepository;
import com.tecsup.clima.persistence.UbicacionUsuarioEntity;
import com.tecsup.clima.persistence.UbicacionUsuarioRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;

/** Noticias aprobadas para el departamento del usuario con sesion, mas las de alcance nacional. */
@RestController
@RequestMapping("/api/noticias")
public class NoticiasParaTiController {

    private static final int LIMITE = 6;

    private final NoticiaRepository noticiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UbicacionUsuarioRepository ubicacionRepository;

    public NoticiasParaTiController(NoticiaRepository noticiaRepository,
                                    UsuarioRepository usuarioRepository,
                                    UbicacionUsuarioRepository ubicacionRepository) {
        this.noticiaRepository = noticiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ubicacionRepository = ubicacionRepository;
    }

    @GetMapping("/para-ti")
    public ResponseEntity<Map<String, Object>> paraTi(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Inicia sesión para ver noticias para ti."));
        }
        UsuarioEntity usuario = usuarioRepository.findByEmail(autenticacion.getName()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Inicia sesión para ver noticias para ti."));
        }

        String departamento = departamentoDe(usuario);
        List<Map<String, Object>> noticias = noticiaRepository
            .findByEstadoOrderByFechaCreacionDesc(Estado.APROBADA).stream()
            .filter(n -> n.getDepartamento() == null
                || (departamento != null && n.getDepartamento().equalsIgnoreCase(departamento)))
            .limit(LIMITE)
            .map(this::resumen)
            .toList();

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("departamento", departamento);
        respuesta.put("noticias", noticias);
        return ResponseEntity.ok(respuesta);
    }

    /** Departamento de la ultima ubicacion del usuario que lo tenga resuelto. */
    private String departamentoDe(UsuarioEntity usuario) {
        return ubicacionRepository.findByUsuarioOrderByIdDesc(usuario).stream()
            .map(UbicacionUsuarioEntity::getDepartamento)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private Map<String, Object> resumen(NoticiaEntity noticia) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", noticia.getId());
        datos.put("titulo", noticia.getTitulo());
        datos.put("resumen", noticia.getResumen());
        datos.put("contenido", noticia.getContenido());
        datos.put("categoria", noticia.getCategoria());
        datos.put("departamento", noticia.getDepartamento());
        datos.put("fechaCreacion", noticia.getFechaCreacion());
        return datos;
    }
}
