package com.tecsup.clima.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tecsup.clima.persistence.RegistroTsmEntity;
import com.tecsup.clima.persistence.RegistroTsmRepository;
import com.tecsup.clima.persistence.ZonaVulnerableRepository;
import com.tecsup.clima.web.AdminHistoryResponse;

@ExtendWith(MockitoExtension.class)
class AdminHistoryServiceTest {
    @Mock private RegistroTsmRepository registros;
    @Mock private ZonaVulnerableRepository zonas;
    @InjectMocks private AdminHistoryService service;

    @Test
    void ultimasLecturasRetornaTresLecturasEnOrdenCronologico() {
        when(zonas.existsById(1)).thenReturn(true);
        when(registros.findTop3ByEstacion_IdOrderByFechaObservacionDesc(1)).thenReturn(List.of(
            reading("2026-01-03T12:00:00", "1.5"),
            reading("2026-01-02T12:00:00", "1.0"),
            reading("2026-01-01T12:00:00", "0.5")
        ));

        var result = service.ultimasLecturas(1);

        assertEquals(List.of("2026-01-01T12:00", "2026-01-02T12:00", "2026-01-03T12:00"),
            result.puntos().stream().map(AdminHistoryResponse.PuntoHistorial::fecha).toList());
        assertEquals(List.of(24.5, 25.0, 25.5),
            result.puntos().stream().map(AdminHistoryResponse.PuntoHistorial::valor).toList());
        assertTrue(result.puntos().size() == 3);
    }

    private RegistroTsmEntity reading(String dateTime, String anomaly) {
        RegistroTsmEntity reading = new RegistroTsmEntity();
        reading.setFechaObservacion(LocalDateTime.parse(dateTime));
        reading.setAnomalia(new BigDecimal(anomaly));
        return reading;
    }
}
