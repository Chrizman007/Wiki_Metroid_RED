package com.metroidwiki.model;

public class ComentarioResponse {
    private String message;
    private ComentarioDTO comentario;

    public ComentarioResponse() {
        // Constructor vacío requerido obligatoriamente para la deserialización JSON de Retrofit
    }

    public String getMessage() { return message; }
    public ComentarioDTO getComentario() { return comentario; }

    public void setMessage(String message) { this.message = message; }
    public void setComentario(ComentarioDTO comentario) { this.comentario = comentario; }
}
