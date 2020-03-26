package com.liskovsoft.leankeyboard.helpers;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.liskovsoft.leankeyboard.utils.LocaleUtility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {
    /**
     * Simple wildcard matching routine. Implemented without regex. So you may expect huge performance boost.
     * @param host
     * @param mask
     * @return
     */
    public static boolean matchSubstr(String host, String mask) {
        String[] sections = mask.split("\\*");
        String text = host;
        for (String section : sections) {
            int index = text.indexOf(section);
            if (index == -1) {
                return false;
            }
            text = text.substring(index + section.length());
        }
        return true;
    }

    public static boolean matchSubstrNoCase(String host, String mask) {
        return matchSubstr(host.toLowerCase(), mask.toLowerCase());
    }

    public static InputStream getAsset(Context ctx, String fileName) {
        InputStream is = null;
        try {
            is = ctx.getAssets().open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }

    public static String encodeURI(byte[] data) {
        try {
            // make behaviour of java uri-encode the same as javascript's one
            return URLEncoder.encode(new String(data, "UTF-8"), "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean floatEquals(float num1, float num2) {
        float epsilon = 0.1f;
        return Math.abs(num1 - num2) < epsilon;
    }

    public static String getDeviceName() {
        return String.format("%s (%s)", Build.MODEL, Build.PRODUCT);
    }

    public static boolean deviceMatch(String[] devicesToProcess) {
        String thisDeviceName = Helpers.getDeviceName();
        for (String deviceName : devicesToProcess) {
            boolean match = matchSubstrNoCase(thisDeviceName, deviceName);
            if (match) {
                return true;
            }
        }
        return false;
    }

    public static String toString(Throwable ex) {
        if (ex instanceof IllegalStateException &&
                ex.getCause() != null) {
            ex = ex.getCause();
        }
        return String.format("%s: %s", ex.getClass().getCanonicalName(), ex.getMessage());
    }

    public static String toString(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        return result;
    }

    public static InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    public static void postOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static String unixToLocalDate(Context ctx, String timestamp) {
        Locale current = LocaleUtility.getSystemLocale(ctx);
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, current);
        Date date;
        if (timestamp == null) {
            date = new Date();
        } else {
            date = new Date((long) Integer.parseInt(timestamp) * 1000);
        }
        return dateFormat.format(date);
    }

    public static String runMultiMatcher(String input, String... patterns) {
        if (input == null) {
            return null;
        }

        Pattern regex;
        Matcher matcher;
        String result = null;
        for (String pattern : patterns) {
            regex = Pattern.compile(pattern);
            matcher = regex.matcher(input);

            if (matcher.find()) {
                result = matcher.group(matcher.groupCount()); // get last group
                break;
            }
        }

        return result;
    }

    public static boolean isCallable(Context ctx, Intent intent) {
        List<ResolveInfo> list = ctx.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static String getSimpleClassName(String name) {
        if (name == null) {
            return null;
        }

        return name.substring(name.lastIndexOf('.') + 1);
    }

    private static void killThisPackageProcess(Context context) {
        Log.e("RestartServiceReceiver", "Attempting to kill org.liskovsoft.androidtv.rukeyboard process");
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.killBackgroundProcesses(getPackageName(context));
    }

    private static void restartService(Context context) {
        // START YOUR SERVICE HERE
        Log.e("RestartServiceReceiver", "Restarting Service");
        //final Class<?> serviceClass = classForName("com.google.leanback.ime.LeanbackImeService");
        //Intent serviceIntent = new Intent(context.getApplicationContext(), serviceClass);
        Intent serviceIntent = new Intent();
        serviceIntent.setComponent(new ComponentName(getPackageName(context), "com.google.leanback.ime.LeanbackImeService"));
        context.stopService(serviceIntent);
        context.startService(serviceIntent);
    }

    public static Class<?> classForName(String clazz) {
        Class<?> serviceClass;
        try {
            serviceClass = Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return serviceClass;
    }
    
    public static String getPackageName(Context ctx) {
        return ctx.getPackageName();
    }

    public static void startActivity(Context context, Class<?> activityClass) {
        try {
            Intent intent = new Intent(context, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            MessageHelpers.showLongMessage(context, "Can't start: " + e.getMessage());
        }
    }

    public static boolean startIntent(final Context context, final Intent intent) {
        if (intent == null) {
            return false;
        }

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            return false;
        }

        return true;
    }

    public static boolean isGenymotion() {
        String deviceName = getDeviceName();

        return deviceName.contains("(vbox86p)");
    }
}
