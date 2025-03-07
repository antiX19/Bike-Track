package com.exemple.applicationble;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateAccountActivity extends AppCompatActivity {

    private KeyPair keyPair; // Diffie-Hellman key pair
    private SecretKey sharedSecretKey; // Shared secret key for encryption

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accountsettings);
        // Step 1: Generate Diffie-Hellman key pair
        generateDiffieHellmanKeys();
        // Step 2: Send the public key to the server
        sendPublicKeyToServer();
    }
    /**
     * Generates a Diffie-Hellman key pair.
     */
    private void generateDiffieHellmanKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(2048); // Use a secure key size
            keyPair = keyPairGenerator.generateKeyPair();
            Log.d("DH", "Public key generated: " + Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT));
        } catch (Exception e) {
            Log.e("DH", "Error generating Diffie-Hellman keys", e);
        }
    }

    /**
     * Sends the public key to the server.
     */
    private void sendPublicKeyToServer() {
        if (keyPair == null) {
            Log.e("DH", "Key pair is null. Cannot send public key.");
            return;
        }

        String publicKeyEncoded = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);
        ApiService apiService = ApiClient.getApiService();

        apiService.sendPublicKey(publicKeyEncoded).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("DH", "Public key sent successfully");
                    // Step 3: Retrieve the server's public key
                    retrieveServerPublicKey();
                } else {
                    Log.e("DH", "Failed to send public key: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("DH", "Error sending public key", t);
            }
        });
    }

    /**
     * Retrieves the server's public key and establishes a shared secret key.
     */
    private void retrieveServerPublicKey() {
        ApiService apiService = ApiClient.getApiService();

        apiService.getServerPublicKey().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String serverPublicKeyEncoded = response.body();
                    Log.d("DH", "Server public key received: " + serverPublicKeyEncoded);
                    establishSharedSecret(serverPublicKeyEncoded);
                } else {
                    Log.e("DH", "Failed to retrieve server public key: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("DH", "Error retrieving server public key", t);
            }
        });
    }

    /**
     * Establishes a shared secret key using the server's public key.
     */
    private void establishSharedSecret(String serverPublicKeyEncoded) {
        try {
            byte[] serverPublicKeyBytes = Base64.decode(serverPublicKeyEncoded, Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            PublicKey serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(serverPublicKey, true);

            byte[] sharedSecret = keyAgreement.generateSecret();
            sharedSecretKey = new SecretKeySpec(sharedSecret, 0, 16, "AES"); // Use the first 16 bytes for AES
            Log.d("DH", "Shared secret key established successfully");

            // Save the shared secret key for later use
            saveSharedSecretKey();
        } catch (Exception e) {
            Log.e("DH", "Error establishing shared secret key", e);
        }
    }

    /**
     * Saves the shared secret key in SharedPreferences for later use.
     */
    private void saveSharedSecretKey() {
        if (sharedSecretKey == null) {
            Log.e("DH", "Shared secret key is null. Cannot save.");
            return;
        }

        String sharedKeyEncoded = Base64.encodeToString(sharedSecretKey.getEncoded(), Base64.DEFAULT);
        getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .edit()
                .putString("SHARED_SECRET_KEY", sharedKeyEncoded)
                .apply();

        Log.d("DH", "Shared secret key saved successfully");
    }
}
