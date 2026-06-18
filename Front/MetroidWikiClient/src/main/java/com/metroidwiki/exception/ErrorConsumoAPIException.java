package com.metroidwiki.exception;

public class ErrorConsumoAPIException extends Exception {
    private final int codigoRespuesta;

    public ErrorConsumoAPIException(String mensaje, int codigoRespuesta) {
        super(mensaje);
        this.codigoRespuesta = codigoRespuesta;
    }

    public int getCodigoRespuesta() {
        return codigoRespuesta;
    }
}
