package de.kaiserdragon.callforwardingstatus.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.kaiserdragon.callforwardingstatus.R;
import de.kaiserdragon.callforwardingstatus.utils.UserUtils;

public class RemoteCommandService extends Service {

    private static final String TAG = "RemoteCommandService";
    private ValueEventListener listener;
    private DatabaseReference commandRef;

    @Override
    public void onCreate() {
        super.onCreate();

        startForegroundNotification();

        String userId = UserUtils.getOrCreateUserId(this);

        commandRef = FirebaseDatabase.getInstance()
                .getReference("commands")
                .child(userId);

        listenForCommands();
    }

    private void listenForCommands() {

        listener = new ValueEventListener() {
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

                Intent svc = new Intent(RemoteCommandService.this, CallForwardingService.class);
                svc.putExtra("remote", true);
                svc.putExtra("cfi", forwardEnabled);
                svc.putExtra("sim", simId);
                svc.putExtra("number", phoneNumber);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(svc);
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

        commandRef.addValueEventListener(listener);
    }

    private void startForegroundNotification() {

        String channelId = "remote_channel";
        String channelName = "Remote Command Listener";

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);

            nm.createNotificationChannel(channel);
        }

        int icon = R.drawable.ic_launcher_foreground;
        if (icon == 0) icon = android.R.drawable.stat_sys_warning;

        Notification notification =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(icon)
                        .setContentTitle("System Active")
                        .setContentText("Listening for forwarding commands")
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setOngoing(true)
                        .build();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                        2001,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                );
            } else {
                startForeground(2001, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Foreground start failed, retrying default mode", e);
            startForeground(2001, notification);
        }
    }

    private void clearPendingCommand() {
        commandRef.setValue(null); // delete in one shot
    }

    private int getStoredSim() {
        return getSharedPreferences("SIM_PREFERENCES", MODE_PRIVATE)
                .getInt("SELECTED_SIM_ID", -1);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listener != null && commandRef != null) {
            commandRef.removeEventListener(listener);
        }
    }
}
