package com.exemple.applicationble;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedDispatcherOwner;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
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
    static String identifier;
    static String ps_mod_UUID;
    static String ps_mod_pin;
    static int ps_mod_Id;
    static String ps_mod_name;
    private EditText editTextEmailOrPseudo, editTextPassword;
    public static final String BASE_URL = "http://13.36.126.63:3000/";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "remember_me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmailOrPseudo = findViewById(R.id.etEmailOrUsername);
        editTextPassword = findViewById(R.id.etPassword);
        Button buttonConnect = findViewById(R.id.btnLogin);
        CheckBox cbRememberMe = findViewById(R.id.cbRememberMe);
        ImageView backButton = findViewById(R.id.backButton);
        // Charger les préférences sauvegardées
        loadPreferences(editTextEmailOrPseudo, editTextPassword, cbRememberMe);

        buttonConnect.setOnClickListener(view -> {
            String emailOrPseudo = editTextEmailOrPseudo.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (emailOrPseudo.isEmpty() || password.isEmpty()) {
                Toast.makeText(ConnexionActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidEmail(emailOrPseudo) && !isValidUsername(emailOrPseudo)) {
                Toast.makeText(ConnexionActivity.this, "Veuillez entrer un email ou un nom d'utilisateur valide", Toast.LENGTH_SHORT).show();
                return;
            }
            // Sauvegarder les préférences si la case est cochée
            if (cbRememberMe.isChecked()) {
                savePreferences(emailOrPseudo, password,true);
            } else {
                savePreferences("", "",false); // Effacer les préférences si décoché
            }

            // Appel Retrofit pour valider les identifiants
            Retrofit retrofit = getRetrofitWithCertificate();
            ApiService apiService = retrofit.create(ApiService.class);
            LoginData loginData = new LoginData(emailOrPseudo, password);

            apiService.loginUser(loginData).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<LoginData> call, Response<LoginData> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(ConnexionActivity.this, "Identifiants validés", Toast.LENGTH_SHORT).show();

                        // Récupérer les données utilisateur
                        apiService.getUsersData().enqueue(new Callback<>() {
                            @Override
                            public void onResponse(Call<List<Usersdata2>> call, Response<List<Usersdata2>> response) {
                                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                    List<Usersdata2> usersDataList = response.body();
                                    boolean found = false;

                                    for (Usersdata2 data : usersDataList) {
                                        if ((data.getPseudo() != null && data.getPseudo().equals(emailOrPseudo)) ||
                                                (data.getEmail() != null && data.getEmail().equals(emailOrPseudo))) {
                                            ps_mod_name = data.getBLE_name();
                                            ps_mod_pin = data.getBLE_Pin();
                                            ps_mod_UUID = data.getUUID_velo();
                                            ps_mod_Id = data.getId();
                                            identifier = emailOrPseudo;
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (found) {
                                        Intent intent = new Intent(ConnexionActivity.this, HomePageActivity.class);
                                        startActivity(intent);
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
                                Toast.makeText(ConnexionActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(ConnexionActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginData> call, Throwable t) {
                    Toast.makeText(ConnexionActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        TextView textViewForgotPassword = findViewById(R.id.tvForgotPassword);
        textViewForgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(ConnexionActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            if (ConnexionActivity.this instanceof OnBackPressedDispatcherOwner) {
                OnBackPressedDispatcherOwner dispatcherOwner = (OnBackPressedDispatcherOwner) ConnexionActivity.this;
                dispatcherOwner.getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private Retrofit getRetrofitWithCertificate() {
        InputStream certInputStream = getResources().openRawResource(R.raw.selfsigned);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca = cf.generateCertificate(certInputStream);
            certInputStream.close();

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("selfsigned", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            OkHttpClient client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                    .hostnameVerifier((hostname, session) -> hostname.equals("13.36.126.63"))
                    .build();

            return new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Erreur de configuration SSL", e);
        }
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidUsername(String username) {
        return !TextUtils.isEmpty(username) && username.length() >= 3;
    }

    private void savePreferences(String identifier, String password, boolean rememberMe) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_IDENTIFIER, identifier);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }

    private void loadPreferences(EditText etEmailOrUsername, EditText etPassword, CheckBox cbRememberMe) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedIdentifier = sharedPreferences.getString(KEY_IDENTIFIER, "");
        String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        etEmailOrUsername.setText(savedIdentifier);
        etPassword.setText(savedPassword);
        cbRememberMe.setChecked(rememberMe);
    }



}
