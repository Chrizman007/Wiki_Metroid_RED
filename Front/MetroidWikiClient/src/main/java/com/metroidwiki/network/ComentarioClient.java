package com.metroidwiki.network;

import com.metroidwiki.model.ComentarioRequest;
import com.metroidwiki.model.ComentarioResponse;
import com.metroidwiki.model.ComentariosListResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE; // 🛠️ NUEVO: Importamos DELETE
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ComentarioClient {
    // Obtener comentarios de un artículo
    @GET("comentarios/{articuloId}")
    Call<ComentariosListResponse> obtenerComentarios(
            @Path("articuloId") String articuloId,
            @Query("limit") int limit,
            @Query("skip") int skip
    );

    // Crear un nuevo comentario
    @POST("comentarios/{articuloId}")
    Call<ComentarioResponse> crearComentario(
            @Path("articuloId") String articuloId,
            @Header("Authorization") String token,
            @Body ComentarioRequest request
    );

    // Eliminar un comentario (Moderación)
    @DELETE("comentarios/{id}")
    Call<Void> eliminarComentario(
            @Path("id") String idComentario,
            @Header("Authorization") String token
    );
}