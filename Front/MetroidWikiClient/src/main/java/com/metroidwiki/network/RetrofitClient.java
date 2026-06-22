package com.metroidwiki.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.metroidwiki.exception.ErrorConsumoAPIException;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    public static final String BASE_URL = "http://18.224.252.1:3000";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();

            try {
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            } catch (IllegalArgumentException e) {
                System.err.println("Error de configuración: La URL base no es válida.");
                throw e;
            }
        }
        return retrofit;
    }

    public static <T> void procesarRespuestaError(Response<T> response) throws ErrorConsumoAPIException {
        int codigo = response.code();
        String errorBackend = "ErrorDesconocido";
        String mensaje = "Ocurrió un fallo al comunicarse con el servidor.";

        if (response.errorBody() != null) {
            try {
                String cuerpoError = response.errorBody().string();
                
                JsonObject jsonObject = new Gson().fromJson(cuerpoError, JsonObject.class);
                
                if (jsonObject != null) {
                    if (jsonObject.has("error")) {
                        errorBackend = jsonObject.get("error").getAsString();
                    }
                    if (jsonObject.has("message")) {
                        mensaje = jsonObject.get("message").getAsString();
                    }
                }
            } catch (Exception e) { 
                mensaje = "Error al decodificar la respuesta del servidor: " + e.getMessage();
            }
        }

        throw new ErrorConsumoAPIException(mensaje, codigo, errorBackend);
    }
}