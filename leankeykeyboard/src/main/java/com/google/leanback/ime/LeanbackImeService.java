package com.google.leanback.ime;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.google.android.leanback.ime.LeanbackKeyboardController;
import com.google.android.leanback.ime.LeanbackSuggestionsFactory;
import com.google.android.leanback.ime.LeanbackUtils;

public class LeanbackImeService extends InputMethodService {
    private static final boolean DEBUG = false;
    public static final String IME_CLOSE = "com.google.android.athome.action.IME_CLOSE";
    public static final String IME_OPEN = "com.google.android.athome.action.IME_OPEN";
    public static final int MAX_SUGGESTIONS = 10;
    static final int MODE_FREE_MOVEMENT = 1;
    static final int MODE_TRACKPAD_NAVIGATION = 0;
    private static final int MSG_SUGGESTIONS_CLEAR = 123;
    private static final int SUGGESTIONS_CLEAR_DELAY = 1000;
    private static final String TAG = "LbImeService";
    private boolean mEnterSpaceBeforeCommitting;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message var1) {
            if (var1.what == 123 && LeanbackImeService.this.mShouldClearSuggestions) {
                LeanbackImeService.this.mSuggestionsFactory.clearSuggestions();
                LeanbackImeService.this.mKeyboardController.updateSuggestions(LeanbackImeService.this.mSuggestionsFactory.getSuggestions());
                LeanbackImeService.this.mShouldClearSuggestions = false;
            }

        }
    };
    private LeanbackKeyboardController.InputListener mInputListener = new LeanbackKeyboardController.InputListener() {
        public void onEntry(int var1, int var2, CharSequence var3) {
            LeanbackImeService.this.handleTextEntry(var1, var2, var3);
        }
    };
    private View mInputView;
    private LeanbackKeyboardController mKeyboardController;
    private boolean mShouldClearSuggestions = true;
    private LeanbackSuggestionsFactory mSuggestionsFactory;

    @SuppressLint("NewApi")
    public LeanbackImeService() {
        if (!this.enableHardwareAcceleration()) {
            Log.w("LbImeService", "Could not enable hardware acceleration");
        }

    }

    private void clearSuggestionsDelayed() {
        if (!this.mSuggestionsFactory.shouldSuggestionsAmend()) {
            this.mHandler.removeMessages(123);
            this.mShouldClearSuggestions = true;
            this.mHandler.sendEmptyMessageDelayed(123, 1000L);
        }

    }

    private int getAmpersandLocation(InputConnection var1) {
        String var4 = this.getEditorText(var1);
        int var3 = var4.indexOf(64);
        int var2 = var3;
        if (var3 < 0) {
            var2 = var4.length();
        }

        return var2;
    }

    private int getCharLengthAfterCursor(InputConnection var1) {
        int var2 = 0;
        CharSequence var3 = var1.getTextAfterCursor(1000, 0);
        if (var3 != null) {
            var2 = var3.length();
        }

        return var2;
    }

    private int getCharLengthBeforeCursor(InputConnection var1) {
        int var2 = 0;
        CharSequence var3 = var1.getTextBeforeCursor(1000, 0);
        if (var3 != null) {
            var2 = var3.length();
        }

        return var2;
    }

    private String getEditorText(InputConnection var1) {
        StringBuilder var2 = new StringBuilder();
        CharSequence var3 = var1.getTextBeforeCursor(1000, 0);
        CharSequence var4 = var1.getTextAfterCursor(1000, 0);
        if (var3 != null) {
            var2.append(var3);
        }

        if (var4 != null) {
            var2.append(var4);
        }

        return var2.toString();
    }

    private void handleTextEntry(int var1, int var2, CharSequence var3) {
        InputConnection var5 = this.getCurrentInputConnection();
        boolean var4 = true;
        if (var5 != null) {
            boolean var6;
            switch (var1) {
                case 0:
                    this.clearSuggestionsDelayed();
                    if (this.mEnterSpaceBeforeCommitting && this.mKeyboardController.enableAutoEnterSpace()) {
                        if (LeanbackUtils.isAlphabet(var2)) {
                            var5.commitText(" ", 1);
                        }

                        this.mEnterSpaceBeforeCommitting = false;
                    }

                    var5.commitText(var3, 1);
                    var6 = var4;
                    if (var2 == 46) {
                        this.mEnterSpaceBeforeCommitting = true;
                        var6 = var4;
                    }
                    break;
                case 1:
                    this.clearSuggestionsDelayed();
                    var5.deleteSurroundingText(1, 0);
                    this.mEnterSpaceBeforeCommitting = false;
                    var6 = var4;
                    break;
                case 2:
                case 6:
                    this.clearSuggestionsDelayed();
                    if (!this.mSuggestionsFactory.shouldSuggestionsAmend()) {
                        var5.deleteSurroundingText(this.getCharLengthBeforeCursor(var5), this.getCharLengthAfterCursor(var5));
                    } else {
                        var1 = this.getAmpersandLocation(var5);
                        var5.setSelection(var1, var1);
                        var5.deleteSurroundingText(0, this.getCharLengthAfterCursor(var5));
                    }

                    var5.commitText(var3, 1);
                    this.mEnterSpaceBeforeCommitting = true;
                case 5:
                    this.sendDefaultEditorAction(false);
                    var6 = false;
                    break;
                case 3:
                case 4:
                    var3 = var5.getTextBeforeCursor(1000, 0);
                    if (var3 == null) {
                        var2 = 0;
                    } else {
                        var2 = var3.length();
                    }

                    if (var1 == 3) {
                        var1 = var2;
                        if (var2 > 0) {
                            var1 = var2 - 1;
                        }
                    } else {
                        var3 = var5.getTextAfterCursor(1000, 0);
                        var1 = var2;
                        if (var3 != null) {
                            var1 = var2;
                            if (var3.length() > 0) {
                                var1 = var2 + 1;
                            }
                        }
                    }

                    var5.setSelection(var1, var1);
                    var6 = var4;
                    break;
                case 7:
                    var5.performEditorAction(1);
                    var6 = false;
                    break;
                case 8:
                    var5.performEditorAction(2);
                    var6 = false;
                    break;
                default:
                    var6 = var4;
            }

            if (this.mKeyboardController.areSuggestionsEnabled() && var6) {
                this.mKeyboardController.updateSuggestions(this.mSuggestionsFactory.getSuggestions());
                return;
            }
        }

    }

    public View onCreateInputView() {
        this.mInputView = this.mKeyboardController.getView();
        this.mInputView.requestFocus();
        return this.mInputView;
    }

    public void onDisplayCompletions(CompletionInfo[] var1) {
        if (this.mKeyboardController.areSuggestionsEnabled()) {
            this.mShouldClearSuggestions = false;
            this.mHandler.removeMessages(123);
            this.mSuggestionsFactory.onDisplayCompletions(var1);
            this.mKeyboardController.updateSuggestions(this.mSuggestionsFactory.getSuggestions());
        }

    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public boolean onEvaluateInputViewShown() {
        return true;
    }

    public void onFinishInputView(boolean var1) {
        super.onFinishInputView(var1);
        this.sendBroadcast(new Intent("com.google.android.athome.action.IME_CLOSE"));
        this.mSuggestionsFactory.clearSuggestions();
    }

    public boolean onGenericMotionEvent(MotionEvent var1) {
        return this.isInputViewShown() && (var1.getSource() & 2097152) == 2097152 && this.mKeyboardController.onGenericMotionEvent(var1) ? true :
                super.onGenericMotionEvent(var1);
    }

    public void onHideIme() {
        this.requestHideSelf(0);
    }

    public void onInitializeInterface() {
        this.mKeyboardController = new LeanbackKeyboardController(this, this.mInputListener);
        this.mEnterSpaceBeforeCommitting = false;
        this.mSuggestionsFactory = new LeanbackSuggestionsFactory(this, 10);
    }

    public boolean onKeyDown(int var1, KeyEvent var2) {
        return this.isInputViewShown() && this.mKeyboardController.onKeyDown(var1, var2) ? true : super.onKeyDown(var1, var2);
    }

    public boolean onKeyUp(int var1, KeyEvent var2) {
        return this.isInputViewShown() && this.mKeyboardController.onKeyUp(var1, var2) ? true : super.onKeyUp(var1, var2);
    }

    public boolean onShowInputRequested(int var1, boolean var2) {
        return true;
    }

    public int onStartCommand(Intent var1, int var2, int var3) {
        if (var1 != null) {
            super.onStartCommand(var1, var2, var3);
            if (var1.getBooleanExtra("restart", false)) {
                Log.e("LeanbackImeService", "Service->onStartCommand: trying to restart service");
                LeanbackKeyboardController var4 = this.mKeyboardController;
                if (var4 != null) {
                    var4.updateAddonKeyboard();
                }
            }
        }

        return 1;
    }

    public void onStartInput(EditorInfo var1, boolean var2) {
        super.onStartInput(var1, var2);
        this.mEnterSpaceBeforeCommitting = false;
        this.mSuggestionsFactory.onStartInput(var1);
        this.mKeyboardController.onStartInput(var1);
    }

    public void onStartInputView(EditorInfo var1, boolean var2) {
        super.onStartInputView(var1, var2);
        this.mKeyboardController.onStartInputView();
        this.sendBroadcast(new Intent("com.google.android.athome.action.IME_OPEN"));
        if (this.mKeyboardController.areSuggestionsEnabled()) {
            this.mSuggestionsFactory.createSuggestions();
            this.mKeyboardController.updateSuggestions(this.mSuggestionsFactory.getSuggestions());
            InputConnection var4 = this.getCurrentInputConnection();
            if (var4 != null) {
                String var3 = this.getEditorText(var4);
                var4.deleteSurroundingText(this.getCharLengthBeforeCursor(var4), this.getCharLengthAfterCursor(var4));
                var4.commitText(var3, 1);
            }
        }

    }
}
