package com.metroidwiki.network;

import com.metroidwiki.model.AuthResponse;
import com.metroidwiki.model.LoginRequest;
import retrofit2.Call;
import com.metroidwiki.model.RegistroRequest;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthClient {

    @POST("auth/login")
    Call<AuthResponse> iniciarSesion(@Body LoginRequest loginRequest);

    @POST("auth/registro")
    Call<AuthResponse> registrarUsuario(@Body RegistroRequest registroRequest);
}