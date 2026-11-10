package com.tecsup.clima.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Escenario de simulacion: lecturas hipoteticas de TSM y ubicaciones a
 * verificar. Ejemplo:
 * <pre>
 * {
 *   "nombre": "Nino costero fuerte",
 *   "lecturas": [{"zonaId": 1, "temperatura": 29.5}],
 *   "ubicaciones": [{"etiqueta": "Tumbes", "latitud": -3.62, "longitud": -81.0}]
 * }
 * </pre>
 *
 * @param nombre      nombre del escenario
 * @param lecturas    TSM hipotetica por zona
 * @param ubicaciones puntos a comprobar contra las zonas criticas
 */
public record SimulacionEscenarioRequest(
    @NotBlank @Size(max = 120) String nombre,
    @NotEmpty @Valid List<LecturaRequest> lecturas,
    @NotEmpty @Valid List<UbicacionSimuladaRequest> ubicaciones) {
}
