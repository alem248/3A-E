package com.tecsup.clima.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.domain.LecturaZona;
import com.tecsup.clima.domain.ReglaAlerta;
import com.tecsup.clima.service.EvaluacionAlertasService;
import com.tecsup.clima.service.ResultadoEvaluacion;
import com.tecsup.clima.service.ResultadoSimulacion;
import com.tecsup.clima.service.SimulacionEscenarioService;
import com.tecsup.clima.service.UbicacionSimulada;

/** Consulta, simulacion y ejecucion del motor de reglas de alerta. */
@RestController
@RequestMapping("/api/reglas")
public class AlertasController {

    private final EvaluacionAlertasService evaluacionService;
    private final SimulacionEscenarioService simulacionService;

    public AlertasController(EvaluacionAlertasService evaluacionService,
                             SimulacionEscenarioService simulacionService) {
        this.evaluacionService = evaluacionService;
        this.simulacionService = simulacionService;
    }

    /** Reglas configuradas en application.properties. */
    @GetMapping
    public ResponseEntity<List<ReglaAlerta>> getReglas() {
        return ResponseEntity.ok(evaluacionService.getReglas());
    }

    /** Que regla se dispararia con una lectura dada. No guarda nada. */
    @PostMapping("/simular")
    public ResponseEntity<Map<String, Object>> simular(@Valid @RequestBody LecturaRequest request) {
        Optional<ReglaAlerta> regla = evaluacionService.simular(
            new LecturaZona(request.zonaId(), request.temperatura()));

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("seCumple", regla.isPresent());
        respuesta.put("regla", regla.orElse(null));
        return ResponseEntity.ok(respuesta);
    }

    /** Evalua las reglas con la TSM actual y registra las alertas que correspondan. */
    @PostMapping("/evaluar")
    public ResponseEntity<ResultadoEvaluacion> evaluar() {
        return ResponseEntity.ok(evaluacionService.evaluar());
    }

    /**
     * Simula un escenario completo: lecturas hipoteticas de TSM y ubicaciones.
     * Devuelve las alertas del escenario y si cada ubicacion las recibe.
     */
    @PostMapping("/simular-escenario")
    public ResponseEntity<ResultadoSimulacion> simularEscenario(
            @Valid @RequestBody SimulacionEscenarioRequest request) {

        List<LecturaZona> lecturas = request.lecturas().stream()
            .map(lectura -> new LecturaZona(lectura.zonaId(), lectura.temperatura()))
            .toList();

        List<UbicacionSimulada> ubicaciones = new ArrayList<>();
        for (int i = 0; i < request.ubicaciones().size(); i++) {
            UbicacionSimuladaRequest ubicacion = request.ubicaciones().get(i);
            String etiqueta = ubicacion.etiqueta() == null || ubicacion.etiqueta().isBlank()
                ? "Ubicacion " + (i + 1) : ubicacion.etiqueta();
            ubicaciones.add(new UbicacionSimulada(etiqueta, ubicacion.latitud(), ubicacion.longitud()));
        }

        return ResponseEntity.ok(simulacionService.simular(request.nombre(), lecturas, ubicaciones));
    }
}
