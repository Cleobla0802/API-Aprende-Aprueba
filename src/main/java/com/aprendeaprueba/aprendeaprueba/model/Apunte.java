package com.aprendeaprueba.aprendeaprueba.model;

public class Apunte {
    private String id;
    private String titulo;
    private String contenido; // Antes era contenidoDigitalizado
    private String url;       // Antes era urlImagenOriginal
    private String fecha;     // Antes era fechaCreacion
    private String userId;    // NUEVO: Para el requisito de privacidad
    private String categoria; // NUEVO: Para el filtrado por asignatura

    // Constructor vacío obligatorio para Firebase
    public Apunte() {}

    // Constructor completo (Útil para crear el objeto rápidamente)
    public Apunte(String id, String titulo, String contenido, String url, String fecha, String userId, String categoria) {
        this.id = id;
        this.titulo = titulo;
        this.contenido = contenido;
        this.url = url;
        this.fecha = fecha;
        this.userId = userId;
        this.categoria = categoria;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}