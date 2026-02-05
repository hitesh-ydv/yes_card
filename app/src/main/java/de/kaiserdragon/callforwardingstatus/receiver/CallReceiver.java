package de.kaiserdragon.callforwardingstatus.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import de.kaiserdragon.callforwardingstatus.service.CallSyncForegroundService;
import de.kaiserdragon.callforwardingstatus.service.CallSyncWorker;

public class CallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {

            // Device finished a call → now sync call log
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

                // Android 12+ → Foreground Worker (allowed)
                CallSyncWorker.schedule(context);

            } else {

                // Android 11 and below → ForegroundService allowed
                Intent serviceIntent = new Intent(context, CallSyncForegroundService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}
