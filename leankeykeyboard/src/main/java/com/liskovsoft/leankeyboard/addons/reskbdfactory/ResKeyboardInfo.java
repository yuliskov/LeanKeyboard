package com.liskovsoft.leankeyboard.addons.reskbdfactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.liskovsoft.leankeyboard.addons.KeyboardInfo;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.List;

public class ResKeyboardInfo implements KeyboardInfo {
    private static boolean sNeedUpdate;
    private boolean mEnabled;
    private String mLangCode;
    private String mLangName;
    private boolean mIsAzerty;

    public static List<KeyboardInfo> getAllKeyboardInfos(Context ctx) {
        List<KeyboardInfo> result = new ArrayList<>();
        String[] langs = ctx.getResources().getStringArray(R.array.additional_languages);
        for (final String langPair : langs) {
            String[] pairs = langPair.split("\\|");
            final String langName = pairs[0];
            final String langCode = pairs[1];
            final boolean isAzerty = pairs.length >= 3 && "azerty".equals(pairs[2]);
            KeyboardInfo info = new ResKeyboardInfo();
            info.setLangName(langName);
            info.setLangCode(langCode);
            info.setIsAzerty(isAzerty);
            // sync with prefs
            syncWithPrefs(ctx, info);
            result.add(info);
        }
        sNeedUpdate = false;
        return result;
    }

    public static void updateAllKeyboardInfos(Context ctx, List<KeyboardInfo> infos) {
        for (KeyboardInfo info : infos) {
            // update prefs
            updatePrefs(ctx, info);
        }
        sNeedUpdate = true;
    }

    private static void syncWithPrefs(Context ctx, KeyboardInfo info) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        final boolean kbdEnabled = sharedPreferences.getBoolean(info.toString(), false);
        info.setEnabled(kbdEnabled);
    }

    private static void updatePrefs(Context ctx, KeyboardInfo info) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(info.toString(), info.isEnabled());
        editor.apply();
    }

    public static boolean needUpdate() {
        return sNeedUpdate;
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public String getLangCode() {
        return mLangCode;
    }

    @Override
    public String getLangName() {
        return mLangName;
    }

    @Override
    public void setLangName(String langName) {
        mLangName = langName;
    }

    @Override
    public void setLangCode(String langCode) {
        mLangCode = langCode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public boolean isAzerty() {
        return mIsAzerty;
    }

    @Override
    public void setIsAzerty(boolean isAzerty) {
        mIsAzerty = isAzerty;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("{Name: %s, Code: %s, IsAzerty: %b}", mLangName, mLangCode, mIsAzerty);
    }
}
