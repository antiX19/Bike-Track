package com.exemple.applicationble;

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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class MainPageActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;
    private Button buttonEco, buttonBikeStolen, buttonBikeFound;
    private SecretKey sharedSecretKey; // Clé partagée pour le chiffrement

    private List<BluetoothDevice> bleDeviceList = new ArrayList<>();
    private List<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is disabled or not available", Toast.LENGTH_SHORT).show();
            return;
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        buttonEco = findViewById(R.id.btnEco);
        buttonBikeStolen = findViewById(R.id.buttonBikeStolen);
        buttonBikeFound = findViewById(R.id.buttonBikeFound);
        // Récupération de la clé partagée depuis les préférences ou une autre source
        retrieveSharedSecretKey();

        ListView lvModules = findViewById(R.id.listViewBleDevices); // Replace with your actual ListView ID
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        lvModules.setAdapter(adapter);

        startBleScan();
    }

    private void retrieveSharedSecretKey() {
        try {
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            String sharedKeyEncoded = prefs.getString("SHARED_SECRET_KEY", null);
            if (sharedKeyEncoded != null) {
                byte[] sharedKeyBytes = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sharedKeyBytes = Base64.getDecoder().decode(sharedKeyEncoded);
                }
                sharedSecretKey = new SecretKeySpec(sharedKeyBytes, 0, 16, "AES");
                Log.d("DH", "Clé partagée récupérée avec succès");
            } else {
                Log.e("DH", "Clé partagée non trouvée");
            }
        } catch (Exception e) {
            Log.e("DH", "Erreur lors de la récupération de la clé partagée", e);
        }
    }

    // Méthode pour envoyer une commande AT au module BLE avec chiffrement
    private void sendAtCommand(String command) {
        if (mBluetoothGatt == null) {
            Toast.makeText(MainPageActivity.this, "mBluetoothGatt est null", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            Toast.makeText(MainPageActivity.this, "Service non découvert", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            Toast.makeText(MainPageActivity.this, "Caractéristique non trouvée", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Chiffrement de la commande avec la clé partagée
            if (sharedSecretKey == null) {
                Log.e("DH", "Clé partagée non disponible pour le chiffrement");
                return;
            }
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sharedSecretKey);
            byte[] encryptedCommand = cipher.doFinal(command.getBytes());

            // Envoi de la commande chiffrée
            characteristic.setValue(encryptedCommand);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
            if (!success) {
                Toast.makeText(MainPageActivity.this, "Échec de l'envoi de la commande: " + command, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainPageActivity.this, "Commande envoyée: " + command, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("DH", "Erreur lors du chiffrement de la commande", e);
        }
    }
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null) {
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                Log.d("BLE_SCAN", "Device found: " + deviceName + " [" + deviceAddress + "]");

                if (!bleDeviceList.contains(device)) {
                    bleDeviceList.add(device);
                    deviceList.add(deviceName + " - " + deviceAddress);
                    adapter.notifyDataSetChanged();
                }
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BLE_SCAN", "Scan failed with error code: " + errorCode);
        }
    };

        // Méthode pour démarrer le scan BLE
        private void startBleScan() {
            if (mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mScanCallback);
                Toast.makeText(this, "Scanning for BLE devices...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "BLE scanner not available", Toast.LENGTH_SHORT).show();
            }
        }

        // Callback pour la connexion BLE incluant la découverte des services
        private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BLE_GATT", "Connected to GATT server.");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BLE_GATT", "Disconnected from GATT server.");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE_GATT", "Services discovered.");
                } else {
                    Log.w("BLE_GATT", "Service discovery failed with status: " + status);
                }
            }
        };
}

