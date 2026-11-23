package com.tecsup.clima.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Indice del fenomeno El Nino: promedio de los ultimos 3 meses del Niño 3.4 relativo,
 * publicado por NOAA/CPC (base para el RONI). Se guarda en memoria unas 6 horas.
 */
@Service
public class IndiceNinoService {

    private static final String FUENTE = "https://www.cpc.ncep.noaa.gov/data/indices/rel_mthsst9120.txt";
    private static final Duration VIGENCIA = Duration.ofHours(6);

    public record IndiceNino(double valor, String periodo, String clasificacion, String intensidad, String fuente) {
    }

    private final RestTemplate restTemplate = new RestTemplate();
    private IndiceNino cache;
    private Instant cacheHasta = Instant.EPOCH;

    public synchronized IndiceNino obtener() {
        if (cache != null && Instant.now().isBefore(cacheHasta)) {
            return cache;
        }
        try {
            String texto = restTemplate.getForObject(FUENTE, String.class);
            IndiceNino indice = interpretar(texto);
            if (indice != null) {
                cache = indice;
                cacheHasta = Instant.now().plus(VIGENCIA);
            }
            return indice != null ? indice : cache;
        } catch (Exception error) {
            return cache;
        }
    }

    static IndiceNino interpretar(String texto) {
        if (texto == null) {
            return null;
        }
        List<String[]> filas = new ArrayList<>();
        for (String linea : texto.split("\\R")) {
            String[] partes = linea.trim().split("\\s+");
            if (partes.length == 6 && partes[0].matches("\\d{4}")) {
                filas.add(partes);
            }
        }
        if (filas.size() < 3) {
            return null;
        }
        List<String[]> ultimas = filas.subList(filas.size() - 3, filas.size());
        double suma = 0;
        for (String[] fila : ultimas) {
            double valor = Double.parseDouble(fila[5]);
            if (valor < -90) {
                return null;
            }
            suma += valor;
        }
        double promedio = Math.round(suma / 3 * 10) / 10.0;
        String[] ultima = ultimas.get(2);
        String periodo = ultima[0] + "-" + String.format("%02d", Integer.parseInt(ultima[1]));
        return new IndiceNino(promedio, periodo, clasificar(promedio), intensidad(promedio), FUENTE);
    }

    static String clasificar(double valor) {
        if (valor >= 0.5) {
            return "El Niño";
        }
        if (valor <= -0.5) {
            return "La Niña";
        }
        return "Neutral";
    }

    static String intensidad(double valor) {
        double absoluto = Math.abs(valor);
        if (absoluto < 0.5) {
            return "";
        }
        if (absoluto < 1.0) {
            return "débil";
        }
        if (absoluto < 1.5) {
            return "moderado";
        }
        if (absoluto < 2.0) {
            return "fuerte";
        }
        return "muy fuerte";
    }
}
