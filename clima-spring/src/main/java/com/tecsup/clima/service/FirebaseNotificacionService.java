package com.tecsup.clima.service;

import java.util.List;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.config.FirebaseConfig;
import com.tecsup.clima.persistence.AlertaPreventivaEntity;
import com.tecsup.clima.persistence.AlertaPreventivaRepository;
import com.tecsup.clima.persistence.HistorialNotificacionEntity;
import com.tecsup.clima.persistence.HistorialNotificacionRegistroRepository;
import com.tecsup.clima.persistence.PreferenciaAlertaEntity;
import com.tecsup.clima.persistence.PreferenciaAlertaRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;

/**
 * Envia las alertas preventivas por Firebase Cloud Messaging (HU-2).
 *
 * <p>Solo recibe la notificacion el usuario que tiene activa la preferencia
 * del nivel de riesgo de la alerta y que cuenta con un {@code fcm_token}. Si
 * Firebase no tiene credenciales, el envio se simula en el log y tambien se
 * registra en HISTORIAL_NOTIFICACION.</p>
 */
@Service
public class FirebaseNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseNotificacionService.class);

    private final FirebaseConfig firebaseConfig;
    private final AlertaPreventivaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PreferenciaAlertaRepository preferenciaRepository;
    private final HistorialNotificacionRegistroRepository historialRepository;

    public FirebaseNotificacionService(FirebaseConfig firebaseConfig,
                                       AlertaPreventivaRepository alertaRepository,
                                       UsuarioRepository usuarioRepository,
                                       PreferenciaAlertaRepository preferenciaRepository,
                                       HistorialNotificacionRegistroRepository historialRepository) {
        this.firebaseConfig = firebaseConfig;
        this.alertaRepository = alertaRepository;
        this.usuarioRepository = usuarioRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.historialRepository = historialRepository;
    }

    /**
     * Envia la alerta indicada a todos los usuarios afectados y registra cada
     * transaccion en HISTORIAL_NOTIFICACION.
     */
    @Transactional
    public ResultadoNotificacion enviarAlerta(Integer alertaId) {
        AlertaPreventivaEntity alerta = alertaRepository.findById(alertaId)
            .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + alertaId));

        if (alerta.getNivelRiesgo() == null) {
            throw new IllegalStateException("La alerta " + alertaId + " no tiene nivel de riesgo.");
        }

        List<UsuarioEntity> destinatarios = obtenerDestinatarios(alerta);
        int siguienteId = historialRepository.obtenerUltimoId() + 1;
        int enviados = 0;
        int fallidos = 0;

        for (UsuarioEntity usuario : destinatarios) {
            if (enviar(usuario, alerta)) {
                registrarHistorial(usuario, alerta, siguienteId++);
                enviados++;
            } else {
                fallidos++;
            }
        }

        log.info("Alerta {} (nivel {}): destinatarios={} enviados={} fallidos={} simulado={}",
            alertaId, alerta.getNivelRiesgo().getNombre(), destinatarios.size(),
            enviados, fallidos, !firebaseConfig.isDisponible());

        return new ResultadoNotificacion(alertaId, destinatarios.size(), enviados, fallidos,
            !firebaseConfig.isDisponible());
    }

    /**
     * Usuarios con token FCM cuya preferencia para el nivel de la alerta esta
     * activa. La ausencia de fila equivale a "activa" (comportamiento por
     * defecto de la pantalla de preferencias).
     */
    private List<UsuarioEntity> obtenerDestinatarios(AlertaPreventivaEntity alerta) {
        return usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getFcmToken() != null && !usuario.getFcmToken().isBlank())
            .filter(usuario -> preferenciaRepository
                .findByUsuarioAndNivelRiesgo(usuario, alerta.getNivelRiesgo())
                .map(PreferenciaAlertaEntity::isActiva)
                .orElse(true))
            .toList();
    }

    private boolean enviar(UsuarioEntity usuario, AlertaPreventivaEntity alerta) {
        String token = usuario.getFcmToken();
        String nivel = alerta.getNivelRiesgo().getNombre();
        String titulo = "Alerta " + nivel;
        String cuerpo = alerta.getMensaje() != null && !alerta.getMensaje().isBlank()
            ? alerta.getMensaje()
            : "Se registro una alerta de nivel " + nivel + " en su zona.";

        if (!firebaseConfig.isDisponible()) {
            log.info("[FCM-SIMULADO] usuario={} token={} titulo='{}' cuerpo='{}' alerta={} zona={}",
                usuario.getId(), enmascarar(token), titulo, cuerpo, alerta.getId(),
                alerta.getZona() != null ? alerta.getZona().getId() : null);
            return true;
        }

        try {
            Message mensaje = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle(titulo)
                    .setBody(cuerpo)
                    .build())
                .putData("alertaId", String.valueOf(alerta.getId()))
                .putData("nivelRiesgo", nivel)
                .putData("zonaId", alerta.getZona() != null
                    ? String.valueOf(alerta.getZona().getId()) : "")
                .build();

            String messageId = firebaseConfig.getMessaging().send(mensaje);
            log.info("FCM enviado a usuario={} alerta={} messageId={}",
                usuario.getId(), alerta.getId(), messageId);
            return true;
        } catch (FirebaseMessagingException error) {
            log.warn("FCM rechazo el envio a usuario={} alerta={}: {}",
                usuario.getId(), alerta.getId(), error.getMessage());
            return false;
        } catch (RuntimeException error) {
            log.warn("Error inesperado al enviar FCM a usuario={} alerta={}: {}",
                usuario.getId(), alerta.getId(), error.getMessage());
            return false;
        }
    }

    private void registrarHistorial(UsuarioEntity usuario, AlertaPreventivaEntity alerta, int id) {
        HistorialNotificacionEntity historial = new HistorialNotificacionEntity();
        historial.setId(id);
        historial.setUsuario(usuario);
        historial.setAlerta(alerta);
        historial.setLeida(false);
        historialRepository.save(historial);
    }

    private String enmascarar(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
