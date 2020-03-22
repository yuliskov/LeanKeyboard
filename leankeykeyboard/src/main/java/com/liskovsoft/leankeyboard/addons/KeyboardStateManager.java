package com.liskovsoft.leankeyboard.addons;

import android.content.Context;
import com.liskovsoft.leankeyboard.settings.LeanKeySettings;

public class KeyboardStateManager {
    private final Context mContext;
    private final KeyboardManager mManager;
    private final LeanKeySettings mPrefs;

    public KeyboardStateManager(Context context, KeyboardManager manager) {
        mContext = context;
        mManager = manager;
        mPrefs = LeanKeySettings.instance(mContext);
    }

    public void restore() {
        int idx = mPrefs.getKeyboardIndex();
        mManager.setKeyboardIndex(idx);
    }

    public void onNextKeyboard() {
        mPrefs.setKeyboardIndex(mManager.getKeyboardIndex());
    }
}
