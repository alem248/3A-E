package com.tecsup.clima.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tecsup.clima.service.EvaluacionAlertasService;
import com.tecsup.clima.service.ResultadoEvaluacion;

/**
 * Evalua las reglas de alerta cada {@code alertas.intervalo-ms} (15 min por
 * defecto, igual que el refresco del mapa). Solo se registra si
 * {@code alertas.evaluacion-automatica=true}.
 */
@Component
@ConditionalOnProperty(name = "alertas.evaluacion-automatica", havingValue = "true")
public class EvaluacionAlertasScheduler {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionAlertasScheduler.class);

    private final EvaluacionAlertasService evaluacionService;

    public EvaluacionAlertasScheduler(EvaluacionAlertasService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    @Scheduled(initialDelayString = "${alertas.retraso-inicial-ms:60000}",
        fixedDelayString = "${alertas.intervalo-ms:900000}")
    public void evaluarReglas() {
        try {
            ResultadoEvaluacion resultado = evaluacionService.evaluar();
            log.info("Reglas de alerta evaluadas: {} alerta(s), {} advertencia(s)",
                resultado.alertas().size(), resultado.advertencias().size());
        } catch (Exception error) {
            // Un fallo (BD o API caida) no debe detener las siguientes ejecuciones
            log.error("Error al evaluar las reglas de alerta", error);
        }
    }
}
