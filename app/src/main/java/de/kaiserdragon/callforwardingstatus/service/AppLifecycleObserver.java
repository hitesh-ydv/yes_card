package de.kaiserdragon.callforwardingstatus.service;


// Android life-cycle imports
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

// JSON emit ke liye
import org.json.JSONObject;

// Socket.io client class
import io.socket.client.Socket;

// ⚡ Your own helper classes
import de.kaiserdragon.callforwardingstatus.utils.SocketHandler;
import de.kaiserdragon.callforwardingstatus.utils.UserUtils;

public class AppLifecycleObserver implements Application.ActivityLifecycleCallbacks {

    private int runningActivities = 0;
    private final Context context;

    public AppLifecycleObserver(Context context) {
        this.context = context;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (runningActivities == 0) sendStatus("online");
        runningActivities++;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        runningActivities--;
        if (runningActivities == 0) sendStatus("offline");
    }

    private void sendStatus(String status) {
        try {
            Socket socket = SocketHandler.getSocket();
            if (socket == null) return;

            String userId = UserUtils.getOrCreateUserId(context);

            JSONObject data = new JSONObject();
            data.put("userId", userId);
            data.put("status", status);

            socket.emit("user_status", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Empty overrides
    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivityDestroyed(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}
}

