package com.exemple.applicationble;

import static android.content.ContentValues.TAG;

import static com.exemple.applicationble.ConnexionActivity.BASE_URL;
import static com.exemple.applicationble.MainActivity.question;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.exemple.applicationble.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class IdentificationActivity extends AppCompatActivity {

    // Partie Identification
    private EditText editTextEmail, editTextPseudo, editTextPassword, editTextConfirmPassword;
    // Le bouton "Inscription" d'origine est toujours présent dans le layout mais sera masqué
    private Button buttonConnecter;
    private ListView listViewBleDevices;
    private ArrayAdapter<String> deviceAdapter;
    private ArrayList<String> deviceNames;
    private ArrayList<BluetoothDevice> foundDevices;
    private EditText editTextPIN; // Ajout du champ PIN

    private TextView buttonTrouveTonVelo;

    private EditText editTextSecurityAnswer;


    String email;

    String pseudo_insc;

    String pwd;

    static String  UUID_NEW;
    public static String PIN;

    String newName;



    // Partie Renommage (intégrée dans le même layout)
    private LinearLayout layoutRename;
    // Ces vues proviennent de la partie renommage intégrée dans activity_identification.xml
    private EditText editTextModuleName;  // Champ de renommage
    private Button buttonConfirmRename;

    private ImageView buttonBypass;


    private BluetoothAdapter bluetoothAdapter;

    public static RadioButton radioQuestion1;

    public static RadioButton radioQuestion2;

    public static RadioButton radioQuestion3;

    public static  String respond;

    public int selectedId;

    public static int selectedQuestionId;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 10000;

    // Variables pour gérer la connexion BLE en cours
    private static BluetoothGatt currentGatt = null;
    private BluetoothDevice currentConnectedDevice = null;

    // UUID utilisés pour le module BLE
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    ApiService apiService;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identification);
        // Liaison des vues Identification
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPseudo = findViewById(R.id.editTextPseudo);
        radioQuestion1 = findViewById(R.id.radioQuestion1);
        radioQuestion2 = findViewById(R.id.radioQuestion2);
        radioQuestion3 = findViewById(R.id.radioQuestion3);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonTrouveTonVelo = findViewById(R.id.buttonTrouveTonVelo);
        //buttonConnecter = findViewById(R.id.buttonInscription);
        listViewBleDevices = findViewById(R.id.listViewBleDevices);
        editTextSecurityAnswer = findViewById(R.id.editTextSecurityAnswer);
       // RadioGroup radioGroup = findViewById(R.id.radioGroupSecurityQuestions);
       // selectedId = radioGroup.getCheckedRadioButtonId();
        buttonBypass = findViewById(R.id.buttonBypass);
        buttonBypass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Retour à l'activité précédente
                finish();
            }
        });

        Retrofit retrofit = getRetrofitWithCertificate();
        apiService = retrofit.create(ApiService.class);
        getIt();
        // Masquer le bouton "Inscription" d'origine (il n'est plus utilisé)
        //buttonConnecter.setVisibility(View.GONE);

        // Liaison des vues Renommage intégrées dans activity_identification.xml
        layoutRename = findViewById(R.id.layoutRename);
        // Masquer la partie renommage initialement
        layoutRename.setVisibility(View.GONE);
        editTextModuleName = findViewById(R.id.editTextModuleName);
        editTextModuleName.setFilters(new InputFilter[] { new InputFilter.LengthFilter(12) });
        buttonConfirmRename = findViewById(R.id.buttonConfirm);

        editTextPIN = findViewById(R.id.editTextPIN);
        editTextPIN.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(6) });

        // Initialisation des listes pour le scan BLE
        deviceNames = new ArrayList<>();
        foundDevices = new ArrayList<>();
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        listViewBleDevices.setAdapter(deviceAdapter);

        // Ajout d'un TextWatcher pour valider en temps réel les champs d'identification et de renommage
        TextWatcher inputWatcher = new TextWatcher() {
            @Override public void afterTextChanged(Editable s) { validateInputs(); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        };
        editTextEmail.addTextChangedListener(inputWatcher);
        editTextPassword.addTextChangedListener(inputWatcher);
        editTextConfirmPassword.addTextChangedListener(inputWatcher);
        editTextModuleName.addTextChangedListener(inputWatcher);
        editTextPIN.addTextChangedListener(inputWatcher);


        // Avertissements à la perte de focus
        editTextEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = editTextEmail.getText().toString().trim();
                if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(IdentificationActivity.this, "L'adresse email doit être conforme", Toast.LENGTH_LONG).show();
                }
            }
        });
        editTextConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String pwd = editTextPassword.getText().toString();
                String confirm = editTextConfirmPassword.getText().toString();
                if (!pwd.equals(confirm)) {
                    Toast.makeText(IdentificationActivity.this, "Les mots de passe doivent être identiques", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Bouton pour lancer le scan BLE
        buttonTrouveTonVelo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Lancement de l'activité d'identification pour "J'ai un vélo"
              startBleScan();
            }
        });

        // Gestion du clic sur un élément de la ListView (sélection d'un module BLE)
        listViewBleDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = foundDevices.get(position);

            // Si aucun module n'est connecté, connecte le module cliqué
            if (currentConnectedDevice == null) {
                currentGatt = device.connectGatt(IdentificationActivity.this, false, bluetoothGattCallback);
                currentConnectedDevice = device;
                //Toast.makeText(IdentificationActivity.this, "Tentative de connexion au module...", Toast.LENGTH_SHORT).show();
            } else {
                // Si le module cliqué est déjà connecté, déconnecte-le
                if (currentConnectedDevice.getAddress().equals(device.getAddress())) {
                    if (currentGatt != null) {
                        currentGatt.disconnect();
                        currentGatt.close();
                    }
                    currentConnectedDevice = null;
                   // Toast.makeText(IdentificationActivity.this, "Module déconnecté", Toast.LENGTH_SHORT).show();
                } else {
                    // Si un autre module est connecté, déconnecte-le puis connecte le nouveau module
                    if (currentGatt != null) {
                        currentGatt.disconnect();
                        currentGatt.close();
                    }
                    currentGatt = device.connectGatt(IdentificationActivity.this, false, bluetoothGattCallback);
                    currentConnectedDevice = device;
                     //Toast.makeText(IdentificationActivity.this, "Tentative de connexion au nouveau module...", Toast.LENGTH_SHORT).show();
                }
            }
            // Met à jour le statut affiché dans la liste
            updateDeviceStatus(device, currentConnectedDevice != null && currentConnectedDevice.getAddress().equals(device.getAddress()));
        });


        // Initialisation du Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non supporté sur cet appareil", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        // Bouton de renommage (Confirmé) : déclenche le processus d'envoi des commandes
        buttonConfirmRename.setOnClickListener(v -> {
            newName = editTextModuleName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(IdentificationActivity.this, "Entrez un nom valide", Toast.LENGTH_SHORT).show();
                return;
            }

            respond = editTextSecurityAnswer.getText().toString().trim();
            if (respond.isEmpty()) {
                editTextSecurityAnswer.setError("Veuillez répondre à cette question");
                Toast.makeText(this, "Vous devez répondre à la question de sécurité", Toast.LENGTH_SHORT).show();
                return;
            }

            PIN = editTextPIN.getText().toString().trim();
            if (!PIN.matches("[0-9]{1,6}")) {
                Toast.makeText(IdentificationActivity.this, "Le code PIN doit contenir 1 à 6 chiffres (entre 1 et 9)", Toast.LENGTH_SHORT).show();
                return;
            }
            // Enregistrer le nom du module dans SharedPreferences pour que HomeActivity puisse le récupérer
            SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("module_name", newName);
            editor.apply();

            buttonConfirmRename.setEnabled(false);
            final ProgressDialog progressDialog = new ProgressDialog(IdentificationActivity.this);
            progressDialog.setMessage("Initialisation du module en cours...");
            progressDialog.setCancelable(false);
            progressDialog.show();

// Envoi immédiat de la commande AT+NAME
            String command = "AT+NAME" + newName;
            sendAtCommand(command);

// Envoi de la commande AT+PASS après 1000 ms
            new Handler().postDelayed(() -> {
                String nw_cmd = "AT+PASS" + PIN;
                sendAtCommand(nw_cmd);
            }, 1000);

            final String[] commands = {
                    "AT+MODE1",
                    "AT+NOTI1",
                    "AT+TYPE2",
                    "AT+IBEA1",
                    "AT+POWE3",
                    "AT+ADVI5",
                    "AT+NAME?",
            };

            for (int i = 0; i < commands.length; i++) {
                final String cmd = commands[i];
                handler.postDelayed(() -> sendAtCommand(cmd), i * 2000);
            }

// Appel fetchUuidFromWebsiteAndSendIbeCommand après les commandes précédentes
            handler.postDelayed(() -> fetchUuidFromWebsiteAndSendIbeCommand(progressDialog), commands.length * 2000 + 1000);
            Log.e("serveur", "Firstgroup : " + UUID_NEW);

// Envoi de la commande AT+RESET en dernier, après un délai supplémentaire
            handler.postDelayed(() -> {
                String command_NEW = "AT+RESET";
                sendAtCommand(command_NEW);
            }, commands.length * 2000 + 2000);


        });
    }

    /**
     * Valide que tous les champs d'identification et de renommage sont renseignés.
     * Affiche un avertissement via setError si l'email n'est pas conforme ou si les mots de passe ne correspondent pas.
     * Active le bouton "Confirmé" si toutes les informations sont présentes.
     */
    private void validateInputs() {
        email = editTextEmail.getText().toString().trim();
        boolean emailValid = !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
        if (!emailValid) {
            editTextEmail.setError("Adresse email invalide");
        } else {
            editTextEmail.setError(null);
        }

        pseudo_insc = editTextPseudo.getText().toString().trim();

        pwd = editTextPassword.getText().toString();
        String confirmPwd = editTextConfirmPassword.getText().toString();
        boolean passwordsValid = !pwd.isEmpty() && pwd.equals(confirmPwd);
        if (!passwordsValid) {
            editTextPassword.setError("Les mots de passe ne correspondent pas");
            editTextConfirmPassword.setError("Les mots de passe ne correspondent pas");
        } else {
            editTextPassword.setError(null);
            editTextConfirmPassword.setError(null);
        }
        boolean renameValid = !editTextModuleName.getText().toString().trim().isEmpty();
        if (!renameValid) {
            editTextModuleName.setError("Veuillez renseigner le nom du module");
        } else {
            editTextModuleName.setError(null);
        }
        String pin = editTextPIN.getText().toString().trim();
        boolean pinValid = !pin.isEmpty() && pin.matches("[0-9]{1,6}");
        if (!pinValid) {
            editTextPIN.setError("Le code PIN doit contenir 1 à 6 chiffres (0-9)");
        } else {
            editTextPIN.setError(null);
        }
        radioQuestion1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedQuestionId = 1;
                // Vous pouvez également afficher un Toast pour confirmer :
                //Toast.makeText(IdentificationActivity.this, "Question 1 sélectionnée", Toast.LENGTH_SHORT).show();
            }
        });
        radioQuestion2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedQuestionId = 2;
                // Vous pouvez également afficher un Toast pour confirmer :
                //Toast.makeText(IdentificationActivity.this, "Question 1 sélectionnée", Toast.LENGTH_SHORT).show();
            }
        });
        radioQuestion3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedQuestionId = 3;
                // Vous pouvez également afficher un Toast pour confirmer :
                //Toast.makeText(IdentificationActivity.this, "Question 1 sélectionnée", Toast.LENGTH_SHORT).show();
            }
        });
            // Par exemple, affectez une valeur selon la question sélectionnée

        buttonConfirmRename.setEnabled(emailValid && passwordsValid && renameValid);
    }

    /**
     * Récupère le UUID depuis le site internet, extrait le premier groupe (8 caractères) en majuscules,
     * envoie la commande AT+IBE0 correspondante, puis affiche une pop-up "Configuration réussi",
     * ferme le ProgressDialog, et redirige l'utilisateur vers le MainActivity.
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
    public void getIt(){
        Call<List<Secretquestion>> call= apiService.getscretquestion();
        call.enqueue(new Callback<List<Secretquestion>>() {
            @Override
            public void onResponse(Call<List<Secretquestion>> call, retrofit2.Response<List<Secretquestion>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        List<Secretquestion> secretQuestionList = response.body();
                        for (Secretquestion i : secretQuestionList) {
                            question.add(i.toString());
                            Log.d("QUest","alima " + i.toString());
                        }
                        radioQuestion1.setText(question.get(0).toString());
                        radioQuestion2.setText(question.get(1).toString());
                        radioQuestion3.setText(question.get(2).toString());
                        //Log.d("QUest","al " + question.get(0).toString());
                    } else {
                        Log.e(TAG, "Erreur lors de la récupération des données : " + response.message());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception dans onResponse", e);
                }
            }
            @Override
            public void onFailure(Call<List<Secretquestion>> call, Throwable t) {
                Log.e(TAG, "Erreur de connexion lors du poll du serveur", t);
            }
        });
    }
    private void fetchUuidFromWebsiteAndSendIbeCommand(final ProgressDialog progressDialog) {
        new Thread(() -> {
            try {
                URL url = new URL("https://www.uuidgenerator.net/api/version4");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                // Ajout d'un User-Agent pour être sûr d'obtenir une réponse
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String uuid = reader.readLine();
                    reader.close();
                    if (uuid != null && !uuid.isEmpty()) {
                        String firstGroup = uuid.split("-")[0].toUpperCase();
                        String command = "AT+IBE0" + firstGroup;
                        UUID_NEW = firstGroup;
                        runOnUiThread(() -> {
                            sendAtCommand(command);

                            // Charge le certificat auto-signé depuis res/raw/selfsigned.cer
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

                            UsersData usersData = new UsersData(pseudo_insc, email, pwd, firstGroup, newName, PIN,selectedQuestionId,respond);
                            // Envoyer les données via POST
                            Call<UsersData> call = apiService.postUsersData(usersData);
                            Log.d("serveur", "Requête envoyée " + call.request().toString() + " And userData is " + usersData);
                            Request resquest = call.request();
                          //  resquest.newBuilder().addHeader("Autorization","Bearer" + TOKEN);
                            call.enqueue(new Callback<UsersData>() {
                                @Override
                                public void onResponse(Call<UsersData> call, Response<UsersData> response) {
                                    if (response.isSuccessful()) {
                                       // Toast.makeText(IdentificationActivity.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                                    } else {
                                      //  Toast.makeText(IdentificationActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                                        Log.e("serveur", "Erreur côté serveur : " + response.message());
                                    }
                                }

                                @Override
                                public void onFailure(Call<UsersData> call, Throwable t) {
                                  //  Toast.makeText(IdentificationActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("test2", "Erreur de connexion", t);
                                }
                            });
                            new AlertDialog.Builder(IdentificationActivity.this)
                                    .setTitle("Configuration réussi")
                                    .setMessage("Toutes les commandes ont été envoyées avec succès.")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        progressDialog.dismiss();
                                        // Masquer la partie renommage
                                        layoutRename.setVisibility(View.GONE);
                                        Intent intent = new Intent(IdentificationActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                        });
                    } else {
                        runOnUiThread(() -> {
                          //  Toast.makeText(IdentificationActivity.this, "Erreur: UUID vide", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            buttonConfirmRename.setEnabled(true);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                       // Toast.makeText(IdentificationActivity.this, "Erreur HTTP: " + responseCode, Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        buttonConfirmRename.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                   // Toast.makeText(IdentificationActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    buttonConfirmRename.setEnabled(true);
                });
            }
        }).start();
    }


    public static BluetoothGatt getCurrentGatt() {
        return currentGatt;
    }

    private void startBleScan() {
        // Demande des permissions pour Android S et versions supérieures
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                }, 100);
            }
        }

        // Demande de permission pour la localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Assurez-vous que le layout contenant la liste BLE soit visible
        // (Si ce layout était masqué pour la partie renommage, on le rend visible pour afficher la liste)
        layoutRename.setVisibility(View.VISIBLE);

        // Réinitialiser la liste des dispositifs trouvés
        foundDevices.clear();
        deviceNames.clear();
        deviceAdapter.notifyDataSetChanged();

        if (bluetoothLeScanner == null) {
         //   Toast.makeText(this, "Scanner BLE non disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                ScanRecord scanRecord = result.getScanRecord();
                Log.d("BLE_SCANNER", "Result not null");
                if (scanRecord != null) {
                    // Affiche les UUID des services trouvés (à titre de diagnostic)
                    List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
                    if (serviceUuids != null && !serviceUuids.isEmpty()) {
                        for (ParcelUuid uuid : serviceUuids) {
                            Log.d("BLE_SCANNER_NEW", "Service UUID trouvé: " + uuid.getUuid());
                        }
                    }
                }
                // Condition : nom non null et commençant par "HM"
                if (device.getName() != null && device.getName().startsWith("HM")) {
                    if (!foundDevices.contains(device)) {
                        foundDevices.add(device);
                        deviceNames.add(device.getName() + " - déconnecté");
                        runOnUiThread(() -> deviceAdapter.notifyDataSetChanged());
                    }
                }
            }
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.d("Batchresult", "Batchresult est appelé");
                for (ScanResult result : results) {
                    BluetoothDevice device = result.getDevice();
                    if (device.getName() != null && device.getName().startsWith("HM")) {
                        if (!foundDevices.contains(device)) {
                            foundDevices.add(device);
                            deviceNames.add(device.getName() + " - déconnecté");
                        }
                    }
                }
                runOnUiThread(() -> deviceAdapter.notifyDataSetChanged());
            }
            @Override
            public void onScanFailed(int errorCode) {
                //runOnUiThread(() ->
                      //  Toast.makeText(IdentificationActivity.this, "Scan échoué: " + errorCode, Toast.LENGTH_SHORT).show());
            }
        };

        // Démarrage du scan BLE
        bluetoothLeScanner.startScan(scanCallback);
       // Toast.makeText(this, "Scanning des dispositifs BLE...", Toast.LENGTH_SHORT).show();

        // Arrêt du scan après SCAN_PERIOD (10 secondes)
        handler.postDelayed(() -> {
            bluetoothLeScanner.stopScan(scanCallback);
           // runOnUiThread(() -> Toast.makeText(IdentificationActivity.this, "Scan terminé", Toast.LENGTH_SHORT).show());
        }, SCAN_PERIOD);
    }


    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            runOnUiThread(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                   // Toast.makeText(IdentificationActivity.this, "Module connecté", Toast.LENGTH_SHORT).show();
                    // Met à jour la liste pour afficher "connecté"
                    updateDeviceStatus(gatt.getDevice(), true);
                    // Affiche l'EditText pour renommer le module
                    EditText editTextModuleName = findViewById(R.id.editTextModuleName);
                    if (editTextModuleName != null) {
                        editTextModuleName.setVisibility(View.VISIBLE);
                    }
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                   // Toast.makeText(IdentificationActivity.this, "Module déconnecté", Toast.LENGTH_SHORT).show();
                    // Met à jour la liste pour afficher "déconnecté"
                    updateDeviceStatus(gatt.getDevice(), false);
                    // Masque l'EditText en cas de déconnexion
                    EditText editTextModuleName = findViewById(R.id.editTextModuleName);
                    if (editTextModuleName != null) {
                        editTextModuleName.setVisibility(View.GONE);
                    }
                    if (currentConnectedDevice != null &&
                            currentConnectedDevice.getAddress().equals(gatt.getDevice().getAddress())) {
                        currentGatt = null;
                        currentConnectedDevice = null;
                    }
                }
            });
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
               // runOnUiThread(() -> Toast.makeText(IdentificationActivity.this, "Services découverts", Toast.LENGTH_SHORT).show());
            } else {
               // runOnUiThread(() -> Toast.makeText(IdentificationActivity.this, "Échec de la découverte des services", Toast.LENGTH_SHORT).show());
            }
        }
    };


    private void updateDeviceStatus(BluetoothDevice device, boolean connected) {
        int index = foundDevices.indexOf(device);
        if (index != -1) {
            String baseName = device.getName();
            if (connected) {
                deviceNames.set(index, baseName + " - connecté");
            } else {
                deviceNames.set(index, baseName + " - déconnecté");
            }
            runOnUiThread(() -> deviceAdapter.notifyDataSetChanged());
        }
    }

    public static void sendAtCommand(String command) {
        if (currentGatt == null) {
            return;
        }
        BluetoothGattService service = currentGatt.getService(SERVICE_UUID);
        if (service == null) {
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            return;
        }
        characteristic.setValue(command.getBytes());
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        currentGatt.writeCharacteristic(characteristic);
    }
}
