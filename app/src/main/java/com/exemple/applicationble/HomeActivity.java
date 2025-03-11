package com.exemple.applicationble;

import static android.content.ContentValues.TAG;
import static com.exemple.applicationble.ConnexionActivity.ps_mod_Id;
import static com.exemple.applicationble.ConnexionActivity.ps_mod_UUID;
import static com.exemple.applicationble.ConnexionActivity.ps_mod_name;
import static com.exemple.applicationble.ConnexionActivity.ps_mod_pin;
import static com.exemple.applicationble.ConnexionActivity.pseudo_conn;
import static com.exemple.applicationble.IdentificationActivity.PIN;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeActivity extends AppCompatActivity{

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private Button buttonNonVisible, buttonScanner, buttonVeloVole, buttonVeloTrouve, buttonShowPin;

    private ImageView imgvelo;

    private ListView lvModules;
    // Liste pour afficher le nom et le statut du périphérique
    private ArrayList<String> deviceList;
    // Liste pour stocker les objets BluetoothDevice détectés
    private ArrayList<BluetoothDevice> bleDeviceList;
    private ArrayAdapter<String> adapter;
    //ApiService apiService;
    static Intent monitoringServiceIntent;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    public static boolean vole = false;
    String deviceName;
    String baseName;
    // Variables pour le scan BLE
    private BluetoothAdapter mBluetoothAdapter;
    private boolean new_status;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;

    public static boolean bool = false;

    // Périphérique actuellement connecté
    private BluetoothDevice currentConnectedDevice = null;

    // UUID du service et de la caractéristique
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    // Handler pour le délai entre l'envoi des commandes
    private Handler commandHandler = new Handler();

    // ScanCallback filtrant selon le nom enregistré
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            BluetoothDevice device = result.getDevice();
            // Filtrer uniquement par ps_mod_name
            if (device.getName() == null || !device.getName().equals(ps_mod_name)) {
                return;
            }
            deviceName = device.getName();
            final String baseName = (deviceName != null && !deviceName.isEmpty())
                    ? deviceName : device.getAddress();
            final String displayName = baseName + " - déconnecté";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean alreadyPresent = false;
                    for (BluetoothDevice d : bleDeviceList) {
                        if (d.getAddress().equals(device.getAddress())) {
                            alreadyPresent = true;
                            break;
                        }
                    }
                    if (!alreadyPresent) {
                        bleDeviceList.add(device);
                        deviceList.add(displayName);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onBatchScanResults(java.util.List<ScanResult> results) {
            for (ScanResult result : results) {
                BluetoothDevice device = result.getDevice();
                // Filtrer uniquement par ps_mod_name
                if (device.getName() == null || !device.getName().equals(ps_mod_name)) {
                    continue;
                }
                deviceName = device.getName();
                final String baseName = (deviceName != null && !deviceName.isEmpty())
                        ? deviceName : device.getAddress();
                final String displayName = baseName + " - déconnecté";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!bleDeviceList.contains(device)) {
                            bleDeviceList.add(device);
                            deviceList.add(displayName);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            //runOnUiThread(() ->
                   // Toast.makeText(HomeActivity.this, "Scan échoué: " + errorCode, Toast.LENGTH_SHORT).show());
        }
    };

    // Callback pour la connexion BLE incluant la découverte des services
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (newState == BluetoothAdapter.STATE_CONNECTED) {
                        //Toast.makeText(HomeActivity.this, "Connecté au module BLE", Toast.LENGTH_SHORT).show();
                        // Activation du bouton "économie d'énergie" dès que le module est connecté
                        buttonNonVisible.setEnabled(true);
                        // Lancer la découverte des services
                        gatt.discoverServices();
                    } else if (newState == BluetoothAdapter.STATE_DISCONNECTED) {
                        //Toast.makeText(HomeActivity.this, "Déconnecté du module BLE", Toast.LENGTH_SHORT).show();
                        updateDeviceStatus(gatt.getDevice(), false);
                        // Désactivation du bouton en cas de déconnexion
                        buttonNonVisible.setEnabled(false);
                    }
                }
            });
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(HomeActivity.this, "Services découverts", Toast.LENGTH_SHORT).show();
                        updateDeviceStatus(gatt.getDevice(), true);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(HomeActivity.this, "Échec de la découverte des services", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    // Met à jour le statut affiché pour le périphérique dans la ListView
    private void updateDeviceStatus(BluetoothDevice device, boolean connected) {
        int index = -1;
        for (int i = 0; i < bleDeviceList.size(); i++) {
            if (bleDeviceList.get(i).getAddress().equals(device.getAddress())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            baseName = device.getName();
            if (baseName == null || baseName.isEmpty()) {
                baseName = device.getAddress();
            }
            String statusText = connected ? " - connecté" : " - déconnecté";
            deviceList.set(index, baseName + statusText);
            adapter.notifyDataSetChanged();
        }
    }

    // Méthode pour envoyer une commande AT au module BLE avec des Toasts de confirmation
    private void sendAtCommand(String command) {
        if (mBluetoothGatt == null) {
            //Toast.makeText(HomeActivity.this, "mBluetoothGatt est null", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            //Toast.makeText(HomeActivity.this, "Service non découvert", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
           // Toast.makeText(HomeActivity.this, "Caractéristique non trouvée", Toast.LENGTH_SHORT).show();
            return;
        }
        characteristic.setValue(command.getBytes());
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
          //  Toast.makeText(HomeActivity.this, "Échec de l'envoi de la commande: " + command, Toast.LENGTH_SHORT).show();
        } else {
           // Toast.makeText(HomeActivity.this, "Commande envoyée: " + command, Toast.LENGTH_SHORT).show();
        }
    }

    // Ajout de la méthode de demande des autorisations
    // Ajout de la méthode de demande des autorisations
    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();

        // Autorisation de localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // Pour Bluetooth (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            // Autorisation Nearby Devices pour Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, "android.permission.NEARBY_DEVICES")
                        != PackageManager.PERMISSION_GRANTED) {
                    permissions.add("android.permission.NEARBY_DEVICES");
                }
            }
        }
        // Autorisation de notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
        }

        // Vérifier si le Bluetooth est activé, sinon demander son activation
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Appel de la méthode pour demander les autorisations avant toute initialisation
        checkAndRequestPermissions();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        TextView textViewPseudo = findViewById(R.id.textViewPseudo);
        textViewPseudo.setText("Bienvenue " + pseudo_conn);
        // Récupération des boutons
        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        // Suppression du bouton "visible"
        //buttonVisible = findViewById(R.id.buttonVisible);
        buttonNonVisible = findViewById(R.id.buttonNonVisible);
        buttonVeloVole = findViewById(R.id.buttonVeloVole);
        buttonVeloTrouve = findViewById(R.id.buttonVeloTrouve);
        imgvelo = findViewById(R.id.bikefront);
        // Récupération de la ListView
        buttonScanner = findViewById(R.id.buttonScanner);
        lvModules = findViewById(R.id.lvModules);
        // Nouvelle récupération du bouton pour afficher le PIN
        Button buttonShowPin = findViewById(R.id.buttonShowPin);

        // Désactivation initiale du bouton "Non Visible" (maintenant "économie d'énergie")
        buttonNonVisible.setEnabled(false);
        // Changement du libellé du bouton "Non Visible"

        if(vole){
            buttonVeloVole.setEnabled(false); // Désactiver "Vélo Volé"
        } else {
            buttonVeloTrouve.setEnabled(false); // Désactiver "Vélo Trouvé"
        }

        // Initialisation des listes et de l'adapter
        deviceList = new ArrayList<>();
        bleDeviceList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        lvModules.setAdapter(adapter);

        buttonScanner.setOnClickListener(v -> {
            startBleScan();
           // Toast.makeText(HomeActivity.this, "Scan BLE lancé", Toast.LENGTH_SHORT).show();
        });
        // Configuration du bouton "Show PIN" pour afficher le PIN dans un Toast pendant 1 seconde
        buttonShowPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButton(v);
                final Toast toast = Toast.makeText(HomeActivity.this, "PIN: " + ps_mod_pin, Toast.LENGTH_SHORT);
                toast.show();
                new Handler().postDelayed(() -> toast.cancel(), 1000);
            }
        });

        // Configuration du bouton menu
        buttonMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(HomeActivity.this, v);
            popupMenu.getMenu().add("Sign out");
            popupMenu.setOnMenuItemClickListener(item -> {
                if ("Sign out".equals(item.getTitle())) {
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("isLoggedIn");
                    editor.apply();
                    Intent intent = new Intent(HomeActivity.this, ConnexionActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });
        buttonNonVisible.setOnClickListener(v -> {
            animateButton(v);
            if(bool){
                buttonNonVisible.setText("Mode Eco");
                if (currentConnectedDevice != null && mBluetoothGatt != null) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> sendAtCommand("AT+POWE3"), 0);
                    handler.postDelayed(() -> sendAtCommand("AT+PWRM1"), 200);
                    handler.postDelayed(() -> sendAtCommand("AT+ADVI5"), 400);

                } else {
                    //Toast.makeText(HomeActivity.this, "Aucun module connecté", Toast.LENGTH_SHORT).show();
                }
            }else{
                buttonNonVisible.setText("Mode normal");
                if (currentConnectedDevice != null && mBluetoothGatt != null) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> sendAtCommand("AT+POWE2"), 0);
                    handler.postDelayed(() -> sendAtCommand("AT+ADVIA"), 200);
                    handler.postDelayed(() -> sendAtCommand("AT+PWRM0"), 400);

                    //buttonNonVisible.setEnabled(false);
                } else {
                    //Toast.makeText(HomeActivity.this, "Aucun module connecté", Toast.LENGTH_SHORT).show();
                }

            }
            bool = !bool;
        });

        Log.d("UUID_VERIF", "L'UUID est : "+ ps_mod_UUID);
        // Bouton "Vélo Volé"
        buttonVeloVole.setOnClickListener(v -> {
            animateButton(v);
            imgvelo.setImageResource(R.drawable.bikefront_red);
            monitoringServiceIntent = new Intent(HomeActivity.this, MonitoringService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(monitoringServiceIntent);
            } else {
                startService(monitoringServiceIntent);
            }
            vole = true;
            String pseuo = pseudo_conn;
            new_status = true;
            poststatus(new_status);
           // Toast.makeText(HomeActivity.this, "Bouton Vélo Volé cliqué", Toast.LENGTH_SHORT).show();
            buttonVeloVole.setEnabled(false); // Désactiver "Vélo Volé"
            buttonVeloTrouve.setEnabled(true); // Activer "Vélo Trouvé"
        });

        // Bouton "Vélo Trouvé"
        buttonVeloTrouve.setOnClickListener(v -> {
            animateButton(v);
            imgvelo.setImageResource(R.drawable.bikefront_green);
            vole = false;
            String pseuo = pseudo_conn;
            new_status = false;
            poststatustrue(new_status);
           // Toast.makeText(HomeActivity.this, "Bouton Vélo Trouvé cliqué", Toast.LENGTH_SHORT).show();
            stopService(monitoringServiceIntent);
            buttonVeloTrouve.setEnabled(false);
            buttonVeloVole.setEnabled(true);
            new Handler(Looper.getMainLooper()).postDelayed(() -> imgvelo.setImageResource(R.drawable.bikefront), 10000);
        });

        // Configuration de la ListView pour la sélection d'un module BLE
        lvModules.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = bleDeviceList.get(position);

            // Si l'appareil n'est pas apparié, déclencher l'appairage (le système affichera la demande de PIN)
            if (selectedDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                selectedDevice.createBond();
               // Toast.makeText(HomeActivity.this, "En attente d'appairage...", Toast.LENGTH_SHORT).show();
            } else {
                // Si l'appareil est déjà apparié, gérer la connexion/déconnexion
                if (currentConnectedDevice != null &&
                        currentConnectedDevice.getAddress().equals(selectedDevice.getAddress())) {
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                    }
                    currentConnectedDevice = null;
                   // Toast.makeText(HomeActivity.this, "Déconnecté du module", Toast.LENGTH_SHORT).show();
                    updateDeviceStatus(selectedDevice, false);
                    buttonNonVisible.setEnabled(false);
                } else {
                    if (currentConnectedDevice != null && mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                    }
                    mBluetoothGatt = selectedDevice.connectGatt(HomeActivity.this, false, mGattCallback);
                    currentConnectedDevice = selectedDevice;
                    Toast.makeText(HomeActivity.this, "Connexion en cours à " +
                                    ((selectedDevice.getName() != null && !selectedDevice.getName().isEmpty())
                                            ? selectedDevice.getName() : selectedDevice.getAddress()),
                            Toast.LENGTH_SHORT).show();
                    if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(mScanCallback);
                    }
                }
            }
        });

        ;

        // Démarrer le scan BLE
        startBleScan();
    }

    public void poststatus(boolean status) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.36.126.63:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Velostatus velostatus = new Velostatus(ps_mod_Id, status);
        Call<Velostatus> call = apiService.postVelostatus(ps_mod_UUID, velostatus);
        Log.d("serveur", "Requête envoyée " + call.request().toString() + " And veloData is " + velostatus);
        call.enqueue(new Callback<Velostatus>() {
            @Override
            public void onResponse(Call<Velostatus> call, Response<Velostatus> response) {
                if (response.isSuccessful()) {
                   // Toast.makeText(HomeActivity.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    //Toast.makeText(HomeActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("serveur", "Erreur côté serveur : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Velostatus> call, Throwable t) {
               // Toast.makeText(HomeActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("test2", "Erreur de connexion", t);
            }
        });
    }

    public void poststatustrue(boolean status) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.36.126.63:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Velostatustrue velostatustrue = new Velostatustrue(ps_mod_Id, status);
        Call<Velostatustrue> call = apiService.postVelostatustrue(ps_mod_UUID, velostatustrue);
        Log.d("serveur", "Requête envoyée " + call.request().toString() + " And veloData is " + velostatustrue);
        call.enqueue(new Callback<Velostatustrue>() {
            @Override
            public void onResponse(Call<Velostatustrue> call, Response<Velostatustrue> response) {
                if (response.isSuccessful()) {
                   // Toast.makeText(HomeActivity.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                } else {
                   // Toast.makeText(HomeActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("serveur", "Erreur côté serveur : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Velostatustrue> call, Throwable t) {
               // Toast.makeText(HomeActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("test2", "Erreur de connexion", t);
            }
        });
    }

    public void animateButton(View button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.5f, 1f);

        scaleX.setDuration(200);
        scaleY.setDuration(200);

        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.start();
        scaleY.start();
    }

    // Méthode pour démarrer le scan BLE
    private void startBleScan() {
        android.bluetooth.BluetoothManager bluetoothManager = (android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth désactivé ou non disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(mScanCallback);
        //Toast.makeText(this, "Scan BLE démarré...", Toast.LENGTH_SHORT).show();
    }
}
