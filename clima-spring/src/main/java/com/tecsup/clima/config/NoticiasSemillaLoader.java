package com.tecsup.clima.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.tecsup.clima.persistence.NoticiaEntity;
import com.tecsup.clima.persistence.NoticiaEntity.Estado;
import com.tecsup.clima.persistence.NoticiaEntity.Origen;
import com.tecsup.clima.persistence.NoticiaRepository;

/**
 * Noticias de ejemplo de "Noticias para ti". Al iniciar crea las que falten y
 * actualiza el contenido de las existentes; no cambia su estado de moderacion.
 */
@Component
public class NoticiasSemillaLoader implements CommandLineRunner {

    private static final OffsetDateTime FECHA_EJEMPLO =
        OffsetDateTime.of(2026, 11, 21, 12, 0, 0, 0, ZoneOffset.of("-05:00"));

    private final NoticiaRepository repository;

    public NoticiasSemillaLoader(NoticiaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        sembrar("Pronóstico: cielo despejado en la costa central y norte",
            "Se espera un clima estable con temperaturas entre 24 °C y 30 °C con vientos moderados.",
            "Se prevé un clima estable en la costa central y norte durante los próximos días, con temperaturas máximas entre 24 °C y 30 °C.\n\n"
            + "Los vientos serán moderados en el litoral. Se recomienda precaución a pescadores y navegantes artesanales al salir al mar.\n\n"
            + "Revisa los pronósticos actualizados antes de planificar actividades en playas y zonas costeras.",
            "Pronóstico", null);
        sembrar("Se activa alerta de riesgo alto en varias zonas costeras",
            "Se recomienda tomar precauciones ante el incremento de la temperatura del mar y posibles oleajes anómalos.",
            "El incremento de la temperatura del mar y los posibles oleajes anómalos elevan el riesgo en varias zonas costeras.\n\n"
            + "Se recomienda evitar el ingreso a playas rocosas y acantilados, y seguir las indicaciones de las autoridades locales.\n\n"
            + "Si observas oleaje fuerte, cambios bruscos en el mar o daños en el litoral, repórtalo a las autoridades de tu distrito.",
            "Alerta climática", null);
        sembrar("Monitoreo constante de variaciones climáticas en la capital",
            "Autoridades locales e instituciones meteorológicas refuerzan la vigilancia en distritos costeros.",
            "Autoridades locales e instituciones meteorológicas refuerzan la vigilancia en distritos costeros ante cambios de temperatura.\n\n"
            + "El monitoreo se realiza de forma constante y los resultados se comunicarán en los próximos boletines.\n\n"
            + "Puedes revisar el nivel de riesgo de tu ubicación en el mapa de este panel.",
            "Actualidad", "Lima");
    }

    private void sembrar(String titulo, String resumen, String contenido, String categoria, String departamento) {
        NoticiaEntity noticia = repository.findFirstByTitulo(titulo).orElseGet(() -> {
            NoticiaEntity nueva = new NoticiaEntity();
            nueva.setTitulo(titulo);
            nueva.setOrigen(Origen.AUTOMATICA);
            nueva.setEstado(Estado.APROBADA);
            return nueva;
        });
        noticia.setResumen(resumen);
        noticia.setContenido(contenido);
        noticia.setCategoria(categoria);
        noticia.setDepartamento(departamento);
        noticia.setFechaCreacion(FECHA_EJEMPLO);
        repository.save(noticia);
    }
}
