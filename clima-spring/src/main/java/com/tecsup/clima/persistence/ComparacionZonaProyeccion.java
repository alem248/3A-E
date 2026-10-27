package com.tecsup.clima.persistence;

/**
 * Resultado de la consulta PostGIS que compara un punto con las zonas vulnerables.
 *
 * <ul>
 *   <li>{@code zonaId}: zona mas cercana al punto (0 m si el punto esta dentro).</li>
 *   <li>{@code dentro}: true si el punto cae dentro del poligono de la zona.</li>
 *   <li>{@code distanciaM}: distancia geodesica en metros entre el punto y la zona.</li>
 * </ul>
 */
public interface ComparacionZonaProyeccion {

    Integer getZonaId();

    Boolean getDentro();

    Double getDistanciaM();
}
