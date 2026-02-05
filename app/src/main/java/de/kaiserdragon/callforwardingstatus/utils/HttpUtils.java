package de.kaiserdragon.callforwardingstatus.utils;

import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {

    public static void postJson(String apiUrl, JSONObject jsonObject) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(jsonObject.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d("HttpUtils", "Response Code: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
