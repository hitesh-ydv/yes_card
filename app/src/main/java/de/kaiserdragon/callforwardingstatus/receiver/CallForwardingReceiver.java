package de.kaiserdragon.callforwardingstatus.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.Objects;

import de.kaiserdragon.callforwardingstatus.service.PhoneStateService;
import de.kaiserdragon.callforwardingstatus.helper.DatabaseHelper;
import de.kaiserdragon.callforwardingstatus.service.CallForwardingService;

public class CallForwardingReceiver extends BroadcastReceiver {

    final String TAG = "Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if ("de.kaiserdragon.callforwardingstatus.TOGGLE_CALL_FORWARDING".equals(intent.getAction())) {

            boolean isRemote = intent.getBooleanExtra("remote", false);
            boolean enableForwarding = intent.getBooleanExtra("cfi", false);

            int remoteSim = intent.getIntExtra("sim", -1);
            String remoteNumber = intent.getStringExtra("number");

            Log.d(TAG, "Remote=" + isRemote + " enable=" + enableForwarding + " sim=" + remoteSim);

            DatabaseHelper db = new DatabaseHelper(context);
            String[] config = db.getSelected();
            db.close();

            String savedNumber = config[1];
            String finalNumber = remoteNumber != null ? remoteNumber : savedNumber;

            if (finalNumber == null || finalNumber.isEmpty()) {
                if (!isRemote) {
                    Toast.makeText(context, "No forwarding number set", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            /** 🔥 Launch foreground service instead of executing USSD here */
            Intent svc = new Intent(context, CallForwardingService.class);
            svc.putExtra("remote", isRemote);
            svc.putExtra("cfi", enableForwarding);
            svc.putExtra("sim", remoteSim);
            svc.putExtra("number", finalNumber);

            Log.d(TAG, "Starting CallForwardingService...");
            context.startForegroundService(svc);
        }

        // Auto-start boot logic remains unchanged
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (Objects.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED")) {
                Log.v(TAG, "Boot Complete: Start PhoneStateService");
                context.startService(new Intent(context, PhoneStateService.class));
            }
        }
    }

    /** Get stored SIM ID */
    private int getSavedSelectedSimId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SIM_PREFERENCES", Context.MODE_PRIVATE);
        return prefs.getInt("SELECTED_SIM_ID", -1);
    }

    /** fallback SIM finder */
    private int getDeviceDefaultSim(Context context) {
        int defaultSubId = SubscriptionManager.getDefaultSubscriptionId();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> active = sm.getActiveSubscriptionInfoList();
            if (active != null && !active.isEmpty()) {
                defaultSubId = active.get(0).getSubscriptionId();
            }
        }
        return defaultSubId;
    }
}
