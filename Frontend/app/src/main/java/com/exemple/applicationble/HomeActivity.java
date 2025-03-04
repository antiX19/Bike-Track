package com.exemple.applicationble;

import static android.content.ContentValues.TAG;
import static com.exemple.applicationble.ConnexionActivity.ps_mod_Id;
import static com.exemple.applicationble.ConnexionActivity.ps_mod_UUID;
import static com.exemple.applicationble.ConnexionActivity.ps_mod_name;
import static com.exemple.applicationble.ConnexionActivity.ps_mod_pin;
import static com.exemple.applicationble.ConnexionActivity.pseudo_conn;
import static com.exemple.applicationble.IdentificationActivity.PIN;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeActivity extends AppCompatActivity {

    private Button buttonVisible, buttonNonVisible, buttonVeloVole, buttonVeloTrouve, buttonShowPin;
    private ListView lvModules;
    // Liste pour afficher le nom et le statut du périphérique
    private ArrayList<String> deviceList;
    // Liste pour stocker les objets BluetoothDevice détectés
    private ArrayList<BluetoothDevice> bleDeviceList;
    private ArrayAdapter<String> adapter;
    //ApiService apiService;
    Intent monitoringServiceIntent;


    String deviceName;



    String baseName;

    // Variables pour le scan BLE
    private BluetoothAdapter mBluetoothAdapter;
    private boolean new_status;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;

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
        public void onBatchScanResults(List<ScanResult> results) {
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
            runOnUiThread(() ->
                    Toast.makeText(HomeActivity.this, "Scan échoué: " + errorCode, Toast.LENGTH_SHORT).show());
        }
    };


    // Callback pour la connexion BLE incluant la découverte des services
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Toast.makeText(HomeActivity.this, "Connecté au module BLE", Toast.LENGTH_SHORT).show();
                        // Activation des boutons dès que le module est connecté
                        buttonVisible.setEnabled(true);
                        buttonNonVisible.setEnabled(true);
                        // Lancer la découverte des services
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Toast.makeText(HomeActivity.this, "Déconnecté du module BLE", Toast.LENGTH_SHORT).show();
                        updateDeviceStatus(gatt.getDevice(), false);
                        // Désactivation des boutons en cas de déconnexion
                        buttonVisible.setEnabled(false);
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
                        Toast.makeText(HomeActivity.this, "Services découverts", Toast.LENGTH_SHORT).show();
                        updateDeviceStatus(gatt.getDevice(), true);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(HomeActivity.this, "Échec de la découverte des services", Toast.LENGTH_SHORT).show();
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
            //if (device.getName().equals(ps_mod_name)){
                baseName = device.getName();
            //}
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
            Toast.makeText(HomeActivity.this, "mBluetoothGatt est null", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            Toast.makeText(HomeActivity.this, "Service non découvert", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            Toast.makeText(HomeActivity.this, "Caractéristique non trouvée", Toast.LENGTH_SHORT).show();
            return;
        }
        characteristic.setValue(command.getBytes());
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
            Toast.makeText(HomeActivity.this, "Échec de l'envoi de la commande: " + command, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(HomeActivity.this, "Commande envoyée: " + command, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        TextView textViewPseudo = findViewById(R.id.textViewPseudo);
        textViewPseudo.setText("Bienvenue " + pseudo_conn);
        // Récupération des boutons
        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        buttonVisible = findViewById(R.id.buttonVisible);
        buttonNonVisible = findViewById(R.id.buttonNonVisible);
        buttonVeloVole = findViewById(R.id.buttonVeloVole);
        buttonVeloTrouve = findViewById(R.id.buttonVeloTrouve);
        // Récupération de la ListView
        lvModules = findViewById(R.id.lvModules);
        // Nouvelle récupération du bouton pour afficher le PIN
        Button buttonShowPin = findViewById(R.id.buttonShowPin);

        // Désactivation initiale des boutons "Visible" et "Non Visible"
        buttonVisible.setEnabled(false);
        buttonNonVisible.setEnabled(false);

        // Initialisation des listes et de l'adapter
        deviceList = new ArrayList<>();
        bleDeviceList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        lvModules.setAdapter(adapter);

        // Configuration du bouton "Show PIN" pour afficher le PIN dans un Toast pendant 1 seconde
        buttonShowPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Toast toast = Toast.makeText(HomeActivity.this, "PIN: " + ps_mod_pin, Toast.LENGTH_SHORT);
                toast.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toast.cancel();
                    }
                }, 1000); // Affiche pendant 1 seconde
            }
        });

        // Configuration des listeners des boutons
        buttonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Créer et afficher un PopupMenu ancré sur le bouton hamburger
                PopupMenu popupMenu = new PopupMenu(HomeActivity.this, v);
                // Ajouter l'option "Sign out" dans le menu (vous pouvez en ajouter d'autres si nécessaire)
                popupMenu.getMenu().add("Sign out");

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        if ("Sign out".equals(item.getTitle())) {
                            // Effacer l'état de connexion dans les SharedPreferences
                            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.remove("isLoggedIn");
                            editor.apply();

                            // Rediriger vers l'écran de connexion (ConnexionActivity)
                            Intent intent = new Intent(HomeActivity.this, ConnexionActivity.class);
                            startActivity(intent);
                            finish();
                            return true;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
        // Bouton "Visible" : envoie les commandes AT pour le mode visible, se grise et active "Non Visible"
        buttonVisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentConnectedDevice != null && mBluetoothGatt != null) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> sendAtCommand("AT+POWE3"), 0);
                    handler.postDelayed(() -> sendAtCommand("AT+ADVI0"), 200);
                    handler.postDelayed(() -> sendAtCommand("AT+PWRM1"), 400);
                    buttonVisible.setEnabled(false);
                    buttonNonVisible.setEnabled(true);
                } else {
                    Toast.makeText(HomeActivity.this, "Aucun module connecté", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Bouton "Non Visible" : envoie les commandes AT pour le mode non visible, se grise et active "Visible"
        buttonNonVisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentConnectedDevice != null && mBluetoothGatt != null) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> sendAtCommand("AT+POWE2"), 0);
                    handler.postDelayed(() -> sendAtCommand("AT+ADVIA"), 200);
                    handler.postDelayed(() -> sendAtCommand("AT+PWRM0"), 400);
                    buttonNonVisible.setEnabled(false);
                    buttonVisible.setEnabled(true);
                } else {
                    Toast.makeText(HomeActivity.this, "Aucun module connecté", Toast.LENGTH_SHORT).show();
                }
            }
        });
        Log.d("UUID_VERIF", "L'UUID est : "+ ps_mod_UUID);
        buttonVeloVole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monitoringServiceIntent = new Intent(HomeActivity.this, MonitoringService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(monitoringServiceIntent);
                } else {
                    startService(monitoringServiceIntent);
                }
                String pseuo = pseudo_conn;
                new_status = true;
                poststatus(new_status);
                Toast.makeText(HomeActivity.this, "Bouton Vélo Volé cliqué", Toast.LENGTH_SHORT).show();
                buttonVeloVole.setEnabled(false); // Désactiver "Vélo Volé"
                buttonVeloTrouve.setEnabled(true); // Activer "Vélo Trouvé" au cas où il était désactivé
            }
        });
        buttonVeloTrouve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pseuo = pseudo_conn;
                new_status = false;
                poststatustrue(new_status);
                Toast.makeText(HomeActivity.this, "Bouton Vélo Trouvé cliqué", Toast.LENGTH_SHORT).show();
                stopService(monitoringServiceIntent);
                buttonVeloTrouve.setEnabled(false); // Désactiver "Vélo Trouvé"
                buttonVeloVole.setEnabled(true); // Activer "Vélo Volé" au cas où il était désactivé
            }
        });

        // Configuration de la ListView pour la sélection d'un module BLE
        lvModules.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = bleDeviceList.get(position);
                SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                String storedPin = sharedPreferences.getString("PIN_CODE", null); // returns null if not found
                if (storedPin != null) {
                    // Use the stored PIN
                    Log.d("PIN", "Stored PIN is: " + storedPin);
                } else {
                    // No PIN saved; store the PIN
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("PIN_CODE", PIN); // PIN is your generated code (a String)
                    editor.apply();
                    Log.d("PIN", "Stored PIN is: " + PIN);
                }

                // Si l'appareil n'est pas apparié, définir automatiquement le PIN et lancer l'appariement
                if (selectedDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    try {
                        Method setPinMethod = selectedDevice.getClass().getMethod("setPin", byte[].class);
                        byte[] pinBytes = String.valueOf(PIN).getBytes("UTF-8");
                        Boolean result = (Boolean) setPinMethod.invoke(selectedDevice, new Object[]{pinBytes});
                        if (result != null && result) {
                            Toast.makeText(HomeActivity.this, "PIN auto défini: " + PIN, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(HomeActivity.this, "Échec de la définition du PIN", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(HomeActivity.this, "Erreur lors de la définition du PIN", Toast.LENGTH_SHORT).show();
                    }
                    selectedDevice.createBond();
                    return;
                } else {
                    // Si l'appareil est déjà apparié, basculer entre connexion et déconnexion
                    if (currentConnectedDevice != null &&
                            currentConnectedDevice.getAddress().equals(selectedDevice.getAddress())) {
                        if (mBluetoothGatt != null) {
                            mBluetoothGatt.disconnect();
                            mBluetoothGatt.close();
                            mBluetoothGatt = null;
                        }
                        currentConnectedDevice = null;
                        Toast.makeText(HomeActivity.this, "Déconnecté du module", Toast.LENGTH_SHORT).show();
                        updateDeviceStatus(selectedDevice, false);
                        buttonVisible.setEnabled(false);
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
            }
        });

        // Démarrer le scan BLE
        startBleScan();
    }

    public void poststatus(boolean status) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.36.126.63:3000/") // URL de base du serveur
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
                    Toast.makeText(HomeActivity.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomeActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("serveur", "Erreur côté serveur : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Velostatus> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("test2", "Erreur de connexion", t);
            }
        });
    }

    public void poststatustrue(boolean status) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.36.126.63:3000/") // URL de base du serveur
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
                    Toast.makeText(HomeActivity.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomeActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("serveur", "Erreur côté serveur : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Velostatustrue> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("test2", "Erreur de connexion", t);
            }
        });
    }


    // Méthode pour démarrer le scan BLE
    private void startBleScan() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth désactivé ou non disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(mScanCallback);
        Toast.makeText(this, "Scan BLE démarré...", Toast.LENGTH_SHORT).show();
    }
}
