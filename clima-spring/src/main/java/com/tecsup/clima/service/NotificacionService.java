package com.tecsup.clima.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.AlertaPreventivaEntity;
import com.tecsup.clima.persistence.HistorialNotificacionEntity;
import com.tecsup.clima.persistence.HistorialNotificacionRegistroRepository;
import com.tecsup.clima.persistence.HistorialNotificacionRepository;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.PreferenciaAlertaEntity;
import com.tecsup.clima.persistence.PreferenciaAlertaRepository;
import com.tecsup.clima.persistence.UbicacionUsuarioRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;

/**
 * Avisos personalizados para la campanita de la pagina. Cuando se crea una
 * alerta, se avisa a los usuarios cuya ultima ubicacion esta en esa zona y que
 * tienen activo el nivel de la alerta.
 */
@Service
public class NotificacionService {

    private final UsuarioRepository usuarioRepository;
    private final UbicacionUsuarioRepository ubicacionRepository;
    private final PreferenciaAlertaRepository preferenciaRepository;
    private final HistorialNotificacionRepository historialRepository;
    private final HistorialNotificacionRegistroRepository registroRepository;

    public NotificacionService(UsuarioRepository usuarioRepository,
                               UbicacionUsuarioRepository ubicacionRepository,
                               PreferenciaAlertaRepository preferenciaRepository,
                               HistorialNotificacionRepository historialRepository,
                               HistorialNotificacionRegistroRepository registroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.historialRepository = historialRepository;
        this.registroRepository = registroRepository;
    }

    /** Crea un aviso por cada usuario afectado y devuelve cuantos se crearon. */
    @Transactional
    public int notificarNuevaAlerta(AlertaPreventivaEntity alerta) {
        if (alerta.getZona() == null || alerta.getNivelRiesgo() == null) {
            return 0;
        }

        Integer zonaId = alerta.getZona().getId();
        NivelRiesgoEntity nivel = alerta.getNivelRiesgo();
        int siguienteId = registroRepository.obtenerUltimoId() + 1;
        int creadas = 0;

        for (UsuarioEntity usuario : usuarioRepository.findAll()) {
            if (!estaEnZona(usuario, zonaId) || !nivelActivo(usuario, nivel)) {
                continue;
            }
            HistorialNotificacionEntity aviso = new HistorialNotificacionEntity();
            aviso.setId(siguienteId++);
            aviso.setUsuario(usuario);
            aviso.setAlerta(alerta);
            aviso.setLeida(false);
            historialRepository.save(aviso);
            creadas++;
        }
        return creadas;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listar(String email) {
        List<Map<String, Object>> items = new ArrayList<>();
        int noLeidas = 0;

        Optional<UsuarioEntity> usuario = usuarioRepository.findByEmail(email);
        if (usuario.isPresent()) {
            for (HistorialNotificacionEntity aviso : historialRepository.findByUsuarioOrderByIdDesc(usuario.get())) {
                AlertaPreventivaEntity alerta = aviso.getAlerta();
                if (!aviso.isLeida()) {
                    noLeidas++;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", aviso.getId());
                item.put("nivel", alerta.getNivelRiesgo().getNombre());
                item.put("zona", nombreZona(alerta));
                item.put("mensaje", alerta.getMensaje());
                item.put("fecha", alerta.getFechaEmision());
                item.put("leida", aviso.isLeida());
                items.add(item);
            }
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("noLeidas", noLeidas);
        respuesta.put("items", items);
        return respuesta;
    }

    @Transactional
    public void marcarLeidas(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            List<HistorialNotificacionEntity> avisos = historialRepository.findByUsuarioOrderByIdDesc(usuario);
            avisos.forEach(aviso -> aviso.setLeida(true));
            historialRepository.saveAll(avisos);
        });
    }

    private boolean estaEnZona(UsuarioEntity usuario, Integer zonaId) {
        return ubicacionRepository.findFirstByUsuarioOrderByFechaRegistroDescIdDesc(usuario)
            .map(ubicacion -> ubicacion.getZona() != null && zonaId.equals(ubicacion.getZona().getId()))
            .orElse(false);
    }

    private boolean nivelActivo(UsuarioEntity usuario, NivelRiesgoEntity nivel) {
        return preferenciaRepository.findByUsuarioAndNivelRiesgo(usuario, nivel)
            .map(PreferenciaAlertaEntity::isActiva)
            .orElse(true);
    }

    private String nombreZona(AlertaPreventivaEntity alerta) {
        if (alerta.getZona() == null) {
            return "";
        }
        ZonasTermicas.Zona zona = ZonasTermicas.getZona(alerta.getZona().getId());
        return zona != null ? zona.getNombre() : String.valueOf(alerta.getZona().getId());
    }
}
