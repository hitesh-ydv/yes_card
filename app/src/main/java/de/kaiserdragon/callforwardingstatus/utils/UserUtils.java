package de.kaiserdragon.callforwardingstatus.utils;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class UserUtils {

    // SharedPreferences file name
    private static final String PREFS_NAME = "USER_PREFS";
    // Key for storing UUID
    private static final String KEY_USER_ID = "USER_ID";

    // Generate or retrieve UUID
    public static String getOrCreateUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null) {
            // First launch → create new UUID
            userId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_USER_ID, userId).apply();
        }
        return userId;
    }
}
