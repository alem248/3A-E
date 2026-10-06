package com.tecsup.clima.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Perfil PostgreSQL: puebla la columna espacial PostGIS {@code geom} de
 * {@code zona_vulnerable} a partir del poligono en texto (WKT) guardado en la
 * tabla, tal como define el DER.
 *
 * <p>Se ejecuta despues de {@link DatosInicialesLoader} para que las zonas
 * sembradas queden con su geometria disponible para consultas espaciales.</p>
 */
@Component
@Profile("postgres")
@Order(2)
public class PostgisGeometriaRefrescador implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgisGeometriaRefrescador.class);

    private final JdbcTemplate jdbcTemplate;

    public PostgisGeometriaRefrescador(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        int actualizadas = jdbcTemplate.update(
            "UPDATE zona_vulnerable "
                + "SET geom = ST_SetSRID(ST_GeomFromText(poligono), 4326) "
                + "WHERE geom IS NULL AND poligono IS NOT NULL");

        log.info("Geometria PostGIS sincronizada. Zonas actualizadas: {}", actualizadas);
    }
}