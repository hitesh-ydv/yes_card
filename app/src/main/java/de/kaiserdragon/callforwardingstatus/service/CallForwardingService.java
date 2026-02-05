package de.kaiserdragon.callforwardingstatus.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.kaiserdragon.callforwardingstatus.R;
import de.kaiserdragon.callforwardingstatus.utils.UserUtils;
import de.kaiserdragon.callforwardingstatus.helper.DatabaseHelper;
import de.kaiserdragon.callforwardingstatus.FirebaseForwardingResponse;


public class CallForwardingService extends Service {

    private static final String TAG = "CallForwardService";

    private PowerManager.WakeLock wakeLock;

    private static final int WAKELOCK_TIMEOUT_MS = 30_000; // 30s


    @Override
    public void onCreate() {
        super.onCreate();
        acquireWakeLock();
        startForegroundNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "Service started");

        boolean remote = intent.getBooleanExtra("remote", false);
        boolean enable = intent.getBooleanExtra("cfi", false);
        int sim = intent.getIntExtra("sim", -1);
        String number = intent.getStringExtra("number");

        executeForwarding(remote, enable, number, sim);

        return START_NOT_STICKY;
    }

    private void executeForwarding(boolean isRemote, boolean enableForward, String number, int sim) {

        Log.d(TAG, "Executing call forwarding: enable=" + enableForward + " number=" + number);

        if (number == null || number.isEmpty()) {
            stopSelfSafely();
            return;
        }

        DatabaseHelper db = new DatabaseHelper(this);
        String[] config = db.getSelected();
        db.close();

        if (sim <= 0) {
            sim = getDeviceDefaultSim();
        }

        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        TelephonyManager simTelephony = tm.createForSubscriptionId(sim);

        String ussd = enableForward ? "*21*" + number + "#" : "#21#";

        Log.d(TAG, "Sending USSD: " + ussd);

        Handler handler = new Handler();

        TelephonyManager.UssdResponseCallback callback = new TelephonyManager.UssdResponseCallback() {
            @Override
            public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                Log.d(TAG, "USSD Success: " + response);

                if (isRemote) {
                    sendAck(enableForward, response.toString());
                }

                stopSelfSafely();
            }

            @Override
            public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                Log.e(TAG, "USSD Failed: " + failureCode);

                if (isRemote) {
                    sendAck(false, "Failed: " + failureCode);
                }

                stopSelfSafely();
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            simTelephony.sendUssdRequest(ussd, callback, handler);
        } else {
            if (isRemote) sendAck(false, "CALL_PHONE missing");
            stopSelfSafely();
        }
    }

    private int getDeviceDefaultSim() {
        return android.telephony.SubscriptionManager.getDefaultSubscriptionId();
    }

    private void sendAck(boolean enabled, String msg) {
        String userId = UserUtils.getOrCreateUserId(this);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("commands")
                .child(userId)
                .child("response");

        ref.setValue(new FirebaseForwardingResponse(
                enabled ? "ENABLED" : "DISABLED",
                msg,
                System.currentTimeMillis()
        ));
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "de.kaiserdragon.callforwarding:CallForwardingWakeLock");
            // Acquire with timeout to be safe
            wakeLock.acquire(WAKELOCK_TIMEOUT_MS);
            Log.d(TAG, "WakeLock acquired for " + WAKELOCK_TIMEOUT_MS + "ms");
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "WakeLock released");
            }
        } catch (Exception e) {
            Log.w(TAG, "releaseWakeLock failed", e);
        }
    }

    private void startForegroundNotification() {

        String channelId = "call_forward_channel";
        String channelName = "Call Forward Executor";

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(ch);
        }

        Notification notification =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Call Forwarding")
                        .setContentText("Processing remote command…")
                        .setOngoing(true)
                        .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    3001,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(3001, notification);
        }
    }

    private void stopSelfSafely() {
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
