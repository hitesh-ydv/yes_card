package de.kaiserdragon.callforwardingstatus;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import org.json.JSONObject;

import de.kaiserdragon.callforwardingstatus.service.AppLifecycleObserver;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new AppLifecycleObserver(this));
    }
}

