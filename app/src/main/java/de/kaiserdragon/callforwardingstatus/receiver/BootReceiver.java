package de.kaiserdragon.callforwardingstatus.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.kaiserdragon.callforwardingstatus.service.PhoneStateService;
import de.kaiserdragon.callforwardingstatus.service.RemoteCommandService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startForegroundService(new Intent(context, PhoneStateService.class));
        context.startForegroundService(new Intent(context, RemoteCommandService.class));
    }
}
