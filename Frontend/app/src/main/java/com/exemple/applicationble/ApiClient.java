package com.exemple.applicationble;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Base URL of your server
    private static final String BASE_URL = "http://your-server-url.com/"; // Replace with your actual server URL

    // Singleton instance of Retrofit
    private static Retrofit retrofit;

    /**
     * Returns a singleton instance of Retrofit.
     */
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // Set the base URL for API requests
                    .addConverterFactory(GsonConverterFactory.create()) // Add a converter for JSON serialization/deserialization
                    .build();
        }
        return retrofit;
    }

    /**
     * Returns an instance of ApiService.
     */
    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}
