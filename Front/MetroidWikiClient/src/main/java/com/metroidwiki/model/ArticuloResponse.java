package com.metroidwiki.model;

public class ArticuloResponse {
    private String message;
    private ArticuloDTO articulo; // 🛠️ APUNTA AL DTO GLOBAL CON TODOS SUS CAMPOS

    // Getters y Setters
    public String getMessage() { return message; }
    public ArticuloDTO getArticulo() { return articulo; }
}