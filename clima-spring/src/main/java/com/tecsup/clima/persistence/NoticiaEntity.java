package com.tecsup.clima.persistence;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Noticia climatica: manual (la escribe el admin) o automatica (feed a revisar). */
@Entity
@Table(name = "noticia")
public class NoticiaEntity {

    public enum Origen { MANUAL, AUTOMATICA }

    public enum Estado { PENDIENTE, APROBADA, DESCARTADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "resumen", nullable = false, length = 600)
    private String resumen;

    @Column(name = "categoria", length = 40)
    private String categoria;

    /** Departamento destino; null significa todos los departamentos. */
    @Column(name = "departamento", length = 80)
    private String departamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 20)
    private Origen origen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado;

    @Column(name = "contenido", length = 4000)
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    /** Foto opcional de la noticia (JPG, PNG o WEBP, maximo 2 MB). */
    @Column(name = "imagen", length = 2097152)
    private byte[] imagen;

    @Column(name = "imagen_tipo", length = 40)
    private String imagenTipo;

    public byte[] getImagen() { return imagen; }
    public void setImagen(byte[] imagen) { this.imagen = imagen; }
    public String getImagenTipo() { return imagenTipo; }
    public void setImagenTipo(String imagenTipo) { this.imagenTipo = imagenTipo; }

    public Integer getId() { return id; }
    public String getContenido() { return contenido; }

    public void setContenido(String contenido) { this.contenido = contenido; }

    public void setId(Integer id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public Origen getOrigen() { return origen; }
    public void setOrigen(Origen origen) { this.origen = origen; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
