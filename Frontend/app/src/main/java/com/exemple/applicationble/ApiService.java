package com.exemple.applicationble;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    @GET("gps/{ps_mod_UUID}") // Fetch GPS data for a specific UUID
    Call<List<VeloData>> getVeloData(@Path("ps_mod_UUID") String psModUUID);

    @POST("gps") // Send GPS data
    Call<VeloData> postVeloData(@Body VeloData veloData);

    @PUT("velo/vole/{ps_mod_UUID}") // Update status to "stolen" for a specific UUID
    Call<VeloStatus> postVelostatus(@Path("ps_mod_UUID") String psModUuid, @Body VeloStatus velostatus);

    @PUT("velo/retrouve/{ps_mod_UUID}") // Update status to "retrieved" for a specific UUID
    Call<VeloStatusTrue> postVelostatustrue(@Path("ps_mod_UUID") String psModUid, @Body VeloStatusTrue velostatustrue);

    @GET("velo/vole") // Fetch all "stolen" statuses
    Call<List<VeloStatus>> getVelostatus();

    @POST("register2") // Register a new user
    Call<UsersData> postUsersData(@Body UsersData userdata);

    @POST("/reset-password/question") // Reset password using a security question
    Call<ResetPswd> postNewpsw(@Body ResetPswd resetPswd);

    @GET("users") // Fetch all user data
    Call<List<UsersData2>> getUsersData();

    @POST("login") // Login a user
    Call<LoginData> loginUser(@Body LoginData loginData);

    @GET("secret-questions") // Fetch secret questions
    Call<List<SecretQuestion>> getsecretquestion();

    // New methods for Diffie-Hellman key exchange
    @POST("dh/public-key") // Send the public key to the server
    Call<Void> sendPublicKey(@Body String publicKey);

    @GET("dh/server-public-key") // Get the server's public key
    Call<String> getServerPublicKey();

    @POST("dh/encrypted-data") // Send encrypted data to the server
    Call<Void> sendEncryptedData(@Body String encryptedData);
}
