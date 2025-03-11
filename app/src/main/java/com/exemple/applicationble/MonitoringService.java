package com.exemple.applicationble;

import static com.exemple.applicationble.ConnexionActivity.ps_mod_UUID;
import static com.exemple.applicationble.DiffieHellman.decodePublicKey;
import static com.exemple.applicationble.DiffieHellman.decryptedfunction;
import static com.exemple.applicationble.DiffieHellman.encodePublicKey;
import static com.exemple.applicationble.ForegroundService.getmanageralice;

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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.security.PublicKey;
import java.util.List;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MonitoringService extends Service {

    private static final String TAG = "MonitoringService";
    private static final String CHANNEL_ID = "MonitoringServiceChannel";
    private Handler handler;
    private Runnable pollingRunnable;
    private ApiService apiService;

    private static SecretKey bobSharedKey;
    private static DiffieHellman.DiffieHellmanManager bobManager;

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

    public static SecretKey getbobosecret(){
        return bobSharedKey;
    }

    public static DiffieHellman.DiffieHellmanManager getmanagerbob(){
        return bobManager;
    }

    private void pollServer() {
        if (apiService == null) {
            Log.e(TAG, "ApiService non initialisé");
            return;
        }
        String bobkey = encodePublicKey(bobManager.getPublicKey());
        PublicKey_new publicKey = new PublicKey_new(bobkey, null, ps_mod_UUID);
        Call<PublicKey_new> call2 = apiService.postpublickey(publicKey).clone();
        Log.d("serveur3", "Requête envoyée " + call2.request().toString() + " And alicekey is " + publicKey);
        call2.enqueue(new Callback<PublicKey_new>() {
            @Override
            public void onResponse(Call<PublicKey_new> call2, Response<PublicKey_new> response) {
                if (response.isSuccessful()) {
                    //Toast.makeText(MonitoringService.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                    Log.d("serveur3", "Réponse succès : " + response.body());
                } else {
                    //Toast.makeText(MonitoringService.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("serveur3", "Erreur côté serveur : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PublicKey_new> call2, Throwable t) {
                //Toast.makeText(MonitoringService.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("test2", "Erreur de connexion", t);
            }
        });

        Call<List<VeloData>> call = apiService.getVeloData(ps_mod_UUID);
        call.enqueue(new Callback<List<VeloData>>() {
            @Override
            public void onResponse(Call<List<VeloData>> call, Response<List<VeloData>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<VeloData> veloDataList = response.body();
                    Log.d(TAG, "Nombre d'éléments récupérés : " + veloDataList.size());
                    VeloData latestData = veloDataList.get(veloDataList.size() - 1);
                    String gps = latestData.getGps();
                    Log.d("decryt", "latestData.getGps() :: " + gps);

                    new Thread(() -> {
                        try {
                            // Récupérer la clé publique d'Alice de manière synchrone
                            Call<PublicKey_new> call_new = apiService.getPublicKey(ps_mod_UUID);
                            Response<PublicKey_new> response_new = call_new.execute();
                            if (response_new.isSuccessful() && response_new.body() != null) {
                                String alikey = response_new.body().getPublicKeyali();
                                if (alikey == null || alikey.isEmpty()) {
                                    Log.e("GET_KEY", "La clé publique d'Alice est nulle ou vide");
                                    return;
                                }
                                PublicKey publicalikey = decodePublicKey(alikey);
                                if (publicalikey != null) {
                                    bobManager.setPeerPublicKey(publicalikey);
                                    Log.d("GET_KEY", "Clé publique d'Alice correctement assignée");
                                } else {
                                    Log.e("GET_KEY", "Erreur lors du décodage de la clé publique d'Alice");
                                    return;
                                }
                            } else {
                                Log.e("GET_KEY", "Erreur côté serveur lors de la récupération de la clé publique : " + response_new.message());
                                return;
                            }

                            bobSharedKey = bobManager.generateSharedSecret();
                            Log.d("decrytons", "bobSharedKey  :: " + bobSharedKey);

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
        // Ajout d'un log pour vérifier l'appel à la notification
        Log.d("NOTIFICATION", "Envoi de notification avec titre: " + title + ", message: " + message + ", gps: " + gps);

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
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Passage à IMPORTANCE_HIGH afin de forcer l'affichage de la notification
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal Service de Monitoring",
                    NotificationManager.IMPORTANCE_HIGH
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
