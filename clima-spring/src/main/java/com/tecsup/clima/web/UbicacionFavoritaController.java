package com.tecsup.clima.web;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.persistence.UbicacionFavoritaEntity;
import com.tecsup.clima.persistence.UbicacionFavoritaRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ubicaciones favoritas del usuario de sesion. Solo puede ver y cambiar las suyas. */
@RestController
@RequestMapping("/api/usuarios/{usuarioId}/favoritas")
public class UbicacionFavoritaController {

    public record FavoritaRequest(
            @NotBlank @Size(max = 60) String etiqueta,
            @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud) {
    }

    public record FavoritaRespuesta(int id, String etiqueta, BigDecimal latitud, BigDecimal longitud) {
    }

    private final UsuarioRepository usuarios;
    private final UbicacionFavoritaRepository favoritas;

    public UbicacionFavoritaController(UsuarioRepository usuarios, UbicacionFavoritaRepository favoritas) {
        this.usuarios = usuarios;
        this.favoritas = favoritas;
    }

    @GetMapping
    public ResponseEntity<?> listar(@PathVariable int usuarioId, Authentication autenticacion) {
        return conUsuario(usuarioId, autenticacion, usuario -> ResponseEntity.ok(
                favoritas.findByUsuarioOrderByIdAsc(usuario).stream()
                        .map(UbicacionFavoritaController::aRespuesta)
                        .toList()));
    }

    @PostMapping
    public ResponseEntity<?> crear(@PathVariable int usuarioId,
                                   @Valid @RequestBody FavoritaRequest request,
                                   Authentication autenticacion) {
        return conUsuario(usuarioId, autenticacion, usuario -> {
            UbicacionFavoritaEntity guardada = favoritas.save(new UbicacionFavoritaEntity(
                    usuario, request.etiqueta().trim(), request.latitud(), request.longitud()));
            return ResponseEntity.status(201).body(aRespuesta(guardada));
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int usuarioId,
                                      @PathVariable int id,
                                      Authentication autenticacion) {
        return conUsuario(usuarioId, autenticacion, usuario -> favoritas.findByIdAndUsuario(id, usuario)
                .map(favorita -> {
                    favoritas.delete(favorita);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "Ubicación favorita no encontrada."))));
    }

    private ResponseEntity<?> conUsuario(int usuarioId, Authentication autenticacion,
                                         Function<UsuarioEntity, ResponseEntity<?>> accion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Inicia sesión para usar tus ubicaciones."));
        }
        return usuarios.findByEmail(autenticacion.getName())
                .filter(usuario -> Objects.equals(usuario.getId(), usuarioId))
                .map(accion)
                .orElseGet(() -> ResponseEntity.status(403)
                        .body(Map.of("error", "No puedes acceder a estas ubicaciones.")));
    }

    private static FavoritaRespuesta aRespuesta(UbicacionFavoritaEntity favorita) {
        return new FavoritaRespuesta(favorita.getId(), favorita.getEtiqueta(),
                favorita.getLatitud(), favorita.getLongitud());
    }
}
