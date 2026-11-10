package com.tecsup.clima.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecsup.clima.config.UbicacionProperties;
import com.tecsup.clima.domain.PoligonoUtils;
import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.AlertaPreventivaRepository;
import com.tecsup.clima.persistence.UbicacionUsuarioEntity;
import com.tecsup.clima.persistence.UbicacionUsuarioRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

/**
 * Registra la posicion GPS del usuario junto con la zona vulnerable que la
 * contiene y la marca temporal de captura.
 *
 * <p>Los rangos de latitud/longitud se validan con {@code @Valid} en el
 * controlador; aqui se aplican las reglas que dependen de la configuracion o
 * de la hora actual.</p>
 */
@Service
public class UbicacionService {

    /** Las coordenadas se guardan con 6 decimales (~0.1 m), suficiente para GPS. */
    private static final int DECIMALES = 6;

    private final UsuarioRepository usuarioRepository;
    private final UbicacionUsuarioRepository ubicacionRepository;
    private final ZonaVulnerableRepository zonaRepository;
    private final AlertaPreventivaRepository alertaRepository;
    private final UbicacionProperties properties;
    private final Clock clock;

    public UbicacionService(UsuarioRepository usuarioRepository,
                            UbicacionUsuarioRepository ubicacionRepository,
                            ZonaVulnerableRepository zonaRepository,
                            AlertaPreventivaRepository alertaRepository,
                            UbicacionProperties properties,
                            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.zonaRepository = zonaRepository;
        this.alertaRepository = alertaRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public UbicacionRegistrada registrar(int usuarioId, double latitud, double longitud,
                                         Double precisionMetros, OffsetDateTime fechaCaptura) {
        validarPrecision(precisionMetros);
        OffsetDateTime fecha = resolverFecha(fechaCaptura);
        UsuarioEntity usuario = buscarUsuario(usuarioId);

        UbicacionUsuarioEntity ubicacion = new UbicacionUsuarioEntity();
        ubicacion.setUsuario(usuario);
        ubicacion.setLatitud(redondear(latitud));
        ubicacion.setLongitud(redondear(longitud));
        ubicacion.setPrecisionMetros(precisionMetros);
        ubicacion.setFechaRegistro(fecha);
        ubicacion.setZona(zonaQueContiene(latitud, longitud).orElse(null));

        return aRespuesta(ubicacionRepository.save(ubicacion));
    }

    @Transactional(readOnly = true)
    public UbicacionRegistrada ultima(int usuarioId) {
        UsuarioEntity usuario = buscarUsuario(usuarioId);

        return ubicacionRepository.findFirstByUsuarioOrderByFechaRegistroDescIdDesc(usuario)
            .map(this::aRespuesta)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "El usuario " + usuarioId + " no tiene ubicacion registrada"));
    }

    /** Usa la hora del dispositivo si es razonable; si no se envia, la del servidor. */
    OffsetDateTime resolverFecha(OffsetDateTime fechaCaptura) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);

        if (fechaCaptura == null) {
            return ahora;
        }
        if (fechaCaptura.isAfter(ahora.plusSeconds(properties.toleranciaFuturoSeg()))) {
            throw new DatoInvalidoException("fechaCaptura no puede estar en el futuro");
        }
        if (fechaCaptura.isBefore(ahora.minusHours(properties.antiguedadMaximaHoras()))) {
            throw new DatoInvalidoException("fechaCaptura tiene mas de "
                + properties.antiguedadMaximaHoras() + " h de antiguedad");
        }

        return fechaCaptura;
    }

    private void validarPrecision(Double precisionMetros) {
        if (precisionMetros != null && precisionMetros > properties.precisionMaximaM()) {
            throw new DatoInvalidoException("Precision GPS insuficiente: " + precisionMetros.intValue()
                + " m (maximo " + properties.precisionMaximaM() + " m)");
        }
    }

    private UsuarioEntity buscarUsuario(int usuarioId) {
        return usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + usuarioId + " no encontrado"));
    }

    private Optional<ZonaVulnerableEntity> zonaQueContiene(double latitud, double longitud) {
        return zonaRepository.findAllByOrderByIdAsc().stream()
            .filter(zona -> PoligonoUtils.contiene(PoligonoUtils.desdeWkt(zona.getPoligono()), latitud, longitud))
            .findFirst();
    }

    private UbicacionRegistrada aRespuesta(UbicacionUsuarioEntity ubicacion) {
        ZonaVulnerableEntity zona = ubicacion.getZona();
        UbicacionRegistrada.Zona zonaInfo = null;
        List<UbicacionRegistrada.Alerta> alertas = List.of();

        if (zona != null) {
            ZonasTermicas.Zona catalogo = ZonasTermicas.getZona(zona.getId());
            String nombre = catalogo != null ? catalogo.getNombre() : String.valueOf(zona.getId());
            String nivel = zona.getNivelRiesgo() != null ? zona.getNivelRiesgo().getNombre() : null;
            String color = zona.getNivelRiesgo() != null ? zona.getNivelRiesgo().getColorHex() : null;
            zonaInfo = new UbicacionRegistrada.Zona(zona.getId(), nombre, nivel, color);

            alertas = alertaRepository
                .findByZonaIdAndVigenciaHastaAfterOrderByFechaEmisionDesc(zona.getId(), OffsetDateTime.now(clock))
                .stream()
                .map(alerta -> new UbicacionRegistrada.Alerta(alerta.getId(),
                    alerta.getNivelRiesgo().getNombre(), alerta.getMensaje(), alerta.getVigenciaHasta()))
                .toList();
        }

        return new UbicacionRegistrada(ubicacion.getId(), ubicacion.getUsuario().getId(),
            ubicacion.getLatitud(), ubicacion.getLongitud(), ubicacion.getPrecisionMetros(),
            ubicacion.getFechaRegistro(), zona != null, zonaInfo, alertas);
    }

    private static BigDecimal redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(DECIMALES, RoundingMode.HALF_UP);
    }
}
