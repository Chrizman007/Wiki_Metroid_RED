package com.metroidwiki.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    public static final String BASE_URL = "http://10.165.25.125:3000";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            try {
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            } catch (IllegalArgumentException e) {
                System.err.println("Error de configuración: La URL base no es válida.");
                throw e;
            }
        }
        return retrofit;
    }
}