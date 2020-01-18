package com.liskovsoft.leankeyboard.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build.VERSION;

import java.util.Locale;

public class LocaleUtility extends LocaleScript {
    public static Locale getSystemLocale(Context context) {
        return getSystemLocale(context.getResources().getConfiguration());
    }

    public static void setSystemLocale(Context context, Locale locale) {
        setSystemLocale(context.getResources().getConfiguration(), locale);
    }

    @SuppressWarnings("deprecation")
    public static void setSystemLocale(Configuration config, Locale locale) {
        if (VERSION.SDK_INT < 24) {
            config.locale = locale;
        } else {
            config.setLocale(locale);
        }
    }

    @SuppressWarnings("deprecation")
    public static Locale getSystemLocale(Configuration config) {
        if (VERSION.SDK_INT < 24) {
            return config.locale;
        } else {
            return config.getLocales().get(0);
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/40221711/android-context-getresources-updateconfiguration-deprecated/40704077#40704077">Modern Solution</a>
     */
    @SuppressWarnings("deprecation")
    public static void forceLocaleOld(Context ctx, Locale locale) {
        Locale.setDefault(locale);
        Configuration config = ctx.getResources().getConfiguration();
        LocaleUtility.setSystemLocale(config, locale);
        ctx.getResources().updateConfiguration(config,
                ctx.getResources().getDisplayMetrics());
    }
}
