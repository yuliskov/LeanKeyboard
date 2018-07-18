package com.liskovsoft.keyboardaddons;

import android.content.Context;
import com.liskovsoft.utils.LeanKeyPreferences;

public class KeyboardStateManager {
    private final Context mContext;
    private final KeyboardManager mManager;
    private final LeanKeyPreferences mPrefs;

    public KeyboardStateManager(Context context, KeyboardManager manager) {
        mContext = context;
        mManager = manager;
        mPrefs = LeanKeyPreferences.instance(mContext);
    }

    public void restore() {
        int idx = mPrefs.getKeyboardIndex();
        mManager.setKeyboardIndex(idx);
    }

    public void onNextKeyboard() {
        mPrefs.setKeyboardIndex(mManager.getKeyboardIndex());
    }
}
