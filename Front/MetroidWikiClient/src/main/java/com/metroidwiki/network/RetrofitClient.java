package com.metroidwiki.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Es vital el slash '/' al final para que Retrofit no arroje IllegalArgumentException
    private static final String BASE_URL = "http://localhost:3000/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            try {
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            } catch (IllegalArgumentException e) {
                // Manejo específico si la URL está mal formateada
                System.err.println("Error de configuración: La URL base no es válida.");
                throw e;
            }
        }
        return retrofit;
    }
}