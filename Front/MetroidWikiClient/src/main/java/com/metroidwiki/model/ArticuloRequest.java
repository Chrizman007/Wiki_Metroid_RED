package com.metroidwiki.model;

public class ArticuloRequest {
    private String titulo;
    private String categoria;
    private String descripcion;
    private String contenido;
    private String imagen;
    private String estado; // 🛠️ Campo de estado añadido

    // 🛠️ CONSTRUCTOR 1 (5 parámetros): Usado por NuevoArticuloFrame (Creación)
    public ArticuloRequest(String titulo, String categoria, String descripcion, String contenido, String imagen) {
        this.titulo = titulo;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.contenido = contenido;
        this.imagen = imagen;
    }

    // 🛠️ CONSTRUCTOR 2 (6 parámetros): Usado por EditarArticuloFrame (Actualización)
    public ArticuloRequest(String titulo, String categoria, String descripcion, String contenido, String imagen, String estado) {
        this.titulo = titulo;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.contenido = contenido;
        this.imagen = imagen;
        this.estado = estado;
    }

    // Getters
    public String getTitulo() { return titulo; }
    public String getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public String getContenido() { return contenido; }
    public String getImagen() { return imagen; }
    public String getEstado() { return estado; }

    // Setters (Especialmente importante el setEstado para Retrofit)
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public void setEstado(String estado) { this.estado = estado; }
}