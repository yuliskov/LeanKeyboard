package com.liskovsoft.leankeyboard.keyboard.leanback.ime;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.LeanbackKeyboardController;
import com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.LeanbackKeyboardController.InputListener;
import com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.LeanbackKeyboardView;
import com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.LeanbackSuggestionsFactory;
import com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.LeanbackUtils;
import com.liskovsoft.leankeyboard.utils.LangUpdater;

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
    private View mInputView;
    private LeanbackKeyboardController mKeyboardController;
    private boolean mShouldClearSuggestions = true;
    private LeanbackSuggestionsFactory mSuggestionsFactory;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SUGGESTIONS_CLEAR && mShouldClearSuggestions) {
                mSuggestionsFactory.clearSuggestions();
                mKeyboardController.updateSuggestions(mSuggestionsFactory.getSuggestions());
                mShouldClearSuggestions = false;
            }

        }
    };

    private LeanbackKeyboardController.InputListener mInputListener = new LeanbackKeyboardController.InputListener() {
        @Override
        public void onEntry(int type, int keyCode, CharSequence text) {
            handleTextEntry(type, keyCode, text);
        }
    };

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public LeanbackImeService() {
        if (VERSION.SDK_INT < 21 && !enableHardwareAcceleration()) {
            Log.w("LbImeService", "Could not enable hardware acceleration");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        LangUpdater langUpdater = new LangUpdater(this);
        langUpdater.update();
    }

    private void clearSuggestionsDelayed() {
        if (!mSuggestionsFactory.shouldSuggestionsAmend()) {
            mHandler.removeMessages(MSG_SUGGESTIONS_CLEAR);
            mShouldClearSuggestions = true;
            mHandler.sendEmptyMessageDelayed(MSG_SUGGESTIONS_CLEAR, SUGGESTIONS_CLEAR_DELAY);
        }

    }

    private int getAmpersandLocation(InputConnection connection) {
        String text = getEditorText(connection);
        int pos = text.indexOf(64);
        if (pos < 0) { // not found
            pos = text.length();
        }

        return pos;
    }

    private int getCharLengthAfterCursor(InputConnection connection) {
        int len = 0;
        CharSequence after = connection.getTextAfterCursor(1000, 0);
        if (after != null) {
            len = after.length();
        }

        return len;
    }

    private int getCharLengthBeforeCursor(InputConnection connection) {
        int len = 0;
        CharSequence before = connection.getTextBeforeCursor(1000, 0);
        if (before != null) {
            len = before.length();
        }

        return len;
    }

    private String getEditorText(InputConnection connection) {
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

    private void handleTextEntry(final int type, final int keyCode, final CharSequence text) {
        final InputConnection connection = getCurrentInputConnection();
        if (connection != null) {
            boolean updateSuggestions;
            switch (type) {
                case InputListener.ENTRY_TYPE_STRING:
                    clearSuggestionsDelayed();
                    if (mEnterSpaceBeforeCommitting && mKeyboardController.enableAutoEnterSpace()) {
                        if (LeanbackUtils.isAlphabet(keyCode)) {
                            connection.commitText(" ", 1);
                        }

                        mEnterSpaceBeforeCommitting = false;
                    }

                    connection.commitText(text, 1);
                    updateSuggestions = true;
                    if (keyCode == LeanbackKeyboardView.ASCII_PERIOD) {
                        mEnterSpaceBeforeCommitting = true;
                    }
                    break;
                case InputListener.ENTRY_TYPE_BACKSPACE:
                    clearSuggestionsDelayed();
                    connection.deleteSurroundingText(1, 0);
                    mEnterSpaceBeforeCommitting = false;
                    updateSuggestions = true;
                    break;
                case InputListener.ENTRY_TYPE_SUGGESTION:
                case InputListener.ENTRY_TYPE_VOICE:
                    clearSuggestionsDelayed();
                    if (!mSuggestionsFactory.shouldSuggestionsAmend()) {
                        connection.deleteSurroundingText(this.getCharLengthBeforeCursor(connection), this.getCharLengthAfterCursor(connection));
                    } else {
                        int location = this.getAmpersandLocation(connection);
                        connection.setSelection(location, location);
                        connection.deleteSurroundingText(0, this.getCharLengthAfterCursor(connection));
                    }

                    connection.commitText(text, 1);
                    mEnterSpaceBeforeCommitting = true;
                case InputListener.ENTRY_TYPE_ACTION:  // NOTE: user presses Go, Send, Search etc
                    boolean result = sendDefaultEditorAction(true);

                    if (result) {
                        hideWindow(); // NOTE: SmartYouTubeTV hide kbd on search page fix
                    } else {
                        sendEnterKey(connection);
                    }

                    updateSuggestions = false;
                    break;
                case InputListener.ENTRY_TYPE_LEFT:
                case InputListener.ENTRY_TYPE_RIGHT:
                    CharSequence textBeforeCursor = connection.getTextBeforeCursor(1000, 0);
                    int len;
                    if (textBeforeCursor == null) {
                        len = 0;
                    } else {
                        len = textBeforeCursor.length();
                    }

                    int index;
                    if (type == InputListener.ENTRY_TYPE_LEFT) {
                        index = len;
                        if (len > 0) {
                            index = len - 1;
                        }
                    } else {
                        textBeforeCursor = connection.getTextAfterCursor(1000, 0);
                        index = len;
                        if (textBeforeCursor != null && textBeforeCursor.length() > 0) {
                            index = len + 1;
                        }
                    }

                    connection.setSelection(index, index);
                    updateSuggestions = true;
                    break;
                case InputListener.ENTRY_TYPE_DISMISS:
                    connection.performEditorAction(EditorInfo.IME_ACTION_NONE);
                    updateSuggestions = false;
                    break;
                case InputListener.ENTRY_TYPE_VOICE_DISMISS:
                    connection.performEditorAction(EditorInfo.IME_ACTION_GO);
                    updateSuggestions = false;
                    break;
                default:
                    updateSuggestions = true;
            }

            if (mKeyboardController.areSuggestionsEnabled() && updateSuggestions) {
                mKeyboardController.updateSuggestions(mSuggestionsFactory.getSuggestions());
            }
        }
    }

    private void sendEnterKey(InputConnection connection) {
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
    }

    @Override
    public View onCreateInputView() {
        mInputView = mKeyboardController.getView();
        mInputView.requestFocus();
        return mInputView;
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] infos) {
        if (mKeyboardController.areSuggestionsEnabled()) {
            mShouldClearSuggestions = false;
            mHandler.removeMessages(123);
            mSuggestionsFactory.onDisplayCompletions(infos);
            mKeyboardController.updateSuggestions(this.mSuggestionsFactory.getSuggestions());
        }

    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    /**
     * At this point, decision whether to show kbd taking place<br/>
     * <a href="https://stackoverflow.com/questions/7449283/is-it-possible-to-have-both-physical-keyboard-and-soft-keyboard-active-at-the-sa">More info</a>
     * @return whether to show kbd
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public boolean onEvaluateInputViewShown() {
        //return mKeyboardController.showInputView();
        return true;
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        this.sendBroadcast(new Intent(IME_CLOSE));
        this.mSuggestionsFactory.clearSuggestions();
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return isInputViewShown() &&
                (event.getSource() & InputDevice.SOURCE_TOUCH_NAVIGATION) == InputDevice.SOURCE_TOUCH_NAVIGATION &&
                mKeyboardController.onGenericMotionEvent(event) || super.onGenericMotionEvent(event);
    }

    public void onHideIme() {
        requestHideSelf(InputMethodService.BACK_DISPOSITION_DEFAULT);
    }

    @Override
    public void onInitializeInterface() {
        mKeyboardController = new LeanbackKeyboardController(this, mInputListener);
        mEnterSpaceBeforeCommitting = false;
        mSuggestionsFactory = new LeanbackSuggestionsFactory(this, MAX_SUGGESTIONS);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // NOTE: hide keyboard on ESC key
        // https://github.com/yuliskov/SmartYouTubeTV/issues/142
        event = mapEscToBack(event);
        keyCode = mapEscToBack(keyCode);

        return isInputViewShown() && mKeyboardController.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // NOTE: hide keyboard on ESC key
        // https://github.com/yuliskov/SmartYouTubeTV/issues/142
        event = mapEscToBack(event);
        keyCode = mapEscToBack(keyCode);

        return isInputViewShown() && mKeyboardController.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    private KeyEvent mapEscToBack(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
            // pay attention, you must pass the same action
            event = new KeyEvent(event.getAction(), KeyEvent.KEYCODE_BACK);
        }
        return event;
    }

    private int mapEscToBack(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            keyCode = KeyEvent.KEYCODE_BACK;
        }
        return keyCode;
    }

    // FireTV fix
    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        return true;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            super.onStartCommand(intent, flags, startId);
            if (intent.getBooleanExtra("restart", false)) {
                Log.e("LeanbackImeService", "Service->onStartCommand: trying to restart service");
                LeanbackKeyboardController controller = mKeyboardController;
                if (controller != null) {
                    controller.updateAddonKeyboard();
                }
            }
        }

        return Service.START_STICKY;
    }

    @Override
    public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        mEnterSpaceBeforeCommitting = false;
        mSuggestionsFactory.onStartInput(info);
        mKeyboardController.onStartInput(info);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);

        // FireTV: fix accidental kbd pop-ups
        // more info: https://forum.xda-developers.com/fire-tv/general/guide-change-screen-keyboard-to-leankey-t3527675/page2
        //updateInputViewShown();
        //if (!mKeyboardController.showInputView()) {
        //    onHideIme();
        //    return;
        //}

        mKeyboardController.onStartInputView();
        sendBroadcast(new Intent(IME_OPEN));
        if (mKeyboardController.areSuggestionsEnabled()) {
            mSuggestionsFactory.createSuggestions();
            mKeyboardController.updateSuggestions(mSuggestionsFactory.getSuggestions());
            InputConnection connection = getCurrentInputConnection();
            if (connection != null) {
                String text = getEditorText(connection);
                connection.deleteSurroundingText(getCharLengthBeforeCursor(connection), getCharLengthAfterCursor(connection));
                connection.commitText(text, 1);
            }
        }

    }
}
