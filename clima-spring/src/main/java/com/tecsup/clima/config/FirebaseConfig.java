package com.tecsup.clima.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Inicializa Firebase Admin SDK para el envio de notificaciones FCM.
 *
 * <p>Las credenciales se buscan en la ruta indicada por
 * {@code firebase.credentials-path} (por defecto {@code firebase-service-account.json}),
 * primero en el sistema de archivos y luego en el classpath. Si el archivo no
 * existe o no es valido, la aplicacion arranca en <strong>modo simulado</strong>:
 * las notificaciones se escriben en el log en lugar de enviarse a Firebase.</p>
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private static final String NOMBRE_APP = "clima-costero-notificaciones";

    private final String rutaCredenciales;
    private final String projectIdConfigurado;

    private FirebaseApp firebaseApp;
    private boolean disponible;

    public FirebaseConfig(
            @Value("${firebase.credentials-path:firebase-service-account.json}") String rutaCredenciales,
            @Value("${firebase.project-id:}") String projectIdConfigurado) {
        this.rutaCredenciales = rutaCredenciales;
        this.projectIdConfigurado = projectIdConfigurado;
    }

    @PostConstruct
    public void inicializar() {
        try (InputStream credenciales = abrirCredenciales()) {
            if (credenciales == null) {
                log.warn("No se encontro '{}'. Firebase FCM arranca en MODO SIMULADO: "
                    + "las notificaciones se registraran en consola.", rutaCredenciales);
                return;
            }

            ServiceAccountCredentials cuenta = ServiceAccountCredentials.fromStream(credenciales);
            String projectId = projectIdConfigurado != null && !projectIdConfigurado.isBlank()
                ? projectIdConfigurado
                : cuenta.getProjectId();

            if (projectId == null || projectId.isBlank()) {
                log.warn("La cuenta de servicio no incluye project_id. Firebase FCM arranca en MODO SIMULADO.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(cuenta)
                .setProjectId(projectId)
                .build();

            firebaseApp = FirebaseApp.initializeApp(options, NOMBRE_APP);
            disponible = true;
            log.info("Firebase FCM inicializado correctamente (projectId={}).", projectId);
        } catch (Exception error) {
            firebaseApp = null;
            disponible = false;
            log.warn("No se pudo inicializar Firebase FCM ({}). Se usara MODO SIMULADO.",
                error.getMessage());
        }
    }

    private InputStream abrirCredenciales() throws IOException {
        Path archivo = Paths.get(rutaCredenciales);
        if (Files.isRegularFile(archivo)) {
            return Files.newInputStream(archivo);
        }

        Resource recurso = new ClassPathResource(rutaCredenciales);
        if (recurso.exists()) {
            return recurso.getInputStream();
        }

        return null;
    }

    /** Indica si Firebase quedo operativo; si es falso se usa el modo simulado. */
    public boolean isDisponible() {
        return disponible && firebaseApp != null;
    }

    /** Cliente FCM listo para enviar mensajes. */
    public FirebaseMessaging getMessaging() {
        if (!isDisponible()) {
            throw new IllegalStateException("Firebase FCM no esta disponible (modo simulado).");
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @PreDestroy
    public void cerrar() {
        if (firebaseApp != null) {
            firebaseApp.delete();
            firebaseApp = null;
            disponible = false;
        }
    }
}
