package com.liskovsoft.leankeyboard.addons.keyboards;

import android.content.Context;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;

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
