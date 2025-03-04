package com.exemple.applicationble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Redémarrage détecté, démarrage des services");
            Intent foregroundServiceIntent = new Intent(context, ForegroundService.class);
            Intent monitoringServiceIntent = new Intent(context, MonitoringService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(foregroundServiceIntent);
                context.startForegroundService(monitoringServiceIntent);
            } else {
                context.startService(foregroundServiceIntent);
                context.startService(monitoringServiceIntent);
            }
        }
    }
}
