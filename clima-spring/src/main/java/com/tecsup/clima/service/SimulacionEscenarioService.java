package com.tecsup.clima.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.config.ReglasAlertaProperties;
import com.tecsup.clima.domain.AlertaGenerada;
import com.tecsup.clima.domain.LecturaZona;
import com.tecsup.clima.domain.MotorReglasAlerta;
import com.tecsup.clima.domain.PoligonoUtils;
import com.tecsup.clima.domain.ReglaAlerta;
import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

/**
 * Simula un escenario de fenomeno natural con lecturas de TSM hipoteticas y
 * comprueba que la alerta solo llegue a las ubicaciones que caen dentro de una
 * zona critica afectada por el escenario.
 *
 * <p>Es una simulacion de solo lectura: no registra alertas ni modifica las
 * zonas. Se apoya en el mismo {@link MotorReglasAlerta} que usa la evaluacion
 * real, por lo que las reglas y los umbrales son los de produccion.</p>
 */
@Service
public class SimulacionEscenarioService {

    private final MotorReglasAlerta motor;
    private final ZonaVulnerableRepository zonaRepository;
    private final Clock clock;

    public SimulacionEscenarioService(ReglasAlertaProperties properties,
                                      ZonaVulnerableRepository zonaRepository,
                                      Clock clock) {
        this.motor = new MotorReglasAlerta(properties.reglas());
        this.zonaRepository = zonaRepository;
        this.clock = clock;
    }

    /**
     * Evalua el escenario y devuelve las alertas generadas y el resultado de
     * cada ubicacion.
     *
     * @param escenario   nombre del escenario
     * @param lecturas    TSM hipotetica por zona
     * @param ubicaciones puntos a verificar
     * @return alertas del escenario y evaluacion por ubicacion
     */
    @Transactional(readOnly = true)
    public ResultadoSimulacion simular(String escenario, List<LecturaZona> lecturas,
                                       List<UbicacionSimulada> ubicaciones) {
        List<ZonaVulnerableEntity> zonas = zonaRepository.findAllByOrderByIdAsc();
        Map<Integer, AlertaGenerada> alertasPorZona = alertasPorZona(lecturas);

        List<ResultadoSimulacion.AlertaEscenario> alertas = alertasPorZona.values().stream()
            .map(this::aAlerta)
            .toList();

        List<ResultadoSimulacion.EvaluacionUbicacion> evaluadas = ubicaciones.stream()
            .map(ubicacion -> evaluar(ubicacion, zonas, alertasPorZona))
            .toList();

        return new ResultadoSimulacion(escenario, OffsetDateTime.now(clock), alertas, evaluadas);
    }

    private Map<Integer, AlertaGenerada> alertasPorZona(List<LecturaZona> lecturas) {
        Map<Integer, AlertaGenerada> alertas = new LinkedHashMap<>();

        for (AlertaGenerada alerta : motor.evaluar(lecturas)) {
            alertas.putIfAbsent(alerta.regla().zonaId(), alerta);
        }

        return alertas;
    }

    private ResultadoSimulacion.AlertaEscenario aAlerta(AlertaGenerada alerta) {
        ReglaAlerta regla = alerta.regla();

        return new ResultadoSimulacion.AlertaEscenario(regla.zonaId(), nombreZona(regla.zonaId()),
            regla.nombre(), regla.nivel().getId(), alerta.lectura().temperatura(),
            alerta.mensajeCompleto());
    }

    private ResultadoSimulacion.EvaluacionUbicacion evaluar(UbicacionSimulada ubicacion,
                                                            List<ZonaVulnerableEntity> zonas,
                                                            Map<Integer, AlertaGenerada> alertasPorZona) {
        ZonaVulnerableEntity zona = zonaQueContiene(zonas, ubicacion.latitud(), ubicacion.longitud());
        AlertaGenerada alerta = zona != null ? alertasPorZona.get(zona.getId()) : null;

        return new ResultadoSimulacion.EvaluacionUbicacion(
            ubicacion.etiqueta(), ubicacion.latitud(), ubicacion.longitud(),
            zona != null ? zona.getId() : null,
            zona != null ? nombreZona(zona.getId()) : null,
            zona != null && zona.getNivelRiesgo() != null ? zona.getNivelRiesgo().getNombre() : null,
            alerta != null,
            alerta != null,
            alerta != null ? alerta.regla().nivel().getId() : null,
            mensaje(alerta, zona));
    }

    private ZonaVulnerableEntity zonaQueContiene(List<ZonaVulnerableEntity> zonas,
                                                 double latitud, double longitud) {
        for (ZonaVulnerableEntity zona : zonas) {
            if (PoligonoUtils.contiene(PoligonoUtils.desdeWkt(zona.getPoligono()), latitud, longitud)) {
                return zona;
            }
        }

        return null;
    }

    private static String mensaje(AlertaGenerada alerta, ZonaVulnerableEntity zona) {
        if (alerta != null) {
            return alerta.mensajeCompleto();
        }
        if (zona != null) {
            return "Zona vulnerable no afectada por el escenario: no se genera alerta";
        }

        return "Ubicacion fuera de toda zona vulnerable: no se genera alerta";
    }

    private static String nombreZona(int zonaId) {
        ZonasTermicas.Zona catalogo = ZonasTermicas.getZona(zonaId);
        return catalogo != null ? catalogo.getNombre() : "Zona " + zonaId;
    }
}
