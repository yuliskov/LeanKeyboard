package com.liskovsoft.leankeyboard.ime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import androidx.core.text.BidiFormatter;

public class LeanbackUtils {
    private static final int ACCESSIBILITY_DELAY_MS = 250;
    private static final String EDITOR_LABEL = "label";
    private static final Handler sAccessibilityHandler = new Handler();
    private static final String TAG = LeanbackUtils.class.getSimpleName();

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

    public static DisplayMetrics createMetricsFrom(Context context, float factor) {
        DisplayMetrics metrics = null;
        Object service = context.getSystemService(Context.WINDOW_SERVICE);

        if (service instanceof WindowManager) {
            WindowManager manager = (WindowManager) service;
            metrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(metrics);
            Log.d(TAG, metrics.toString());

            // new values
            metrics.density *= factor;
            metrics.densityDpi *= factor;
            metrics.heightPixels *= factor;
            metrics.widthPixels *= factor;
            metrics.scaledDensity *= factor;
            metrics.xdpi *= factor;
            metrics.ydpi *= factor;
        }

        return metrics;
    }

    public static void showKeyboardPicker(Context context) {
        if (context != null) {
            InputMethodManager imeManager = (InputMethodManager) context.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imeManager != null) {
                imeManager.showInputMethodPicker();
            }
        }
    }

    public static int getRtlLenAfterCursor(CharSequence text) {
        if (text == null || text.length() == 0) {
            return 0;
        }

        BidiFormatter formatter = BidiFormatter.getInstance();
        int len = 0;

        for (int i = 1; i < text.length(); i++) {
            CharSequence charSequence = text.subSequence(len, i);
            if (formatter.isRtl(charSequence)) {
                len++;
            } else {
                break;
            }
        }

        return len;
    }

    public static int getRtlLenBeforeCursor(CharSequence text) {
        if (text == null || text.length() == 0) {
            return 0;
        }

        BidiFormatter formatter = BidiFormatter.getInstance();
        int len = 0;

        for (int i = text.length(); i > 0; i--) {
            CharSequence charSequence = text.subSequence(i-1, i);
            if (formatter.isRtl(charSequence)) {
                len++;
            } else {
                break;
            }
        }

        return len;
    }
}
