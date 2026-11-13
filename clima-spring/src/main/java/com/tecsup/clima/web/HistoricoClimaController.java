package com.tecsup.clima.web;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tecsup.clima.config.WeatherProperties;
import com.tecsup.clima.domain.HistoricoClima;
import com.tecsup.clima.domain.RegistroClimaDiario;
import com.tecsup.clima.domain.ResumenClima;
import com.tecsup.clima.service.DatoInvalidoException;
import com.tecsup.clima.service.HistoricoClimaService;

/**
 * Clima historico (temperatura y lluvia) de una coordenada, filtrado por meses de temporada.
 * Ej: GET /api/historico/clima?desde=2020-01-01&hasta=2023-12-31&meses=12,1,2,3,4
 */
@RestController
@RequestMapping("/api/historico/clima")
public class HistoricoClimaController {

    private final HistoricoClimaService servicio;
    private final WeatherProperties propiedades;

    public HistoricoClimaController(HistoricoClimaService servicio, WeatherProperties propiedades) {
        this.servicio = servicio;
        this.propiedades = propiedades;
    }

    @GetMapping
    public ResponseEntity<RespuestaHistorico> consultar(
            @RequestParam(required = false) Double latitud,
            @RequestParam(required = false) Double longitud,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String meses) {

        double lat = latitud != null ? latitud : propiedades.getLatitude();
        double lon = longitud != null ? longitud : propiedades.getLongitude();
        Set<Integer> mesesFiltro = parsearMeses(meses);

        HistoricoClima historico = servicio.consultar(lat, lon, desde, hasta, mesesFiltro);

        return ResponseEntity.ok(new RespuestaHistorico(lat, lon, desde, hasta, mesesFiltro,
            historico.resumen(), historico.registros()));
    }

    /** "12,1,2" -> {12, 1, 2}. Vacio o ausente -> null (sin filtro). */
    static Set<Integer> parsearMeses(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Arrays.stream(texto.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        } catch (NumberFormatException error) {
            throw new DatoInvalidoException("meses debe ser una lista de numeros separados por coma, ej: 12,1,2");
        }
    }

    public record RespuestaHistorico(double latitud,
                                     double longitud,
                                     LocalDate desde,
                                     LocalDate hasta,
                                     Set<Integer> meses,
                                     ResumenClima resumen,
                                     List<RegistroClimaDiario> registros) {
    }
}
