package com.metroidwiki.network;

import com.metroidwiki.model.ArticuloRequest;
import com.metroidwiki.model.ArticuloResponse;
import com.metroidwiki.model.ArticulosListResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ArticuloClient {
    // Apuntamos al Gateway que nos redirigirá al microservicio de Artículos
    @POST("articulos")
    Call<ArticuloResponse> crearArticulo(
            @Header("Authorization") String token,
            @Body ArticuloRequest request
    );

    @GET("articulos")
    Call<ArticulosListResponse> obtenerArticulos();
}