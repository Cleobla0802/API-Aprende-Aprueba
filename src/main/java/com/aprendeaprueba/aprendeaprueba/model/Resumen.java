package com.aprendeaprueba.aprendeaprueba.model;

public class Resumen {
	private String id;
    private String titulo;
    private String contenido;
    private String categoria;
    private String fecha;
    private String userId;
    private String idApunteOriginal;

    public Resumen() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getIdApunteOriginal() { return idApunteOriginal; }
    public void setIdApunteOriginal(String idApunteOriginal) { this.idApunteOriginal = idApunteOriginal; }
}
