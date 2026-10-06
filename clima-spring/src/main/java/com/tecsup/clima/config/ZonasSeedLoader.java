package com.tecsup.clima.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.tecsup.clima.domain.ZonaTermica;
import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.ZonaTermicaEntity;
import com.tecsup.clima.persistence.ZonaTermicaRepository;

/**
 * Carga inicial del catalogo de zonas termicas de la costa peruana en la base
 * de datos. Se ejecuta al arrancar la aplicacion y es idempotente: solo inserta
 * las zonas que aun no existen.
 */
@Component
public class ZonasSeedLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ZonasSeedLoader.class);

    private final ZonaTermicaRepository zonaRepository;

    public ZonasSeedLoader(ZonaTermicaRepository zonaRepository) {
        this.zonaRepository = zonaRepository;
    }

    @Override
    public void run(String... args) {
        int orden = 0;

        for (ZonaTermica zona : ZonasTermicas.getZonas()) {
            if (zonaRepository.existsById(zona.getId())) {
                orden++;
                continue;
            }

            ZonaTermicaEntity entidad = new ZonaTermicaEntity();
            entidad.setId(zona.getId());
            entidad.setNombre(zona.getNombre());
            entidad.setDepartamento(zona.getDepartamento());
            entidad.setLatitud(zona.getLatitude());
            entidad.setLongitud(zona.getLongitude());
            entidad.setOrden(orden);
            entidad.setPoligonoWkt(zona.poligonoWkt());

            for (java.util.List<Double> punto : zona.getPoligono()) {
                entidad.agregarVertice(punto.get(0), punto.get(1));
            }

            zonaRepository.save(entidad);
            orden++;
        }

        log.info("Catalogo de zonas termicas sincronizado. Total en base de datos: {}",
            zonaRepository.count());
    }
}
