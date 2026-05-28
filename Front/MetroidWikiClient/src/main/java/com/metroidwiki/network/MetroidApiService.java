package com.metroidwiki.network;

import com.metroidwiki.model.ArticuloDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface MetroidApiService {
    @GET("articulos/{id}")
    Call<ArticuloDTO> obtenerArticuloPorId(@Path("id") String id);

    @POST("articulos")
    Call<Object> crearArticulo(@Body ArticuloDTO nuevoArticulo);
}