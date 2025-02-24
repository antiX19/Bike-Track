package com.exemple.applicationble;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import java.util.List;

public interface ApiService {
    @GET("gps") // Le chemin relatif vers le fichier JSON
    Call<List<VeloData>> getVeloData();

    @POST("gps") // Pour envoyer des données
    Call<VeloData> postVeloData(@Body VeloData veloData);

    @POST("register") // Pour envoyer des données
    Call<UsersData> postUsersData(@Body UsersData userdata);

}