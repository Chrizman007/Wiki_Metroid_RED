package com.metroidwiki.model;

public class AuthResponse {
    private String message;
    private String token;
    private String nombre;
    private String rol;

    public String getMessage() { return message; }
    public String getToken() { return token; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
}