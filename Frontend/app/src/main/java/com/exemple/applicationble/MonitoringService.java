package com.exemple.applicationble;

import static com.exemple.applicationble.LoginActivity.ps_mod_UUID;
import static com.exemple.applicationble.DiffieHellman.decryptedfunction;
import static com.exemple.applicationble.ForegroundService.aliceManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MonitoringService extends Service {

    private static final String TAG = "MonitoringService";
    private static final String CHANNEL_ID = "MonitoringServiceChannel";
    private Handler handler;
    private Runnable pollingRunnable;
    private ApiService apiService;

    public static SecretKey bobSharedKey;
    public static DiffieHellman.DiffieHellmanManager bobManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service de Monitoring")
                .setContentText("En écoute des alertes serveur")
                .setSmallIcon(R.drawable.ic_notification)
                .build();

        startForeground(2, notification);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.36.126.63:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Génération unique de la paire de clés pour Bob
        if (bobManager == null) {
            bobManager = new DiffieHellman.DiffieHellmanManager();
            try {
                bobManager.generateKeyPair();
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la génération de la paire de clés DH pour Bob", e);
            }
        }

        handler = new Handler();
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                pollServer();
                handler.postDelayed(this, 15000);
            }
        };
        handler.post(pollingRunnable);
    }

    private void pollServer() {
        if (apiService == null) {
            Log.e(TAG, "ApiService non initialisé");
            return;
        }
        Call<List<VeloData>> call = apiService.getVeloData(ps_mod_UUID);
        call.enqueue(new Callback<List<VeloData>>() {
            @Override
            public void onResponse(Call<List<VeloData>> call, retrofit2.Response<List<VeloData>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<VeloData> veloDataList = response.body();
                    VeloData latestData = veloDataList.get(veloDataList.size() - 1);
                    // La chaîne reçue est le message chiffré encodé en Base64
                    String gps = latestData.getGps();
                    Log.e("decryt", "latestData.getGps() ::  " + gps);

                    new Thread(() -> {
                        try {
                            // Réutilisation de bobManager généré en onCreate
                            if (aliceManager != null) {
                                PublicKey alicePublicKey = aliceManager.getPublicKey();
                                bobManager.setPeerPublicKey(alicePublicKey);
                            } else {
                                Log.e("ForegroundService", "L'instance de DiffieHellmanManager pour Alice est null");
                                return;
                            }
                            bobSharedKey = bobManager.generateSharedSecret();
                            Log.d("decrytons", "bobSharedKey  :: " + bobSharedKey);

                            // Décodage du message chiffré depuis Base64
                            byte[] encryptedBytes = Base64.decode(gps, Base64.NO_WRAP);
                            String decryptedGps = decryptedfunction(encryptedBytes, bobSharedKey);
                            Log.d("DECRYPT", "decrypted :: " + decryptedGps);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                sendNotification("Bonne nouvelle",
                                        "Votre vélo a été retrouvé !\nCliquez pour voir l'emplacement.",
                                        decryptedGps);
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du déchiffrement", e);
                        }
                    }).start();

                } else {
                    Log.e(TAG, "Erreur lors de la récupération des données : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<VeloData>> call, Throwable t) {
                Log.e(TAG, "Erreur de connexion lors du poll du serveur", t);
            }
        });
    }

    private void sendNotification(String title, String message, String gps) {
        String uri = "geo:" + gps + "?q=" + gps;
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        mapIntent.setPackage("com.google.android.apps.maps");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal Service de Monitoring",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pollingRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
