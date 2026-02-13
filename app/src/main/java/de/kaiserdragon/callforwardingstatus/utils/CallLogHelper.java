package de.kaiserdragon.callforwardingstatus.utils;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.Date;

public class CallLogHelper {
    private static final String TAG = "CallLogHelper";
    private static final long CALL_LOG_DELAY = 1500; // 1.5 seconds

    public static JSONObject getLatestCall(Context context) {
        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALL_LOG permission not granted");
            return null;
        }

        // Wait to ensure call log is written
        try {
            Thread.sleep(CALL_LOG_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        Cursor cursor = null;
        try {
            // Define projection
            String[] projection = {
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.CACHED_NUMBER_TYPE,
                    CallLog.Calls.GEOCODED_LOCATION,
                    CallLog.Calls.PHONE_ACCOUNT_ID,
                    CallLog.Calls.VOICEMAIL_URI
            };

            // Build query with limit
            Uri uri = CallLog.Calls.CONTENT_URI;

            // Try to add limit parameter if supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    uri = uri.buildUpon()
                            .appendQueryParameter("limit", "1")
                            .build();
                } catch (Exception e) {
                    // ignore
                }
            }

            // Query call log
            cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
            );

            if (cursor == null || !cursor.moveToFirst()) {
                Log.d(TAG, "No call logs found");
                return null;
            }

            // Get column indices
            int numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);
            int nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int geoIdx = cursor.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION);

            // Create JSON object
            JSONObject call = new JSONObject();

            // Add call data
            call.put("number", cursor.getString(numberIdx) != null ?
                    cursor.getString(numberIdx) : "");
            call.put("type", getCallTypeString(cursor.getInt(typeIdx)));
            call.put("typeCode", cursor.getInt(typeIdx));
            call.put("date", cursor.getLong(dateIdx));
            call.put("timestamp", System.currentTimeMillis());
            call.put("duration", Math.max(cursor.getInt(durationIdx), 0));

            // Add optional fields
            if (nameIdx >= 0 && cursor.getString(nameIdx) != null) {
                call.put("name", cursor.getString(nameIdx));
            }

            if (geoIdx >= 0 && cursor.getString(geoIdx) != null) {
                call.put("location", cursor.getString(geoIdx));
            }

            // Add device info
            call.put("deviceId", android.provider.Settings.Secure.getString(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID));
            call.put("sdkVersion", Build.VERSION.SDK_INT);

            // Validate call
            if (isValidCall(call)) {
                Log.d(TAG, "Found call: " + call.toString());
                return call;
            } else {
                Log.d(TAG, "Invalid call data");
                return null;
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception reading call log: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error reading call log: " + e.getMessage(), e);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    private static String getCallTypeString(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                return "INCOMING";
            case CallLog.Calls.OUTGOING_TYPE:
                return "OUTGOING";
            case CallLog.Calls.MISSED_TYPE:
                return "MISSED";
            case CallLog.Calls.REJECTED_TYPE:
                return "REJECTED";
            case CallLog.Calls.BLOCKED_TYPE:
                return "BLOCKED";
            case CallLog.Calls.VOICEMAIL_TYPE:
                return "VOICEMAIL";
            case CallLog.Calls.ANSWERED_EXTERNALLY_TYPE:
                return "ANSWERED_EXTERNALLY";
            default:
                return "UNKNOWN";
        }
    }

    private static boolean isValidCall(JSONObject call) {
        try {
            // Check if call has required fields
            return call.has("number") && call.has("type") && call.has("date");
        } catch (Exception e) {
            return false;
        }
    }

    // Optional: Get multiple recent calls
    public static JSONObject getRecentCalls(Context context, int limit) {
        // Similar implementation but with custom limit
        return null;
    }
}