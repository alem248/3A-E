package com.tecsup.clima.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.ComparacionZonaProyeccion;
import com.tecsup.clima.persistence.ComparacionZonaRepository;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

/**
 * Compara la ultima ubicacion del usuario con las zonas del fenomeno usando PostGIS.
 *
 * <p>Clasifica como EN_ZONA si el punto cae dentro de un poligono, CERCA si la zona mas
 * cercana esta a {@code comparacion.distancia-cerca-km} km o menos, y FUERA en otro caso.
 * Solo funciona con el perfil {@code postgres}.</p>
 */
@Service
public class ComparacionUbicacionService {

    static final String EN_ZONA = "EN_ZONA";
    static final String CERCA = "CERCA";
    static final String FUERA = "FUERA";

    private final UbicacionService ubicacionService;
    private final ComparacionZonaRepository comparacionRepository;
    private final ZonaVulnerableRepository zonaRepository;
    private final double distanciaCercaKm;

    public ComparacionUbicacionService(UbicacionService ubicacionService,
                                       ComparacionZonaRepository comparacionRepository,
                                       ZonaVulnerableRepository zonaRepository,
                                       @Value("${comparacion.distancia-cerca-km:50}") double distanciaCercaKm) {
        this.ubicacionService = ubicacionService;
        this.comparacionRepository = comparacionRepository;
        this.zonaRepository = zonaRepository;
        this.distanciaCercaKm = distanciaCercaKm;
    }

    @Transactional(readOnly = true)
    public ComparacionUbicacion comparar(int usuarioId) {
        UbicacionRegistrada ubicacion = ubicacionService.ultima(usuarioId);

        Optional<ComparacionZonaProyeccion> cercana = comparacionRepository.buscarZonaMasCercana(
            ubicacion.latitud().doubleValue(), ubicacion.longitud().doubleValue());

        if (cercana.isEmpty() || cercana.get().getZonaId() == null) {
            return new ComparacionUbicacion(usuarioId, FUERA, null, null, null, null, null, false);
        }

        ComparacionZonaProyeccion resultado = cercana.get();
        double distanciaM = resultado.getDistanciaM() != null ? resultado.getDistanciaM() : Double.MAX_VALUE;
        String estado = clasificar(Boolean.TRUE.equals(resultado.getDentro()), distanciaM,
            distanciaCercaKm * 1000);

        ZonaVulnerableEntity zona = zonaRepository.findById(resultado.getZonaId()).orElse(null);
        String nivel = zona != null && zona.getNivelRiesgo() != null ? zona.getNivelRiesgo().getNombre() : null;
        String color = zona != null && zona.getNivelRiesgo() != null ? zona.getNivelRiesgo().getColorHex() : null;
        ZonasTermicas.Zona catalogo = ZonasTermicas.getZona(resultado.getZonaId());
        String nombre = catalogo != null ? catalogo.getNombre() : String.valueOf(resultado.getZonaId());

        if (FUERA.equals(estado)) {
            return new ComparacionUbicacion(usuarioId, FUERA, null, null, null, null, null, false);
        }

        return new ComparacionUbicacion(usuarioId, estado, resultado.getZonaId(), nombre, nivel, color,
            redondearKm(distanciaM), esRiesgoAlto(nivel));
    }

    /** Regla pura de clasificacion, separada de la consulta para poder probarla sin PostGIS. */
    static String clasificar(boolean dentro, double distanciaM, double umbralM) {
        if (dentro) {
            return EN_ZONA;
        }
        if (distanciaM <= umbralM) {
            return CERCA;
        }
        return FUERA;
    }

    static boolean esRiesgoAlto(String nivel) {
        return nivel != null && (nivel.equalsIgnoreCase("NARANJA") || nivel.equalsIgnoreCase("ROJO"));
    }

    private static Double redondearKm(double distanciaM) {
        return Math.round(distanciaM / 100.0) / 10.0;
    }
}
