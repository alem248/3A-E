package com.tecsup.clima.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Consulta PostGIS de la comparacion de ubicacion. Repositorio propio para no tocar
 * {@link ZonaVulnerableRepository}. Solo funciona con el perfil {@code postgres}.
 */
public interface ComparacionZonaRepository extends JpaRepository<ZonaVulnerableEntity, Integer> {

    /**
     * La zona mas cercana al punto, indicando si lo contiene. Ordenar por distancia pone
     * primero una zona que contenga el punto (distancia 0). Requiere la columna {@code geom}
     * sincronizada por {@code PostgisGeometriaRefrescador}.
     */
    @Query(value = """
        SELECT z.id AS "zonaId",
               ST_Contains(z.geom, pt.geom) AS "dentro",
               ST_Distance(z.geom::geography, pt.geom::geography) AS "distanciaM"
        FROM zona_vulnerable z
        CROSS JOIN (SELECT ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326) AS geom) pt
        WHERE z.geom IS NOT NULL
        ORDER BY ST_Distance(z.geom::geography, pt.geom::geography) ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<ComparacionZonaProyeccion> buscarZonaMasCercana(@Param("latitud") double latitud,
                                                            @Param("longitud") double longitud);
}
