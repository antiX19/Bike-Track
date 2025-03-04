package com.exemple.applicationble;

import static android.content.ContentValues.TAG;

import static com.exemple.applicationble.MainActivity.isLoggedIn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ConnexionActivity extends AppCompatActivity {
    public static String pseudo_conn;
    static  String ps_mod_UUID;

    static String ps_mod_pin;

    static int ps_mod_Id;
    String email;

    static String ps_mod_name;

    String emailOrPseudo;

    private EditText editTextEmailOrPseudo, editTextPassword;
    private Button buttonConnect, buttonBypass;  // Nouveau bouton bypass
    public static final String BASE_URL = "http://13.36.126.63:3000/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connexion);

        editTextEmailOrPseudo = findViewById(R.id.editTextEmailOrPseudo);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonConnect = findViewById(R.id.buttonConnect);
        buttonBypass = findViewById(R.id.buttonBypass);  // Liaison du nouveau boutonps_mod_Id

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailOrPseudo = editTextEmailOrPseudo.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (emailOrPseudo.isEmpty() || password.isEmpty()) {
                    Toast.makeText(ConnexionActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Définir pseudo_conn ou email en fonction de l'entrée
                email = "";
                if (emailOrPseudo.contains("@")) {
                    email = emailOrPseudo;
                    pseudo_conn = ""; // ou vous pouvez stocker l'email dans une variable dédiée
                } else {
                    pseudo_conn = emailOrPseudo;
                }

                Retrofit retrofit = getRetrofitWithCertificate();
                ApiService apiService = retrofit.create(ApiService.class);
                LoginData loginData = new LoginData(emailOrPseudo, password);

                // Premier appel : validation des identifiants
                apiService.loginUser(loginData).enqueue(new Callback<LoginData>() {
                    @Override
                    public void onResponse(Call<LoginData> call, Response<LoginData> response) {
                        // Vérifiez ici que la réponse indique bien que les identifiants sont corrects.
                        if (response.isSuccessful() && response.body() != null /* && vérifiez un champ d'état si disponible */) {
                            // Identifiants valides : maintenant, on peut lancer le second appel
                            Toast.makeText(ConnexionActivity.this, "Identifiants validés", Toast.LENGTH_SHORT).show();
                            // Ensuite, récupérer les infos utilisateur
                            apiService.getUsersData().enqueue(new Callback<List<Usersdata2>>() {
                                @Override
                                public void onResponse(Call<List<Usersdata2>> call, Response<List<Usersdata2>> response) {
                                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                        List<Usersdata2> usersDataList = response.body();
                                        boolean found = false;
                                        for (Usersdata2 data : usersDataList) {
                                            // Ici, vous devez utiliser le bon identifiant : pseudo_conn ou email, selon le cas.
                                            if ((data.getPseudo() != null || data.getEmail() != null) && (data.getPseudo().equals(pseudo_conn) || data.getEmail().equals(email))){
                                                ps_mod_name = data.getBLE_name();
                                                ps_mod_pin = data.getBLE_Pin();
                                                ps_mod_UUID = data.getUUID_velo();
                                                ps_mod_Id = data.getId();
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (found) {
                                            Intent intent = new Intent(ConnexionActivity.this, HomeActivity.class);
                                            startActivity(intent);
                                            saveLoginState();
                                            finish();
                                        } else {
                                            Toast.makeText(ConnexionActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(ConnexionActivity.this, "Erreur lors de la récupération des données utilisateur", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<List<Usersdata2>> call, Throwable t) {
                                    Toast.makeText(ConnexionActivity.this, "Erreur de connexion: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(ConnexionActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<LoginData> call, Throwable t) {
                        Toast.makeText(ConnexionActivity.this, "Erreur de connexion: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ConnexionActivity", "Erreur de connexion", t);
                    }
                });
            }
        });

        TextView textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Lancez l'activité de réinitialisation de mot de passe
                Intent intent = new Intent(ConnexionActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Crée et retourne une instance Retrofit configurée avec un OkHttpClient
     * qui accepte un certificat auto-signé contenu dans res/raw/selfsigned.cer.
     */
    private Retrofit getRetrofitWithCertificate() {
        /*InputStream certInputStream = getResources().openRawResource(R.raw.selfsigned);
        Certificate ca = null;
        KeyStore keyStore = null;
        CertificateFactory cf = null;
        SSLContext sslContext;
        TrustManagerFactory tmf;
        try {
            cf = CertificateFactory.getInstance("X.509");
            ca = cf.generateCertificate(certInputStream);
            certInputStream.close();
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            // Crée un keystore contenant le certificat
            keyStore.load(null, null);
            keyStore.setCertificateEntry("selfsigned", ca);

            // Crée un TrustManager qui utilise notre keystore
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Initialise un contexte SSL avec notre TrustManager
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
        } catch (CertificateException e) {
            Log.e("test", "Erreur de chargement du certificat", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.e("test", "Erreur de chargement du certificat", e);
            throw new RuntimeException(e);
        } catch (KeyStoreException | KeyManagementException e) {
            Log.e("test", "Erreur de chargement du certificat", e);
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            Log.e("test", "Erreur de chargement du certificat", e);
            throw new RuntimeException(e);
        }
        // Configure OkHttpClient avec ce contexte SSL et un hostname verifier personnalisé
        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                .hostnameVerifier((hostname, session) -> hostname.equals("13.36.126.63"))
                .build();*/
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
               // .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    // Nouvelle fonction pour sauvegarder l'état de connexion
    private void saveLoginState() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userPseudo", pseudo_conn);  // Sauvegarde du pseudo
        // Vous pouvez également sauvegarder d'autres informations (ex. pseudo ou email)
        editor.apply();
    }

}
