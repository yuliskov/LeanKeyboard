package com.liskovsoft.leankeyboard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class LeanKeySettings {
    private static final String APP_RUN_ONCE = "appRunOnce";
    private static final String BOOTSTRAP_SELECTED_LANGUAGE = "bootstrapSelectedLanguage";
    private static final String APP_KEYBOARD_INDEX = "appKeyboardIndex";
    private static final String FORCE_SHOW_KEYBOARD = "forceShowKeyboard";
    private static final String ENLARGE_KEYBOARD = "enlargeKeyboard";
    private static final String KEYBOARD_THEME = "keyboardTheme";
    public static final String DEFAULT_THEME_ID = "Default";
    public static final String DARK_THEME_ID = "Dark";
    private static LeanKeySettings sInstance;
    private final Context mContext;
    private SharedPreferences mPrefs;

    public static LeanKeySettings instance(Context ctx) {
        if (sInstance == null)
            sInstance = new LeanKeySettings(ctx);
        return sInstance;
    }

    public LeanKeySettings(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public boolean isRunOnce() {
        return mPrefs.getBoolean(APP_RUN_ONCE, false);
    }

    public void setRunOnce(boolean runOnce) {
        mPrefs.edit()
                .putBoolean(APP_RUN_ONCE, runOnce)
                .apply();
    }

    public void setPreferredLanguage(String name) {
        mPrefs.edit()
                .putString(BOOTSTRAP_SELECTED_LANGUAGE, name)
                .apply();
    }

    public String getPreferredLanguage() {
        return mPrefs.getString(BOOTSTRAP_SELECTED_LANGUAGE, "");
    }

    public int getKeyboardIndex() {
        return mPrefs.getInt(APP_KEYBOARD_INDEX, 0);
    }

    public void setKeyboardIndex(int idx) {
        mPrefs.edit()
                .putInt(APP_KEYBOARD_INDEX, idx)
                .apply();
    }

    public boolean getForceShowKeyboard() {
        return mPrefs.getBoolean(FORCE_SHOW_KEYBOARD, true);
    }

    public void setForceShowKeyboard(boolean force) {
        mPrefs.edit()
                .putBoolean(FORCE_SHOW_KEYBOARD, force)
                .apply();
    }

    public boolean getEnlargeKeyboard() {
        return mPrefs.getBoolean(ENLARGE_KEYBOARD, false);
    }

    public void setEnlargeKeyboard(boolean enlarge) {
        mPrefs.edit()
                .putBoolean(ENLARGE_KEYBOARD, enlarge)
                .apply();
    }

    public void setCurrentTheme(String theme) {
        mPrefs.edit()
                .putString(KEYBOARD_THEME, theme)
                .apply();
    }

    public String getCurrentTheme() {
        return mPrefs.getString(KEYBOARD_THEME, DEFAULT_THEME_ID);
    }
}
