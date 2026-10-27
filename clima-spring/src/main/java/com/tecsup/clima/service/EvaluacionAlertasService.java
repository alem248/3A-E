package com.tecsup.clima.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tecsup.clima.config.ReglasAlertaProperties;
import com.tecsup.clima.domain.AlertaGenerada;
import com.tecsup.clima.domain.LecturaZona;
import com.tecsup.clima.domain.MotorReglasAlerta;
import com.tecsup.clima.domain.ReglaAlerta;
import com.tecsup.clima.domain.ZonaTermica;
import com.tecsup.clima.persistence.AlertaPreventivaEntity;
import com.tecsup.clima.persistence.AlertaPreventivaRepository;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.NivelRiesgoRepository;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

/**
 * Aplica las reglas configuradas a la TSM en tiempo real de cada zona y registra
 * las alertas resultantes en {@code alerta_preventiva}. Si ya hay una alerta
 * vigente de la misma zona y nivel, no se duplica.
 */
@Service
public class EvaluacionAlertasService {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionAlertasService.class);

    private final MotorReglasAlerta motor;
    private final ZonasTermicasService zonasService;
    private final ZonaVulnerableRepository zonaRepository;
    private final NivelRiesgoRepository nivelRepository;
    private final AlertaPreventivaRepository alertaRepository;
    private final Clock clock;

    public EvaluacionAlertasService(ReglasAlertaProperties properties,
                                    ZonasTermicasService zonasService,
                                    ZonaVulnerableRepository zonaRepository,
                                    NivelRiesgoRepository nivelRepository,
                                    AlertaPreventivaRepository alertaRepository,
                                    Clock clock) {
        this.motor = new MotorReglasAlerta(properties.reglas());
        this.zonasService = zonasService;
        this.zonaRepository = zonaRepository;
        this.nivelRepository = nivelRepository;
        this.alertaRepository = alertaRepository;
        this.clock = clock;
    }

    public List<ReglaAlerta> getReglas() {
        return motor.getReglas();
    }

    /** Evalua una lectura hipotetica sin guardar nada (para probar las reglas). */
    public Optional<ReglaAlerta> simular(LecturaZona lectura) {
        return motor.evaluar(lectura);
    }

    /** Consulta la TSM actual de todas las zonas y evalua las reglas. */
    public ResultadoEvaluacion evaluar() {
        List<LecturaZona> lecturas = new ArrayList<>();

        for (ZonaTermica zona : zonasService.consultarTsmPorZonas()) {
            if (zona.isDisponible() && zona.getTsm() != null) {
                lecturas.add(new LecturaZona(Integer.parseInt(zona.getId()), zona.getTsm()));
            }
        }

        return evaluar(lecturas);
    }

    /** Evalua las reglas sobre las lecturas dadas y registra las alertas. */
    public ResultadoEvaluacion evaluar(List<LecturaZona> lecturas) {
        List<String> advertencias = new ArrayList<>();
        Set<Integer> zonasConReglas = new TreeSet<>();
        motor.getReglas().forEach(regla -> zonasConReglas.add(regla.zonaId()));

        for (Integer zonaId : zonasConReglas) {
            if (!zonaRepository.existsById(zonaId)) {
                advertencias.add("La zona " + zonaId + " usada en las reglas no existe");
            } else if (lecturas.stream().noneMatch(lectura -> lectura.zonaId() == zonaId)) {
                advertencias.add("Sin lectura de TSM para la zona " + zonaId);
            }
        }

        List<ResultadoEvaluacion.Alerta> alertas = new ArrayList<>();

        for (AlertaGenerada generada : motor.evaluar(lecturas)) {
            registrar(generada).ifPresent(alertas::add);
        }

        advertencias.forEach(log::warn);
        return new ResultadoEvaluacion(OffsetDateTime.now(clock), alertas, advertencias);
    }

    private Optional<ResultadoEvaluacion.Alerta> registrar(AlertaGenerada generada) {
        ReglaAlerta regla = generada.regla();
        double temperatura = generada.lectura().temperatura();

        // Una regla puede apuntar a una zona que no existe; ya se reporto como advertencia
        Optional<ZonaVulnerableEntity> zona = zonaRepository.findById(regla.zonaId());
        if (zona.isEmpty()) {
            return Optional.empty();
        }

        NivelRiesgoEntity nivel = nivelRepository.findByNombre(regla.nivel().getId())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "El nivel " + regla.nivel().getId() + " no existe en nivel_riesgo"));

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Optional<AlertaPreventivaEntity> vigente = alertaRepository
            .findFirstByZonaIdAndNivelRiesgoIdAndVigenciaHastaAfterOrderByFechaEmisionDesc(
                regla.zonaId(), nivel.getId(), ahora);

        if (vigente.isPresent()) {
            return Optional.of(new ResultadoEvaluacion.Alerta(vigente.get().getId(), regla.zonaId(),
                regla.nombre(), nivel.getNombre(), temperatura, ResultadoEvaluacion.Estado.YA_VIGENTE));
        }

        AlertaPreventivaEntity alerta = new AlertaPreventivaEntity();
        alerta.setZona(zona.get());
        alerta.setNivelRiesgo(nivel);
        alerta.setMensaje(generada.mensajeCompleto());
        alerta.setFechaEmision(ahora);
        alerta.setVigenciaHasta(ahora.plusHours(regla.vigenciaHoras()));
        AlertaPreventivaEntity guardada = alertaRepository.save(alerta);

        log.info("Alerta {} creada por la regla '{}' (zona {}, TSM {} C)",
            guardada.getId(), regla.nombre(), regla.zonaId(), temperatura);

        return Optional.of(new ResultadoEvaluacion.Alerta(guardada.getId(), regla.zonaId(),
            regla.nombre(), nivel.getNombre(), temperatura, ResultadoEvaluacion.Estado.CREADA));
    }
}
