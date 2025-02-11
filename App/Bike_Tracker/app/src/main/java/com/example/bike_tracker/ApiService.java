package com.example.bike_tracker;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import java.util.List;

// Retrofit interface for interacting with the backend API
public interface ApiService {

    // Fetch user details by email
    @GET("api/users/{email}")
    Call<User> getUserByEmail(@Path("email") String email);

    // Fetch bike details by UUID
    @GET("api/bikes/{uuid}")
    Call<Bike> getBikeByUUID(@Path("uuid") String uuid);

    // Update bike status (e.g., stolen or normal)
    @PUT("api/bikes/{uuid}/status")
    Call<Void> updateBikeStatus(@Path("uuid") String uuid, @Body BikeStatusUpdate statusUpdate);

    // Fetch bike location history by UUID
    @GET("api/bikes/{uuid}/locations")
    Call<List<Location>> getBikeLocations(@Path("uuid") String uuid);

    // Data model for updating bike status
    class BikeStatusUpdate {
        private boolean statut;

        public BikeStatusUpdate(boolean statut) {
            this.statut = statut;
        }

        public boolean isStatut() {
            return statut;
        }

        public void setStatut(boolean statut) {
            this.statut = statut;
        }
    }

    // Data model for user details
    class User {
        private int id;
        private String UUID_velo;
        private String nom;
        private String prenom;
        private String email;

        // Getters and setters
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUUID_velo() {
            return UUID_velo;
        }

        public void setUUID_velo(String UUID_velo) {
            this.UUID_velo = UUID_velo;
        }

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public String getPrenom() {
            return prenom;
        }

        public void setPrenom(String prenom) {
            this.prenom = prenom;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // Data model for bike details
    class Bike {
        private String UUID;
        private int user_id;
        private boolean statut;
        private String gps;

        // Getters and setters
        public String getUUID() {
            return UUID;
        }

        public void setUUID(String UUID) {
            this.UUID = UUID;
        }

        public int getUser_id() {
            return user_id;
        }

        public void setUser_id(int user_id) {
            this.user_id = user_id;
        }

        public boolean isStatut() {
            return statut;
        }

        public void setStatut(boolean statut) {
            this.statut = statut;
        }

        public String getGps() {
            return gps;
        }

        public void setGps(String gps) {
            this.gps = gps;
        }
    }

    // Data model for bike location history
    class Location {
        private String gps;
        private String timestamp;

        // Getters and setters
        public String getGps() {
            return gps;
        }

        public void setGps(String gps) {
            this.gps = gps;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}
