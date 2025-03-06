package com.exemple.applicationble;



import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {
    @GET("gps/{ps_mod_UUID}") // Le chemin relatif vers le fichier JSON
    Call<List<VeloData>> getVeloData(@Path("ps_mod_UUID") String psModUUID);

   @POST("gps") // Pour envoyer des données
   Call<VeloData> postVeloData(@Body VeloData veloData);

    @PUT("velo/vole/{ps_mod_UUID}") // {ps_mod_UUID} est un paramètre dynamique
    Call<Velostatus> postVelostatus(@Path("ps_mod_UUID") String psModUuid, @Body Velostatus velostatus);

    @PUT("velo/retrouve/{ps_mod_UUID}") // {ps_mod_UUID} est un paramètre dynamique
    Call<Velostatustrue> postVelostatustrue(@Path("ps_mod_UUID") String psModUid, @Body Velostatustrue Velostatustrue);

    @GET("velo/vole") // Pour envoyer des données
    Call<List<Velostatus>> getVelostatus();


    @POST("register2")
        // Pour envoyer des données
    Call<UsersData> postUsersData(@Body UsersData userdata);

    @POST("/reset-password/question")
        // Pour envoyer des données
    Call<Resetpsw> postNewpsw(@Body Resetpsw resetpsw);

    @GET("users") // Pour envoyer des données
    Call<List<Usersdata2>> getUsersData();

    @POST("login") // Remplacez "login" par le chemin attendu par votre serveur
    Call<LoginData> loginUser(@Body LoginData loginData);

    @GET("secret-questions")
    Call<List<SecretQuestion>> getsecretquestion();

}