package de.kaiserdragon.callforwardingstatus.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

public class CallLogHelper {

    public static JSONObject getLatestCall(Context context) {

        // Permission check (important for Android 12+)
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            return null; // cannot read call logs
        }

        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " DESC"  // remove LIMIT (OEMs ignore it)
        );

        if (cursor == null) return null;

        JSONObject json = null;

        if (cursor.moveToFirst()) {
            try {
                json = new JSONObject();

                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));

                // Android 12+ sometimes gives duration = 0 for outgoing
                if (type == CallLog.Calls.OUTGOING_TYPE && duration == 0) {
                    duration = Math.max(duration, 1);
                }

                json.put("number", number);
                json.put("date", date);
                json.put("duration", duration);
                json.put("type", parseType(type));

            } catch (Exception ignored) {
            } finally {
                cursor.close();
            }
        } else {
            cursor.close();
        }

        return json;
    }


    private static String parseType(int type) {
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
            default:
                return "UNKNOWN";
        }
    }
}
