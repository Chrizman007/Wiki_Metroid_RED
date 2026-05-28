package com.metroidwiki.model;

public class ArticuloRequest {
    private String titulo;
    private String categoria;
    private String descripcion;
    private String contenido;
    private String imagen; // Nuevo campo añadido

    public ArticuloRequest(String titulo, String categoria, String descripcion, String contenido, String imagen) {
        this.titulo = titulo;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.contenido = contenido;
        this.imagen = imagen;
    }

    public String getTitulo() { return titulo; }
    public String getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public String getContenido() { return contenido; }
    public String getImagen() { return imagen; }
}