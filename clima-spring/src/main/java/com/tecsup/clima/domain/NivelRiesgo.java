package com.tecsup.clima.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum NivelRiesgo {
    VERDE("VERDE", "Verde", "#22C55E", "Condiciones normales", null, 24.0),
    AMARILLO("AMARILLO", "Amarillo", "#EAB308", "Vigilancia / Moderado", 24.0, 26.0),
    NARANJA("NARANJA", "Naranja", "#F97316", "Riesgo Alto", 26.0, 28.0),
    ROJO("ROJO", "Rojo", "#EF4444", "Peligro Critico / Alerta Roja", 28.0, null),
    SIN_DATOS("SIN_DATOS", "Sin datos", "#94A3B8", "Sin lectura de TSM disponible", null, null);

    private final String id;
    private final String nombre;
    private final String color;
    private final String descripcion;
    private final Double umbralInferior;
    private final Double umbralSuperior;

    NivelRiesgo(String id, String nombre, String color, String descripcion,
                Double umbralInferior, Double umbralSuperior) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.descripcion = descripcion;
        this.umbralInferior = umbralInferior;
        this.umbralSuperior = umbralSuperior;
    }

    public static NivelRiesgo clasificarTemperatura(Double tsmCelsius) {
        if (tsmCelsius == null || !Double.isFinite(tsmCelsius)) {
            return SIN_DATOS;
        }

        for (NivelRiesgo nivel : values()) {
            if (nivel == SIN_DATOS) {
                continue;
            }

            boolean sobreInferior = nivel.umbralInferior == null || tsmCelsius >= nivel.umbralInferior;
            boolean bajoSuperior = nivel.umbralSuperior == null || tsmCelsius < nivel.umbralSuperior;

            if (sobreInferior && bajoSuperior) {
                return nivel;
            }
        }

        return SIN_DATOS;
    }

    /** Calor (temperatura del aire, en °C) para regiones del interior. */
    public static NivelRiesgo clasificarCalor(Double celsius) {
        if (celsius == null || !Double.isFinite(celsius)) {
            return SIN_DATOS;
        }
        if (celsius < 28.0) return VERDE;
        if (celsius < 30.0) return AMARILLO;
        if (celsius < 33.0) return NARANJA;
        return ROJO;
    }

    /** Lluvia (mm por hora) para regiones del interior. */
    public static NivelRiesgo clasificarLluvia(Double milimetrosPorHora) {
        if (milimetrosPorHora == null || !Double.isFinite(milimetrosPorHora)) {
            return SIN_DATOS;
        }
        if (milimetrosPorHora < 1.0) return VERDE;
        if (milimetrosPorHora < 5.0) return AMARILLO;
        if (milimetrosPorHora < 10.0) return NARANJA;
        return ROJO;
    }

    /** El nivel mas alto de los dos, ignorando los que no tienen datos. */
    public static NivelRiesgo maximo(NivelRiesgo a, NivelRiesgo b) {
        if (a == SIN_DATOS) return b;
        if (b == SIN_DATOS) return a;
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getColor() {
        return color;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Double getUmbralInferior() {
        return umbralInferior;
    }

    public Double getUmbralSuperior() {
        return umbralSuperior;
    }
}