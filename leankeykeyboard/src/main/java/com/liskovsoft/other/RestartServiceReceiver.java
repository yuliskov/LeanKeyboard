package com.liskovsoft.other;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.util.*;

import java.util.*;

public class RestartServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        sendMessageToService(context);
                
    }

    private void sendMessageToService(Context context) {
        Log.e("RestartServiceReceiver", "Sending message to the service");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getPackageName(context), "com.google.leanback.ime.LeanbackImeService"));
        intent.putExtra("restart", true);
        context.startService(intent);
    }

    private void onStartCommand(Intent intent) {
        if (intent.getBooleanExtra("restart", false)) {
            System.out.println("Restarting service");
        }
    }

    private void killThisPackageProcess(Context context) {
        Log.e("RestartServiceReceiver", "Attempting to kill org.liskovsoft.androidtv.rukeyboard process");
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.killBackgroundProcesses(getPackageName(context));
    }

    private void restartService(Context context) {
        // START YOUR SERVICE HERE
        Log.e("RestartServiceReceiver", "Restarting Service");
        //final Class<?> serviceClass = classForName("com.google.leanback.ime.LeanbackImeService");
        //Intent serviceIntent = new Intent(context.getApplicationContext(), serviceClass);
        Intent serviceIntent = new Intent();
        serviceIntent.setComponent(new ComponentName(getPackageName(context), "com.google.leanback.ime.LeanbackImeService"));
        context.stopService(serviceIntent);
        context.startService(serviceIntent);
    }

    private Class<?> classForName(String clazz) {
        Class<?> serviceClass;
        try {
            serviceClass = Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return serviceClass;
    }

    private void switchLocale(Context ctx) {
        Log.e("RestartServiceReceiver", "Trying to switch locale back and forward");
        Locale savedLocale = Locale.getDefault();
        trySwitchLocale(ctx, new Locale("ru"));
        trySwitchLocale(ctx, savedLocale);
    }

    private void trySwitchLocale(Context ctx, Locale locale) {
        Locale.setDefault(locale);
        Configuration config = ctx.getResources().getConfiguration();
        config.locale = locale;
        ctx.getResources().updateConfiguration(config,
                ctx.getResources().getDisplayMetrics());
    }

    private String getPackageName(Context ctx) {
        return ctx.getPackageName();
    }
}