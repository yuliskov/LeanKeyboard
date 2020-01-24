package com.liskovsoft.leankeyboard.keyboard.leanback.ime;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
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
import com.liskovsoft.leankeyboard.utils.LeanKeySettings;

public class LeanbackImeService extends InputMethodService {
    private static final String TAG = LeanbackImeService.class.getSimpleName();
    private static final boolean DEBUG = false;
    public static final String IME_CLOSE = "com.google.android.athome.action.IME_CLOSE";
    public static final String IME_OPEN = "com.google.android.athome.action.IME_OPEN";
    public static final int MAX_SUGGESTIONS = 10;
    static final int MODE_FREE_MOVEMENT = 1;
    static final int MODE_TRACKPAD_NAVIGATION = 0;
    private static final int MSG_SUGGESTIONS_CLEAR = 123;
    private static final int SUGGESTIONS_CLEAR_DELAY = 1000;
    private boolean mEnterSpaceBeforeCommitting;
    private View mInputView;
    private LeanbackKeyboardController mKeyboardController;
    private boolean mShouldClearSuggestions = true;
    private LeanbackSuggestionsFactory mSuggestionsFactory;
    public static final String COMMAND_RESTART = "restart";
    private boolean mForceShowKbd;

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

    private InputListener mInputListener = this::handleTextEntry;

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

        initSettings();
    }

    private void initSettings() {
        mForceShowKbd = LeanKeySettings.instance(this).getForceShowKeyboard();

        updateInputViewShown();
    }

    private void clearSuggestionsDelayed() {
        if (!mSuggestionsFactory.shouldSuggestionsAmend()) {
            mHandler.removeMessages(MSG_SUGGESTIONS_CLEAR);
            mShouldClearSuggestions = true;
            mHandler.sendEmptyMessageDelayed(MSG_SUGGESTIONS_CLEAR, SUGGESTIONS_CLEAR_DELAY);
        }

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
                        connection.deleteSurroundingText(LeanbackUtils.getCharLengthBeforeCursor(connection), LeanbackUtils.getCharLengthAfterCursor(connection));
                    } else {
                        int location = LeanbackUtils.getAmpersandLocation(connection);
                        connection.setSelection(location, location);
                        connection.deleteSurroundingText(0, LeanbackUtils.getCharLengthAfterCursor(connection));
                    }

                    connection.commitText(text, 1);
                    mEnterSpaceBeforeCommitting = true;
                case InputListener.ENTRY_TYPE_ACTION:  // NOTE: user presses Go, Send, Search etc
                    boolean result = sendDefaultEditorAction(true);

                    if (result) {
                        hideWindow(); // NOTE: SmartYouTubeTV hide kbd on search page fix
                    } else {
                        LeanbackUtils.sendEnterKey(connection);
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
        return false; // don't change it (true shows edit dialog above kbd)
    }

    /**
     * At this point, decision whether to show kbd taking place<br/>
     * <a href="https://stackoverflow.com/questions/7449283/is-it-possible-to-have-both-physical-keyboard-and-soft-keyboard-active-at-the-sa">More info</a>
     * @return whether to show kbd
     */
    @Override
    public boolean onEvaluateInputViewShown() {
        return mForceShowKbd || super.onEvaluateInputViewShown();
    }

    // FireTV fix
    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        return mForceShowKbd || super.onShowInputRequested(flags, configChange);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        sendBroadcast(new Intent(IME_CLOSE));
        mSuggestionsFactory.clearSuggestions();
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

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            if (intent.getBooleanExtra(COMMAND_RESTART, false)) {
                Log.d(TAG, "Service->onStartCommand: trying to restart service");
                LeanbackKeyboardController controller = mKeyboardController;
                if (controller != null) {
                    controller.updateAddonKeyboard();
                }

                return Service.START_REDELIVER_INTENT;
            }
        }

        return super.onStartCommand(intent, flags, startId);
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

        //Log.d(TAG, "Restarting ime service...");
        //setInputView(onCreateInputView());

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
                String text = LeanbackUtils.getEditorText(connection);
                connection.deleteSurroundingText(LeanbackUtils.getCharLengthBeforeCursor(connection), LeanbackUtils.getCharLengthAfterCursor(connection));
                connection.commitText(text, 1);
            }
        }

    }
}
