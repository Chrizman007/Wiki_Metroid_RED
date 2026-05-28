package com.metroidwiki.model;

public class LoginRequest {
    private String correo;
    private String password;

    public LoginRequest(String correo, String password) {
        this.correo = correo;
        this.password = password;
    }

    // Getters
    public String getCorreo() { return correo; }
    public String getPassword() { return password; }
}
