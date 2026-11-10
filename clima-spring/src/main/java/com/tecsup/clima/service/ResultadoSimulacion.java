package com.tecsup.clima.service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resultado de simular un escenario de fenomeno natural.
 *
 * @param escenario   nombre del escenario simulado
 * @param fecha       momento de la simulacion
 * @param alertas     alertas que el escenario generaria por zona
 * @param ubicaciones evaluacion de cada ubicacion simulada
 */
public record ResultadoSimulacion(String escenario, OffsetDateTime fecha,
                                  List<AlertaEscenario> alertas,
                                  List<EvaluacionUbicacion> ubicaciones) {

    /**
     * Alerta que el escenario generaria en una zona critica.
     *
     * @param zonaId      zona afectada
     * @param zona        nombre de la zona en el catalogo
     * @param regla       regla que se cumple
     * @param nivel       nivel de riesgo de la alerta
     * @param temperatura TSM del escenario
     * @param mensaje     mensaje para el poblador
     */
    public record AlertaEscenario(int zonaId, String zona, String regla, String nivel,
                                  double temperatura, String mensaje) {
    }

    /**
     * Verificacion de una ubicacion contra las zonas criticas del escenario.
     *
     * @param etiqueta        nombre visible de la ubicacion
     * @param latitud         grados decimales WGS84
     * @param longitud        grados decimales WGS84
     * @param zonaId          zona que contiene el punto; null si esta fuera
     * @param zona            nombre de la zona; null si esta fuera
     * @param nivelRiesgo     nivel persistido de la zona
     * @param zonaCritica     si la zona tiene una alerta en este escenario
     * @param alertaDisparada si la ubicacion recibe la alerta
     * @param nivelAlerta     nivel de la alerta recibida; null si no recibe
     * @param mensaje         explicacion del resultado
     */
    public record EvaluacionUbicacion(String etiqueta, double latitud, double longitud,
                                      Integer zonaId, String zona, String nivelRiesgo,
                                      boolean zonaCritica, boolean alertaDisparada,
                                      String nivelAlerta, String mensaje) {
    }
}
