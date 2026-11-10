package com.tecsup.clima.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tecsup.clima.domain.HistoricoClima;
import com.tecsup.clima.domain.RegistroClimaDiario;
import com.tecsup.clima.domain.ResumenClima;

/**
 * Obtiene el clima historico de una coordenada, lo filtra por meses de temporada
 * y calcula sus promedios.
 */
@Service
public class HistoricoClimaService {

    /** Limite de dias por consulta (aprox. 10 anios) para no pedir series demasiado grandes. */
    static final int MAX_DIAS = 3650;

    private final HistoricoClimaClient cliente;

    public HistoricoClimaService(HistoricoClimaClient cliente) {
        this.cliente = cliente;
    }

    public HistoricoClima consultar(double latitud, double longitud,
                                    LocalDate desde, LocalDate hasta, Set<Integer> meses) {
        validar(latitud, longitud, desde, hasta, meses);
        List<RegistroClimaDiario> registros = filtrarPorMeses(
            cliente.consultar(latitud, longitud, desde, hasta), meses);
        return new HistoricoClima(registros, resumir(registros));
    }

    static void validar(double latitud, double longitud, LocalDate desde, LocalDate hasta, Set<Integer> meses) {
        if (latitud < -90 || latitud > 90) {
            throw new DatoInvalidoException("latitud debe estar entre -90 y 90");
        }
        if (longitud < -180 || longitud > 180) {
            throw new DatoInvalidoException("longitud debe estar entre -180 y 180");
        }
        if (desde == null || hasta == null) {
            throw new DatoInvalidoException("desde y hasta son obligatorios");
        }
        if (desde.isAfter(hasta)) {
            throw new DatoInvalidoException("desde no puede ser posterior a hasta");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAX_DIAS) {
            throw new DatoInvalidoException("el rango no puede superar " + MAX_DIAS + " dias");
        }
        if (meses != null && meses.stream().anyMatch(mes -> mes < 1 || mes > 12)) {
            throw new DatoInvalidoException("los meses deben estar entre 1 y 12");
        }
    }

    /** Conserva los dias cuyo mes esta en {@code meses}. Si {@code meses} es null o vacio, no filtra. */
    static List<RegistroClimaDiario> filtrarPorMeses(List<RegistroClimaDiario> registros, Set<Integer> meses) {
        if (meses == null || meses.isEmpty()) {
            return registros;
        }
        return registros.stream()
            .filter(registro -> meses.contains(registro.fecha().getMonthValue()))
            .toList();
    }

    /** Calcula promedios de temperatura (ignorando nulls) y suma la lluvia del periodo. */
    static ResumenClima resumir(List<RegistroClimaDiario> registros) {
        Double maxPromedio = promedio(registros.stream().map(RegistroClimaDiario::temperaturaMaxC).toList());
        Double minPromedio = promedio(registros.stream().map(RegistroClimaDiario::temperaturaMinC).toList());
        Double lluviaTotal = registros.stream()
            .map(RegistroClimaDiario::precipitacionMm)
            .filter(valor -> valor != null)
            .mapToDouble(Double::doubleValue)
            .sum();
        boolean hayLluvia = registros.stream().anyMatch(r -> r.precipitacionMm() != null);

        return new ResumenClima(
            registros.size(),
            maxPromedio,
            minPromedio,
            hayLluvia ? lluviaTotal : null
        );
    }

    private static Double promedio(List<Double> valores) {
        java.util.OptionalDouble media = valores.stream()
            .filter(valor -> valor != null)
            .mapToDouble(Double::doubleValue)
            .average();
        return media.isPresent() ? media.getAsDouble() : null;
    }
}
