package com.tecsup.clima.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tecsup.clima.config.UbicacionProperties;
import com.tecsup.clima.domain.PoligonoUtils;
import com.tecsup.clima.domain.ZonasTermicas;
import com.tecsup.clima.persistence.AlertaPreventivaEntity;
import com.tecsup.clima.persistence.AlertaPreventivaRepository;
import com.tecsup.clima.persistence.NivelRiesgoEntity;
import com.tecsup.clima.persistence.UbicacionUsuarioEntity;
import com.tecsup.clima.persistence.UbicacionUsuarioRepository;
import com.tecsup.clima.persistence.UsuarioEntity;
import com.tecsup.clima.persistence.UsuarioRepository;
import com.tecsup.clima.persistence.ZonaVulnerableEntity;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;

class UbicacionServiceTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 10, 27, 15, 0, 0, 0, ZoneOffset.UTC);

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final UbicacionUsuarioRepository ubicacionRepository = mock(UbicacionUsuarioRepository.class);
    private final ZonaVulnerableRepository zonaRepository = mock(ZonaVulnerableRepository.class);
    private final AlertaPreventivaRepository alertaRepository = mock(AlertaPreventivaRepository.class);

    private final UbicacionService service = new UbicacionService(usuarioRepository, ubicacionRepository,
        zonaRepository, alertaRepository, new UbicacionProperties(1000, 24, 300),
        Clock.fixed(Instant.from(AHORA), ZoneOffset.UTC));

    private final UsuarioEntity usuario = new UsuarioEntity();
    private final ZonaVulnerableEntity paita = new ZonaVulnerableEntity();

    @BeforeEach
    void configurar() {
        usuario.setId(1);

        NivelRiesgoEntity rojo = new NivelRiesgoEntity();
        rojo.setNombre("ROJO");
        rojo.setColorHex("#EF4444");
        paita.setId(3);
        paita.setNivelRiesgo(rojo);
        paita.setPoligono(PoligonoUtils.aWkt(ZonasTermicas.getZona(3).getPoligono()));

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(zonaRepository.findAllByOrderByIdAsc()).thenReturn(List.of(paita));
        when(ubicacionRepository.save(any())).thenAnswer(invocacion -> {
            UbicacionUsuarioEntity ubicacion = invocacion.getArgument(0);
            ubicacion.setId(5);
            return ubicacion;
        });
    }

    @Test
    void guardaLasCoordenadasConLaZonaYLaHoraDelServidor() {
        AlertaPreventivaEntity alerta = new AlertaPreventivaEntity();
        alerta.setId(9);
        alerta.setNivelRiesgo(paita.getNivelRiesgo());
        alerta.setMensaje("Evacue");
        alerta.setVigenciaHasta(AHORA.plusHours(3));
        when(alertaRepository.findByZonaIdAndVigenciaHastaAfterOrderByFechaEmisionDesc(3, AHORA))
            .thenReturn(List.of(alerta));

        UbicacionRegistrada registrada = service.registrar(1, -5.2500004, -81.6, 12.0, null);

        assertEquals(5, registrada.id());
        assertEquals(new BigDecimal("-5.250000"), registrada.latitud());
        assertEquals(new BigDecimal("-81.600000"), registrada.longitud());
        assertEquals(AHORA, registrada.fechaRegistro());
        assertTrue(registrada.enZonaVulnerable());
        assertEquals("Piura Sur (Paita)", registrada.zona().nombre());
        assertEquals("ROJO", registrada.zona().nivelRiesgo());
        assertEquals(1, registrada.alertasVigentes().size());
        assertEquals("Evacue", registrada.alertasVigentes().get(0).mensaje());
    }

    @Test
    void fueraDeLasZonasGuardaSinZonaYUsaLaFechaDelDispositivo() {
        OffsetDateTime captura = AHORA.minusMinutes(2);

        UbicacionRegistrada registrada = service.registrar(1, -12.05, -77.04, null, captura);

        assertFalse(registrada.enZonaVulnerable());
        assertNull(registrada.zona());
        assertTrue(registrada.alertasVigentes().isEmpty());
        assertEquals(captura, registrada.fechaRegistro());
    }

    @Test
    void rechazaFechaFuturaOMuyAntigua() {
        assertThrows(DatoInvalidoException.class, () -> service.resolverFecha(AHORA.plusMinutes(10)));
        assertThrows(DatoInvalidoException.class, () -> service.resolverFecha(AHORA.minusHours(25)));
        assertEquals(AHORA.plusSeconds(60), service.resolverFecha(AHORA.plusSeconds(60)));
    }

    @Test
    void rechazaPrecisionInsuficienteSinGuardarNada() {
        assertThrows(DatoInvalidoException.class, () -> service.registrar(1, -5.2, -81.6, 5000.0, null));
        verify(ubicacionRepository, never()).save(any());
    }

    @Test
    void usuarioInexistenteOSinUbicacionesDa404() {
        when(usuarioRepository.findById(42)).thenReturn(Optional.empty());
        when(ubicacionRepository.findFirstByUsuarioOrderByFechaRegistroDescIdDesc(usuario))
            .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.registrar(42, -5.2, -81.6, null, null));
        assertThrows(RecursoNoEncontradoException.class, () -> service.ultima(1));
    }
}
