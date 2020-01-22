package com.liskovsoft.leankeyboard.keyboard.android.leanback.ime;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.liskovsoft.leankeyboard.keyboard.leanback.ime.LeanbackImeService;

public class LeanbackUtils {
    private static final int ACCESSIBILITY_DELAY_MS = 250;
    private static final String EDITOR_LABEL = "label";
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
            sAccessibilityHandler.postDelayed(() -> view.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT), ACCESSIBILITY_DELAY_MS);
        }

    }

    public static int getAmpersandLocation(InputConnection connection) {
        String text = getEditorText(connection);
        int pos = text.indexOf(64);
        if (pos < 0) { // not found
            pos = text.length();
        }

        return pos;
    }

    public static int getCharLengthAfterCursor(InputConnection connection) {
        int len = 0;
        CharSequence after = connection.getTextAfterCursor(1000, 0);
        if (after != null) {
            len = after.length();
        }

        return len;
    }

    public static int getCharLengthBeforeCursor(InputConnection connection) {
        int len = 0;
        CharSequence before = connection.getTextBeforeCursor(1000, 0);
        if (before != null) {
            len = before.length();
        }

        return len;
    }

    public static String getEditorText(InputConnection connection) {
        StringBuilder result = new StringBuilder();
        CharSequence before = connection.getTextBeforeCursor(1000, 0);
        CharSequence after = connection.getTextAfterCursor(1000, 0);
        if (before != null) {
            result.append(before);
        }

        if (after != null) {
            result.append(after);
        }

        return result.toString();
    }

    public static void sendEnterKey(InputConnection connection) {
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
    }

    public static String getEditorLabel(EditorInfo info) {
        if (info != null && info.extras != null && info.extras.containsKey(EDITOR_LABEL)) {
            return info.extras.getString(EDITOR_LABEL);
        }

        return null;
    }
}
