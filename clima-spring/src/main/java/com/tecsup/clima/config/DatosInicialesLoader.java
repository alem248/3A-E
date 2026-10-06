package com.tecsup.clima.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.domain.NivelRiesgo;
import com.tecsup.clima.domain.PoligonoUtils;
import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.EstacionMonitoreoEntity;
import com.tecsup.clima.persistence.EstacionMonitoreoRepository;
import com.tecsup.clima.persistence.EventoClimaticoEntity;
import com.tecsup.clima.persistence.EventoClimaticoRepository;
import com.tecsup.clima.persistence.FuenteOficialEntity;
import com.tecsup.clima.persistence.FuenteOficialRepository;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.NivelRiesgoRepository;
import com.tecsup.clima.persistence.RolEntity;
import com.tecsup.clima.persistence.RolRepository;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

/**
 * Carga inicial de las tablas base del DER al arrancar la aplicacion.
 * Es idempotente: solo inserta los registros que aun no existen.
 *
 * <p>Sembrado: rol, nivel_riesgo, fuente_oficial, evento_climatico,
 * estacion_monitoreo y zona_vulnerable (costa norte).</p>
 */
@Component
@Order(1)
public class DatosInicialesLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesLoader.class);

    private final RolRepository rolRepository;
    private final NivelRiesgoRepository nivelRiesgoRepository;
    private final FuenteOficialRepository fuenteRepository;
    private final EventoClimaticoRepository eventoRepository;
    private final EstacionMonitoreoRepository estacionRepository;
    private final ZonaVulnerableRepository zonaRepository;

    public DatosInicialesLoader(RolRepository rolRepository,
                                NivelRiesgoRepository nivelRiesgoRepository,
                                FuenteOficialRepository fuenteRepository,
                                EventoClimaticoRepository eventoRepository,
                                EstacionMonitoreoRepository estacionRepository,
                                ZonaVulnerableRepository zonaRepository) {
        this.rolRepository = rolRepository;
        this.nivelRiesgoRepository = nivelRiesgoRepository;
        this.fuenteRepository = fuenteRepository;
        this.eventoRepository = eventoRepository;
        this.estacionRepository = estacionRepository;
        this.zonaRepository = zonaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sembrarRoles();
        sembrarNivelesRiesgo();
        FuenteOficialEntity fuente = sembrarFuenteOficial();
        sembrarEventoClimatico();
        sembrarZonasYEstaciones(fuente);

        log.info("Datos iniciales del DER verificados. zonas={} estaciones={} niveles={}",
            zonaRepository.count(), estacionRepository.count(), nivelRiesgoRepository.count());
    }

    private void sembrarRoles() {
        crearRol(1, "POBLADOR", "Poblador de la costa norte");
        crearRol(2, "DEFENSA_CIVIL", "Miembro de Defensa Civil");
        crearRol(3, "ADMIN", "Administrador del sistema");
    }

    private void crearRol(int id, String nombre, String descripcion) {
        if (rolRepository.existsById(id)) {
            return;
        }

        RolEntity rol = new RolEntity();
        rol.setId(id);
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        rolRepository.save(rol);
    }

    private void sembrarNivelesRiesgo() {
        int id = 1;

        for (NivelRiesgo nivel : NivelRiesgo.values()) {
            if (nivelRiesgoRepository.existsById(id)) {
                id++;
                continue;
            }

            NivelRiesgoEntity entidad = new NivelRiesgoEntity();
            entidad.setId(id);
            entidad.setNombre(nivel.getId());
            entidad.setColorHex(nivel.getColor());
            nivelRiesgoRepository.save(entidad);
            id++;
        }
    }

    private FuenteOficialEntity sembrarFuenteOficial() {
        if (fuenteRepository.existsById(1)) {
            return fuenteRepository.findById(1).orElseThrow();
        }

        FuenteOficialEntity fuente = new FuenteOficialEntity();
        fuente.setId(1);
        fuente.setNombreInstitucion("Open-Meteo");
        fuente.setUrlApi("https://marine-api.open-meteo.com/v1/marine");
        return fuenteRepository.save(fuente);
    }

    private void sembrarEventoClimatico() {
        if (eventoRepository.existsById(1)) {
            return;
        }

        EventoClimaticoEntity evento = new EventoClimaticoEntity();
        evento.setId(1);
        evento.setNombre("Nino Costero");
        evento.setFechaInicio(LocalDate.now().withDayOfYear(1));
        evento.setEstado("ACTIVO");
        eventoRepository.save(evento);
    }

    /**
     * Las zonas y sus estaciones de monitoreo comparten el mismo identificador
     * (id), por lo que el registro TSM de una estacion corresponde a la zona
     * vulnerable del mismo id.
     */
    private void sembrarZonasYEstaciones(FuenteOficialEntity fuente) {
        NivelRiesgoEntity sinDatos = nivelRiesgoRepository.findAll().stream()
            .filter(nivel -> NivelRiesgo.SIN_DATOS.getId().equals(nivel.getNombre()))
            .findFirst()
            .orElse(null);

        for (ZonasTermicas.Zona zona : ZonasTermicas.getZonas()) {
            int id = zona.getId();
            List<List<Double>> poligono = zona.getPoligono();

            if (!estacionRepository.existsById(id)) {
                EstacionMonitoreoEntity estacion = new EstacionMonitoreoEntity();
                estacion.setId(id);
                estacion.setFuente(fuente);
                estacion.setTipo("MARINA");
                estacion.setLatitud(BigDecimal.valueOf(zona.getLatitud()));
                estacion.setLongitud(BigDecimal.valueOf(zona.getLongitud()));
                estacionRepository.save(estacion);
            }

            if (zonaRepository.existsById(id)) {
                continue;
            }

            ZonaVulnerableEntity entidad = new ZonaVulnerableEntity();
            entidad.setId(id);
            entidad.setNivelRiesgo(sinDatos);
            entidad.setPoligono(PoligonoUtils.aWkt(poligono));
            zonaRepository.save(entidad);
        }
    }
}