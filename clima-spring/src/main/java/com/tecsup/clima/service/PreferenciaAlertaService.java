package com.tecsup.clima.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.domain.NivelRiesgo;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.NivelRiesgoRepository;
import com.tecsup.clima.persistence.PreferenciaAlertaEntity;
import com.tecsup.clima.persistence.PreferenciaAlertaRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;
import com.tecsup.clima.web.dto.PreferenciaNivelResponse;

/**
 * Gestiona las preferencias de alertas por nivel de riesgo (HU-2).
 *
 * <p>Un nivel sin fila registrada se considera activo por defecto. La primera
 * consulta de la pantalla materializa las filas para que el usuario pueda
 * desactivar cualquier nivel de forma individual.</p>
 */
@Service
public class PreferenciaAlertaService {

    private static final Set<String> NIVELES_NOTIFICABLES =
        Set.of("VERDE", "AMARILLO", "NARANJA", "ROJO");

    private final UsuarioRepository usuarioRepository;
    private final NivelRiesgoRepository nivelRiesgoRepository;
    private final PreferenciaAlertaRepository preferenciaRepository;

    public PreferenciaAlertaService(UsuarioRepository usuarioRepository,
                                    NivelRiesgoRepository nivelRiesgoRepository,
                                    PreferenciaAlertaRepository preferenciaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.nivelRiesgoRepository = nivelRiesgoRepository;
        this.preferenciaRepository = preferenciaRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioEntity obtenerUsuario(Integer usuarioId) {
        return usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));
    }

    /** Preferencias del usuario para Verde, Amarillo, Naranja y Rojo. */
    @Transactional
    public List<PreferenciaNivelResponse> listar(Integer usuarioId) {
        UsuarioEntity usuario = obtenerUsuario(usuarioId);
        List<PreferenciaNivelResponse> preferencias = new ArrayList<>();

        for (NivelRiesgoEntity nivel : nivelesNotificables()) {
            PreferenciaAlertaEntity preferencia = preferenciaRepository
                .findByUsuarioAndNivelRiesgo(usuario, nivel)
                .orElseGet(() -> crear(usuario, nivel, true));

            preferencias.add(aRespuesta(nivel, preferencia.isActiva()));
        }

        return preferencias;
    }

    /** Activa o desactiva un nivel de riesgo para el usuario indicado. */
    @Transactional
    public PreferenciaNivelResponse actualizar(Integer usuarioId, Integer nivelRiesgoId, boolean activa) {
        UsuarioEntity usuario = obtenerUsuario(usuarioId);
        NivelRiesgoEntity nivel = nivelRiesgoRepository.findById(nivelRiesgoId)
            .orElseThrow(() -> new IllegalArgumentException("Nivel de riesgo no encontrado: " + nivelRiesgoId));

        if (!esNotificable(nivel)) {
            throw new IllegalArgumentException("El nivel " + nivel.getNombre() + " no admite notificaciones.");
        }

        PreferenciaAlertaEntity preferencia = preferenciaRepository
            .findByUsuarioAndNivelRiesgo(usuario, nivel)
            .orElseGet(() -> crear(usuario, nivel, true));

        preferencia.setActiva(activa);
        PreferenciaAlertaEntity guardada = preferenciaRepository.save(preferencia);
        return aRespuesta(nivel, guardada.isActiva());
    }

    /**
     * Filtra los usuarios que deben recibir una alerta de un nivel: deben tener
     * token FCM y la preferencia activa (o no tener fila, que equivale a activa).
     */
    @Transactional(readOnly = true)
    public List<UsuarioEntity> usuariosAfectadosPorNivel(Integer nivelRiesgoId) {
        NivelRiesgoEntity nivel = nivelRiesgoRepository.findById(nivelRiesgoId)
            .orElseThrow(() -> new IllegalArgumentException("Nivel de riesgo no encontrado: " + nivelRiesgoId));

        return usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getFcmToken() != null && !usuario.getFcmToken().isBlank())
            .filter(usuario -> preferenciaRepository.findByUsuarioAndNivelRiesgo(usuario, nivel)
                .map(PreferenciaAlertaEntity::isActiva)
                .orElse(true))
            .toList();
    }

    private List<NivelRiesgoEntity> nivelesNotificables() {
        return nivelRiesgoRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
            .filter(this::esNotificable)
            .toList();
    }

    private boolean esNotificable(NivelRiesgoEntity nivel) {
        return nivel.getNombre() != null && NIVELES_NOTIFICABLES.contains(nivel.getNombre().toUpperCase());
    }

    private PreferenciaAlertaEntity crear(UsuarioEntity usuario, NivelRiesgoEntity nivel, boolean activa) {
        PreferenciaAlertaEntity preferencia = new PreferenciaAlertaEntity();
        preferencia.setUsuario(usuario);
        preferencia.setNivelRiesgo(nivel);
        preferencia.setActiva(activa);
        return preferenciaRepository.save(preferencia);
    }

    private PreferenciaNivelResponse aRespuesta(NivelRiesgoEntity nivel, boolean activa) {
        return new PreferenciaNivelResponse(
            nivel.getId(),
            nivel.getNombre(),
            nivel.getColorHex(),
            descripcion(nivel.getNombre()),
            activa);
    }

    private String descripcion(String nombreNivel) {
        for (NivelRiesgo nivel : NivelRiesgo.values()) {
            if (nivel.getId().equalsIgnoreCase(nombreNivel)) {
                return nivel.getDescripcion();
            }
        }
        return "";
    }
}
