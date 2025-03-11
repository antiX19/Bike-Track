package com.exemple.applicationble;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView; // Ajouté pour le toggle
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ConnexionActivity extends AppCompatActivity {
    public static String pseudo_conn;
    static String ps_mod_UUID;
    static String token;
    static String ps_mod_pin;
    static int ps_mod_Id;
    private ImageView buttonBypass;
    private TextView creaccunt;
    String email;
    static String ps_mod_name;
    String emailOrPseudo;
    private EditText editTextEmailOrPseudo, editTextPassword;

    private CheckBox rememberme;

    private Button buttonConnect;  // Nouveau bouton de retour
    public static final String BASE_URL = "http://13.36.126.63:3000/";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connexion);

        editTextEmailOrPseudo = findViewById(R.id.editTextEmailOrPseudo);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonConnect = findViewById(R.id.buttonConnect);
        creaccunt = findViewById(R.id.createaccount);
        rememberme = findViewById(R.id.cbRememberMe);


        creaccunt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ConnexionActivity.this, IdentificationActivity.class);
                startActivity(intent);
                finish();
            }
        });




        rememberme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Appelle la fonction lorsque la checkbox est cochée
                    saveLoginState();
                }
                // Sinon, ne rien faire
            }
        });
        // Ajout du bouton de retour
        buttonBypass = findViewById(R.id.buttonBypass);
        buttonBypass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Retour à l'activité précédente
                finish();
            }
        });
        ImageView imageViewTogglePassword = findViewById(R.id.imageViewTogglePassword);
        imageViewTogglePassword.setOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Masquer le mot de passe
                    editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imageViewTogglePassword.setImageResource(R.drawable.ic_eye_off); // Votre icône pour "caché"
                    isPasswordVisible = false;
                } else {
                    // Afficher le mot de passe
                    editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imageViewTogglePassword.setImageResource(R.drawable.ic_eye_on); // Votre icône pour "visible"
                    isPasswordVisible = true;
                }
                // Remettre le curseur à la fin
                editTextPassword.setSelection(editTextPassword.getText().length());
            }
        });

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
                    pseudo_conn = "";
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
                        if (response.isSuccessful() && response.body() != null) {
                           // Toast.makeText(ConnexionActivity.this, "Identifiants validés", Toast.LENGTH_SHORT).show();
                            token = response.body().getToken();
                            Log.d("TOKEN", "Token récupéré : " + token);
                            apiService.getUsersData().enqueue(new Callback<List<Usersdata2>>() {
                                @Override
                                public void onResponse(Call<List<Usersdata2>> call, Response<List<Usersdata2>> response) {
                                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                        List<Usersdata2> usersDataList = response.body();
                                        boolean found = false;
                                        for (Usersdata2 data : usersDataList) {
                                            if ((data.getPseudo() != null || data.getEmail() != null) && (data.getPseudo().equals(pseudo_conn) || data.getEmail().equals(email))) {
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
                                            finish();
                                        } else {
                                            Toast.makeText(ConnexionActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        //Toast.makeText(ConnexionActivity.this, "Erreur lors de la récupération des données utilisateur", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<List<Usersdata2>> call, Throwable t) {
                                    //Toast.makeText(ConnexionActivity.this, "Erreur de connexion: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(ConnexionActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<LoginData> call, Throwable t) {
                        //Toast.makeText(ConnexionActivity.this, "Erreur de connexion: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ConnexionActivity", "Erreur de connexion", t);
                    }
                });
            }
        });

        TextView textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // Nouvelle fonction pour sauvegarder l'état de connexion
    private void saveLoginState() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userPseudo", pseudo_conn);
        editor.apply();
    }
}
