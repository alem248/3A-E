package com.tecsup.clima.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tecsup.clima.config.WeatherProperties;
import com.tecsup.clima.domain.historico.ComparacionHistoricaResponse;
import com.tecsup.clima.domain.historico.RegistroHistoricoDto;

/** Calcula la desviación relativa entre hoy y un día del archivo histórico. */
@Service
public class ComparacionHistoricaService {

    private final SerieHistoricaService historicoService;
    private final WeatherProperties properties;

    public ComparacionHistoricaService(SerieHistoricaService historicoService, WeatherProperties properties) {
        this.historicoService = historicoService;
        this.properties = properties;
    }

    public ComparacionHistoricaResponse comparar(LocalDate fechaHistorica, Double latitud, Double longitud) {
        if (fechaHistorica == null || fechaHistorica.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha histórica no puede ser futura.");
        }
        double lat = latitud == null ? properties.getLatitude() : latitud;
        double lon = longitud == null ? properties.getLongitude() : longitud;

        Optional<Double> actual = historicoService.consultarMediaActual(lat, lon);
        Optional<RegistroHistoricoDto> registro = historicoService.consultarDia(fechaHistorica, lat, lon);
        Double temperaturaHistorica = registro.map(RegistroHistoricoDto::temperaturaMediaC).orElse(null);
        Double temperaturaActual = actual.orElse(null);

        if (temperaturaActual == null || temperaturaHistorica == null) {
            return new ComparacionHistoricaResponse(LocalDate.now(), fechaHistorica, lat, lon,
                temperaturaActual, temperaturaHistorica, null, null, false,
                "No hay datos suficientes para comparar ambas fechas.");
        }

        double diferencia = temperaturaActual - temperaturaHistorica;
        Double desviacion = temperaturaHistorica == 0
            ? null
            : redondear(diferencia / Math.abs(temperaturaHistorica) * 100.0);
        String mensaje = desviacion == null
            ? "La comparación está disponible, pero no se calcula porcentaje porque la media histórica es cero."
            : "Comparación disponible. La desviación porcentual es relativa a la media histórica.";

        return new ComparacionHistoricaResponse(LocalDate.now(), fechaHistorica, lat, lon,
            temperaturaActual, temperaturaHistorica, redondear(diferencia), desviacion, true, mensaje);
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
