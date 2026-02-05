package de.kaiserdragon.callforwardingstatus.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

public class SimUtils {

    public static String getPhoneNumber(Context context, SubscriptionInfo info) {

        if (info == null) return "";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SubscriptionManager sm =
                        (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                return sm != null
                        ? safe(sm.getPhoneNumber(info.getSubscriptionId()))
                        : "";
            } else {
                // ANDROID 11 PATH (Most likely empty)
                return safe(info.getNumber());
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safe(String value) {
        return TextUtils.isEmpty(value) ? "" : value;
    }
}
