package com.tecsup.clima.service;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tecsup.clima.domain.RegistroClimaDiario;
import com.tecsup.clima.domain.ResumenClima;

/**
 * Obtiene el clima historico de una coordenada, lo filtra por meses de temporada
 * y calcula sus promedios.
 */
@Service
public class HistoricoClimaService {

    private final HistoricoClimaClient cliente;

    public HistoricoClimaService(HistoricoClimaClient cliente) {
        this.cliente = cliente;
    }

    public List<RegistroClimaDiario> obtener(double latitud, double longitud,
                                             LocalDate desde, LocalDate hasta, Set<Integer> meses) {
        List<RegistroClimaDiario> registros = cliente.consultar(latitud, longitud, desde, hasta);
        return filtrarPorMeses(registros, meses);
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
        OptionalDouble media = valores.stream()
            .filter(valor -> valor != null)
            .mapToDouble(Double::doubleValue)
            .average();
        return media.isPresent() ? media.getAsDouble() : null;
    }
}
