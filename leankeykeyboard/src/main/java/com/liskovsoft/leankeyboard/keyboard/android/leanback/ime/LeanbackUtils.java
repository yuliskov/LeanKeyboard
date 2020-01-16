package com.liskovsoft.leankeyboard.keyboard.android.leanback.ime;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;

public class LeanbackUtils {
    private static final int ACCESSIBILITY_DELAY_MS = 250;
    private static final Handler sAccessibilityHandler = new Handler();

    public static int getImeAction(EditorInfo info) {
        return info.imeOptions & (EditorInfo.IME_FLAG_NO_ENTER_ACTION | EditorInfo.IME_MASK_ACTION);
    }

    /**
     * Get class of the input
     * @param info attrs
     * @return constant e.g. {@link InputType#TYPE_CLASS_TEXT InputType.TYPE_CLASS_TEXT}
     */
    public static int getInputTypeClass(EditorInfo info) {
        return info.inputType & InputType.TYPE_MASK_CLASS;
    }

    /**
     * Get variation of the input
     * @param info attrs
     * @return constant e.g. {@link InputType#TYPE_DATETIME_VARIATION_DATE InputType.TYPE_DATETIME_VARIATION_DATE}
     */
    public static int getInputTypeVariation(EditorInfo info) {
        return info.inputType & InputType.TYPE_MASK_VARIATION;
    }

    public static boolean isAlphabet(int letter) {
        return Character.isLetter(letter);
    }

    @SuppressLint("NewApi")
    public static void sendAccessibilityEvent(final View view, boolean focusGained) {
        if (view != null && focusGained) {
            sAccessibilityHandler.removeCallbacksAndMessages(null);
            sAccessibilityHandler.postDelayed(new Runnable() {
                public void run() {
                    view.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT);
                }
            }, ACCESSIBILITY_DELAY_MS);
        }

    }
}
