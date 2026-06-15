package com.metroidwiki.network;

import com.metroidwiki.model.ArticuloRequest;
import com.metroidwiki.model.ArticuloResponse;
import com.metroidwiki.model.ArticulosListResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ArticuloClient {
    // Apuntamos al Gateway que nos redirigirá al microservicio de Artículos
    @POST("articulos")
    Call<ArticuloResponse> crearArticulo(
            @Header("Authorization") String token,
            @Body ArticuloRequest request
    );

    @GET("articulos")
    Call<ArticulosListResponse> obtenerArticulos();

    @GET("articulos/{id}")
    Call<ArticuloResponse> obtenerArticulo(@Path("id") String id, @Header("Authorization") String token);
}