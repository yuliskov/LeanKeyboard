package com.google.android.leanback.ime;

import android.graphics.PointF;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard.Key;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnHoverListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.google.android.leanback.ime.LeanbackKeyboardContainer.KeyFocus;
import com.google.android.pano.util.TouchNavSpaceTracker;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;

public class LeanbackKeyboardController implements LeanbackKeyboardContainer.VoiceListener,
                                                   LeanbackKeyboardContainer.DismissListener,
                                                   OnTouchListener, OnHoverListener, Runnable {
    public static final int CLICK_MOVEMENT_BLOCK_DURATION_MS = 500;
    private static final boolean DEBUG = false;
    private static final int KEY_CHANGE_HISTORY_SIZE = 10;
    private static final long KEY_CHANGE_REVERT_TIME_MS = 100L;
    private static final String TAG = "LbKbController";
    public static final String TAG_GO = "Go";
    private boolean clickConsumed;
    private long lastClickTime;
    private LeanbackKeyboardContainer mContainer;
    private InputMethodService mContext;
    private LeanbackKeyboardController.DoubleClickDetector mDoubleClickDetector;
    private LeanbackKeyboardContainer.KeyFocus mDownFocus;
    private Handler mHandler;
    private LeanbackKeyboardController.InputListener mInputListener;
    ArrayList<LeanbackKeyboardController.KeyChange> mKeyChangeHistory;
    private LeanbackKeyboardContainer.KeyFocus mKeyDownKeyFocus;
    private boolean mKeyDownReceived;
    private boolean mLongPressHandled;
    private int mMoveCount;
    private OnLayoutChangeListener mOnLayoutChangeListener;
    public float mResizeSquareDistance;
    private TouchNavSpaceTracker mSpaceTracker;
    private LeanbackKeyboardContainer.KeyFocus mTempFocus;
    private PointF mTempPoint;
    private LeanbackKeyboardController.TouchEventListener mTouchEventListener;
    private long mPrevTime;
    private boolean mShowInput;
    private int mLastEditorIdPhysicalKeyboardWasUsed;
    private boolean mHideKeyboardWhenPhysicalKeyboardUsed = true;

    public LeanbackKeyboardController(final InputMethodService context, final LeanbackKeyboardController.InputListener listener) {
        this(context, listener, new TouchNavSpaceTracker(), new LeanbackKeyboardContainer(context));
    }

    public LeanbackKeyboardController(final InputMethodService context, final LeanbackKeyboardController.InputListener listener, final TouchNavSpaceTracker tracker,
                               final LeanbackKeyboardContainer container) {
        mDoubleClickDetector = new LeanbackKeyboardController.DoubleClickDetector();
        mOnLayoutChangeListener = new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                left = right - left;
                top = bottom - top;
                if (left > 0 && top > 0) {
                    if (LeanbackKeyboardController.this.mSpaceTracker != null) {
                        LeanbackKeyboardController.this.mSpaceTracker.setPixelSize((float) left, (float) top);
                    }

                    if (left != oldRight - oldLeft || top != oldBottom - oldTop) {
                        LeanbackKeyboardController.this.initInputView();
                    }
                }

            }
        };
        mTouchEventListener = new LeanbackKeyboardController.TouchEventListener();
        mDownFocus = new LeanbackKeyboardContainer.KeyFocus();
        mTempFocus = new LeanbackKeyboardContainer.KeyFocus();
        mKeyChangeHistory = new ArrayList<>(11);
        mTempPoint = new PointF();
        mKeyDownReceived = false;
        mLongPressHandled = false;
        mContext = context;
        mResizeSquareDistance = context.getResources().getDimension(R.dimen.resize_move_distance);
        mResizeSquareDistance *= mResizeSquareDistance;
        mInputListener = listener;
        setSpaceTracker(tracker);
        setKeyboardContainer(container);
        mContainer.setVoiceListener(this);
        mContainer.setDismissListener(this);
    }

    private boolean applyLETVFixesDown(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                return true;
            default:
                return false;
        }
    }

    private boolean applyLETVFixesUp(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                this.mContainer.onLangKeyPress();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                this.fakeKeyIndex(0, 2);
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                this.fakeKeyCode(-5);
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                this.fakeKeyCode(32);
                break;
            default:
                return false;
        }

        return true;
    }

    private void beginLongClickCountdown() {
        this.clickConsumed = false;
        Handler handler = this.mHandler;
        if (handler == null) {
            handler = new Handler();
            this.mHandler = handler;
        }

        handler.removeCallbacks(this);
        handler.postDelayed(this, (long) 1000);
    }

    private void clearKeyIfNecessary() {
        ++this.mMoveCount;
        if (this.mMoveCount >= 3) {
            this.mMoveCount = 0;
            this.mKeyDownKeyFocus = null;
        }

    }

    private void commitKey() {
        this.commitKey(this.mContainer.getCurrFocus());
    }

    /**
     * NOTE: where all magic happens. Input from virtual kbd is processed here.
     * @param focus current key
     */
    private void commitKey(LeanbackKeyboardContainer.KeyFocus focus) {
        if (mContainer != null && focus != null) {
            switch (focus.type) {
                case KeyFocus.TYPE_VOICE:
                    mContainer.onVoiceClick();
                    return;
                case KeyFocus.TYPE_ACTION: // NOTE: user presses Go, Send, Search etc
                    mInputListener.onEntry(InputListener.ENTRY_TYPE_ACTION, 0, null);
                    // mContext.hideWindow(); // SmartYouTubeTV fix: force hide keyboard
                    return;
                case KeyFocus.TYPE_SUGGESTION:
                    mInputListener.onEntry(InputListener.ENTRY_TYPE_SUGGESTION, 0, mContainer.getSuggestionText(focus.index));
                    return;
                default:
                    Key key = mContainer.getKey(focus.type, focus.index);
                    if (key != null) {
                        handleCommitKeyboardKey(key.codes[0], key.label);
                    }
            }
        }

    }

    private void fakeClickDown() {
        mContainer.setTouchState(LeanbackKeyboardContainer.TOUCH_STATE_CLICK);
    }

    private void fakeClickUp() {
        LeanbackKeyboardContainer container = mContainer;
        commitKey(container.getCurrFocus());
        container.setTouchState(0);
    }

    private void fakeKeyCode(final int keyCode) {
        mContainer.getCurrFocus().code = keyCode;
        handleCommitKeyboardKey(keyCode, null);
    }

    /**
     * Fake key index
     * @param index key index
     * @param type {@link KeyFocus KeyFocus} constant
     */
    private void fakeKeyIndex(final int index, final int type) {
        LeanbackKeyboardContainer.KeyFocus focus = mContainer.getCurrFocus();
        focus.index = index;
        focus.type = type;
        commitKey(focus);
    }

    private void fakeLongClickDown() {
        LeanbackKeyboardContainer container = mContainer;
        container.onKeyLongPress();
        container.setTouchState(LeanbackKeyboardContainer.TOUCH_STATE_CLICK);
    }

    private void fakeLongClickUp() {
        this.mContainer.setTouchState(0);
    }

    private PointF getBestSnapPosition(final PointF currPoint, final long currTime) {
        if (mKeyChangeHistory.size() <= 1) {
            return currPoint;
        } else {
            int count = 0;

            PointF pos;
            while (true) {
                pos = currPoint;
                if (count >= mKeyChangeHistory.size() - 1) {
                    break;
                }

                LeanbackKeyboardController.KeyChange change = mKeyChangeHistory.get(count);
                if (currTime - mKeyChangeHistory.get(count + 1).time < 100L) {
                    pos = change.position;
                    mKeyChangeHistory.clear();
                    mKeyChangeHistory.add(new LeanbackKeyboardController.KeyChange(currTime, pos));
                    break;
                }

                ++count;
            }

            return pos;
        }
    }

    private PointF getCurrentKeyPosition() {
        if (mContainer != null) {
            LeanbackKeyboardContainer.KeyFocus focus = mContainer.getCurrFocus();
            return new PointF((float) focus.rect.centerX(), (float) focus.rect.centerY());
        } else {
            return null;
        }
    }

    private PointF getRelativePosition(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float x = event.getRawX();
        float y = event.getRawY();
        return new PointF(x - (float) location[0], y - (float) location[1]);
    }

    private int getSimplifiedKey(final int keyCode) {
        int defaultCode = KeyEvent.KEYCODE_DPAD_CENTER;
        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) {
            final byte enter = KeyEvent.KEYCODE_ENTER;
            defaultCode = enter;
            if (keyCode != KeyEvent.KEYCODE_ENTER) {
                defaultCode = enter;
                if (keyCode != KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    defaultCode = keyCode;
                    if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                        defaultCode = enter;
                    }
                }
            }
        }
        
        if (defaultCode == KeyEvent.KEYCODE_BUTTON_B) {
            defaultCode = KeyEvent.KEYCODE_BACK;
        }

        return defaultCode;
    }

    /**
     * Handle keyboard key
     * @param keyCode key code e.g {@link LeanbackKeyboardView#KEYCODE_SHIFT LeanbackKeyboardView.KEYCODE_SHIFT}
     * @param text typed content
     */
    private void handleCommitKeyboardKey(int keyCode, CharSequence text) {
        switch (keyCode) {
            case LeanbackKeyboardView.KEYCODE_DISMISS_MINI_KEYBOARD:
                mContainer.dismissMiniKeyboard();
                return;
            case LeanbackKeyboardView.KEYCODE_VOICE:
                mContainer.startVoiceRecording();
                return;
            case LeanbackKeyboardView.KEYCODE_CAPS_LOCK:
                mContainer.onShiftDoubleClick(mContainer.isCapsLockOn());
                return;
            case LeanbackKeyboardView.KEYCODE_DELETE:
                mInputListener.onEntry(InputListener.ENTRY_TYPE_BACKSPACE, LeanbackKeyboardView.SHIFT_OFF, null);
                return;
            case LeanbackKeyboardView.KEYCODE_RIGHT:
                mInputListener.onEntry(InputListener.ENTRY_TYPE_RIGHT, LeanbackKeyboardView.SHIFT_OFF, null);
                return;
            case LeanbackKeyboardView.KEYCODE_LEFT:
                mInputListener.onEntry(InputListener.ENTRY_TYPE_LEFT, LeanbackKeyboardView.SHIFT_OFF, null);
                return;
            case LeanbackKeyboardView.KEYCODE_SYM_TOGGLE:
                if (Log.isLoggable("LbKbController", Log.DEBUG)) {
                    Log.d("LbKbController", "mode change");
                }

                mContainer.onModeChangeClick();
                return;
            case LeanbackKeyboardView.KEYCODE_SHIFT:
                if (Log.isLoggable("LbKbController", Log.DEBUG)) {
                    Log.d("LbKbController", "shift");
                }

                mContainer.onShiftClick();
                return;
            case LeanbackKeyboardView.ASCII_SPACE:
                mInputListener.onEntry(InputListener.ENTRY_TYPE_STRING, keyCode, " ");
                mContainer.onSpaceEntry();
                return;
            case LeanbackKeyboardView.ASCII_PERIOD:
                mInputListener.onEntry(InputListener.ENTRY_TYPE_STRING, keyCode, text);
                mContainer.onPeriodEntry();
                return;
            case LeanbackKeyboardView.KEYCODE_LANG_TOGGLE:
                if (Log.isLoggable("LbKbController", Log.DEBUG)) {
                    Log.d("LbKbController", "language change");
                }

                mContainer.onLangKeyClick();
                return;
            default:
                mInputListener.onEntry(InputListener.ENTRY_TYPE_STRING, keyCode, text);
                mContainer.onTextEntry();
                if (mContainer.isMiniKeyboardOnScreen()) {
                    mContainer.dismissMiniKeyboard();
                }

        }
    }

    private boolean handleKeyDownEvent(int keyCode, int eventRepeatCount) {
        keyCode = getSimplifiedKey(keyCode);
        boolean handled;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mContainer.cancelVoiceRecording();
            handled = false;
        } else if (mContainer.isVoiceVisible()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                mContainer.cancelVoiceRecording();
            }

            handled = true;
        } else {
            handled = true;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    handled = onDirectionalMove(LeanbackKeyboardContainer.DIRECTION_UP);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    handled = onDirectionalMove(LeanbackKeyboardContainer.DIRECTION_DOWN);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    handled = onDirectionalMove(LeanbackKeyboardContainer.DIRECTION_LEFT);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    handled = onDirectionalMove(LeanbackKeyboardContainer.DIRECTION_RIGHT);
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (eventRepeatCount == 0) {
                        mMoveCount = 0;
                        mKeyDownKeyFocus = new LeanbackKeyboardContainer.KeyFocus();
                        mKeyDownKeyFocus.set(mContainer.getCurrFocus());
                    } else if (eventRepeatCount == 1 && handleKeyLongPress(keyCode)) { // space long press handler and others
                        mKeyDownKeyFocus = null;
                    }

                    handled = true;
                    if (isKeyHandledOnKeyDown(mContainer.getCurrKeyCode())) {
                        commitKey();
                        handled = true;
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    handleCommitKeyboardKey(LeanbackKeyboardView.KEYCODE_DELETE, null);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    handleCommitKeyboardKey(LeanbackKeyboardView.ASCII_SPACE, null);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    handleCommitKeyboardKey(LeanbackKeyboardView.KEYCODE_LEFT, null);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    handleCommitKeyboardKey(LeanbackKeyboardView.KEYCODE_RIGHT, null);
                    handled = true;
                case KeyEvent.KEYCODE_BUTTON_THUMBL:
                case KeyEvent.KEYCODE_BUTTON_THUMBR:
                    break;
                default:
                    handled = false;
            }
        }

        if (!handled) {
            handled = applyLETVFixesDown(keyCode);
        }

        return handled;
    }

    private boolean handleKeyLongPress(int keyCode) {
        boolean isHandled;
        if (isEnterKey(keyCode) && mContainer.onKeyLongPress()) {
            isHandled = true;
        } else {
            isHandled = false;
        }

        mLongPressHandled = isHandled;
        if (mContainer.isMiniKeyboardOnScreen()) {
            Log.d("LbKbController", "mini keyboard shown after long press");
        }

        return mLongPressHandled;
    }

    private boolean handleKeyUpEvent(int keyCode, long currTime) {
        keyCode = getSimplifiedKey(keyCode);
        boolean handled;

        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            handled = false;
        } else if (mContainer.isVoiceVisible()) {
            handled = true;
        } else {
            handled = true;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    clearKeyIfNecessary();
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (mContainer.getCurrKeyCode() == LeanbackKeyboardView.KEYCODE_SHIFT) {
                        mDoubleClickDetector.addEvent(currTime);
                        handled = true;
                    } else {
                        handled = true;
                        if (!isKeyHandledOnKeyDown(mContainer.getCurrKeyCode())) {
                            commitKey(mKeyDownKeyFocus);
                            handled = true;
                        }
                    }
                case KeyEvent.KEYCODE_BUTTON_X:
                case KeyEvent.KEYCODE_BUTTON_Y:
                case KeyEvent.KEYCODE_BUTTON_L1:
                case KeyEvent.KEYCODE_BUTTON_R1:
                    break;
                case KeyEvent.KEYCODE_BUTTON_THUMBL:
                    handleCommitKeyboardKey(LeanbackKeyboardView.KEYCODE_SYM_TOGGLE, null);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_THUMBR:
                    handleCommitKeyboardKey(LeanbackKeyboardView.KEYCODE_CAPS_LOCK, null);
                    handled = true;
                    break;
                default:
                    handled = false;
            }
        }

        if (!handled) {
            handled = applyLETVFixesUp(keyCode);
        }

        return handled;
    }

    private void initInputView() {
        mContainer.onInitInputView();
        updatePositionToCurrentFocus();
    }

    /**
     * Simple throttle routine.
     * @param callInterval interval
     * @return is allowed
     */
    private boolean isCallAllowedOrigin(int callInterval) {
        long currTimeMS = System.currentTimeMillis();
        long timeDelta = currTimeMS - mPrevTime;
        if (mPrevTime != 0 && timeDelta <= (callInterval * 3)) {
            if (timeDelta > callInterval) {
                mPrevTime = 0;
                return true;
            }
        } else {
            mPrevTime = currTimeMS;
        }

        return false;
    }

    /**
     * Simple throttle routine. Simplified comparing to previous. Not tested yet!!!!
     * @param interval interval
     * @return is allowed
     */
    private boolean isCallAllowed2(int interval) {
        long currTimeMS = System.currentTimeMillis();
        long timeDelta = currTimeMS - mPrevTime;
        if (mPrevTime == 0) {
            mPrevTime = currTimeMS;
            return true;
        } else if (timeDelta > interval) {
            mPrevTime = 0;
        }

        return false;
    }

    private boolean isDoubleClick() {
        long currTimeMS = System.currentTimeMillis();
        long lastTime = this.lastClickTime;
        if (this.lastClickTime != 0L && currTimeMS - lastTime <= (long) 300) {
            return true;
        } else {
            this.lastClickTime = currTimeMS;
            return false;
        }
    }

    private boolean isEnterKey(int keyCode) {
        keyCode = this.getSimplifiedKey(keyCode);
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER;
    }

    /**
     * Whether key down is handled
     * @param currKeyCode key code e.g. {@link LeanbackKeyboardView#KEYCODE_DELETE LeanbackKeyboardView.KEYCODE_DELETE}
     * @return key down is handled
     */
    private boolean isKeyHandledOnKeyDown(int currKeyCode) {
        return currKeyCode == LeanbackKeyboardView.KEYCODE_DELETE || currKeyCode == LeanbackKeyboardView.KEYCODE_LEFT || currKeyCode == LeanbackKeyboardView.KEYCODE_RIGHT;
    }

    private void moveSelectorToPoint(float x, float y) {
        LeanbackKeyboardContainer container = mContainer;
        LeanbackKeyboardContainer.KeyFocus focus = mTempFocus;
        container.getBestFocus(x, y, focus);
        mContainer.setFocus(mTempFocus, false);
    }

    private boolean onDirectionalMove(int dir) {
        if (mContainer.getNextFocusInDirection(dir, mDownFocus, mTempFocus)) {
            mContainer.setFocus(mTempFocus);
            mDownFocus.set(mTempFocus);
            clearKeyIfNecessary();
        }

        return true;
    }

    private void performBestSnap(long time) {
        LeanbackKeyboardContainer.KeyFocus focus = this.mContainer.getCurrFocus();
        this.mTempPoint.x = (float) focus.rect.centerX();
        this.mTempPoint.y = (float) focus.rect.centerY();
        PointF pos = this.getBestSnapPosition(this.mTempPoint, time);
        this.mContainer.getBestFocus(pos.x, pos.y, this.mTempFocus);
        this.mContainer.setFocus(this.mTempFocus);
        this.updatePositionToCurrentFocus();
    }

    /**
     * Set key state
     * @param keyIndex key index
     * @param keyState constant e.g. {@link LeanbackKeyboardContainer#TOUCH_STATE_CLICK LeanbackKeyboardContainer.TOUCH_STATE_CLICK}
     */
    private void setKeyState(int keyIndex, boolean keyState) {
        LeanbackKeyboardContainer container = this.mContainer;
        LeanbackKeyboardContainer.KeyFocus focus = container.getCurrFocus();
        focus.index = keyIndex;
        focus.type = KeyFocus.TYPE_MAIN;
        byte state;
        if (keyState) {
            state = LeanbackKeyboardContainer.TOUCH_STATE_CLICK;
        } else {
            state = LeanbackKeyboardContainer.TOUCH_STATE_NO_TOUCH;
        }

        container.setTouchState(state);
    }

    private void updatePositionToCurrentFocus() {
        PointF pos = this.getCurrentKeyPosition();
        if (pos != null && this.mSpaceTracker != null) {
            this.mSpaceTracker.setPixelPosition(pos.x, pos.y);
        }

    }

    public boolean areSuggestionsEnabled() {
        return mContainer != null ? mContainer.areSuggestionsEnabled() : false;
    }

    public boolean enableAutoEnterSpace() {
        return mContainer != null ? mContainer.enableAutoEnterSpace() : false;
    }

    public View getView() {
        if (mContainer != null) {
            RelativeLayout view = mContainer.getView();
            view.setClickable(true);
            view.setOnTouchListener(this);
            view.setOnHoverListener(this);
            Button button = mContainer.getGoButton();
            button.setOnTouchListener(this);
            button.setOnHoverListener(this);
            button.setTag(TAG_GO);
            return view;
        } else {
            return null;
        }
    }

    public void onDismiss(boolean fromVoice) {
        if (fromVoice) {
            mInputListener.onEntry(InputListener.ENTRY_TYPE_VOICE_DISMISS, LeanbackKeyboardView.SHIFT_OFF, null);
        } else {
            mInputListener.onEntry(InputListener.ENTRY_TYPE_DISMISS, LeanbackKeyboardView.SHIFT_OFF, null);
        }
    }
    
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mSpaceTracker != null && mContext != null && mContext.isInputViewShown() && mSpaceTracker.onGenericMotionEvent(event);
    }

    /**
     * Control keyboard from the mouse. Movement catching
     * @param view active view
     * @param event event object
     * @return is hover handled
     */
    @Override
    public boolean onHover(View view, MotionEvent event) {
        boolean handled = false;
        if (event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
            PointF pos = getRelativePosition(mContainer.getView(), event);
            moveSelectorToPoint(pos.x, pos.y);
            handled = true;
        }

        return handled;
    }

    /**
     * Try to handle key down event
     * @param keyCode key code e.g. {@link KeyEvent#KEYCODE_ENTER KeyEvent.KEYCODE_ENTER}
     * @param event {@link KeyEvent KeyEvent}
     * @return is event handled
     */
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        //greater than zero means it is a physical keyboard.
        //we also want to hide the view if it's a glyph (for example, not physical volume-up key)
        //if (event.getDeviceId() > 0 && event.isPrintingKey()) onPhysicalKeyboardKeyPressed();
        if (event.isPrintingKey()) onPhysicalKeyboardKeyPressed();

        mDownFocus.set(mContainer.getCurrFocus());
        if (mSpaceTracker != null && mSpaceTracker.onKeyDown(keyCode, event)) {
            return true;
        } else {
            if (isEnterKey(keyCode)) {
                mKeyDownReceived = true;
                if (event.getRepeatCount() == 0) {
                    mContainer.setTouchState(LeanbackKeyboardContainer.TOUCH_STATE_CLICK);
                }
            }

            return handleKeyDownEvent(keyCode, event.getRepeatCount());
        }
    }

    private void onPhysicalKeyboardKeyPressed() {
        EditorInfo editorInfo = mContext.getCurrentInputEditorInfo();
        mLastEditorIdPhysicalKeyboardWasUsed = editorInfo == null ? 0 : editorInfo.fieldId;
        if (mHideKeyboardWhenPhysicalKeyboardUsed) {
            mContext.hideWindow();
        }

        // For all other keys, if we want to do transformations on
        // text being entered with a hard keyboard, we need to process
        // it and do the appropriate action.
        // using physical keyboard is more annoying with candidate view in
        // the way
        // so we disable it.

        // stopping any soft-keyboard prediction
        //abortCorrectionAndResetPredictionState(false);
    }

    public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
        if (mSpaceTracker != null && mSpaceTracker.onKeyUp(keyCode, keyEvent)) {
            return true;
        } else {
            if (isEnterKey(keyCode)) {
                if (!mKeyDownReceived || mLongPressHandled) {
                    mLongPressHandled = false;
                    return true;
                }

                mKeyDownReceived = false;
                if (mContainer.getTouchState() == 3) {
                    mContainer.setTouchState(1);
                }
            }

            return handleKeyUpEvent(keyCode, keyEvent.getEventTime());
        }
    }

    public void onStartInput(EditorInfo info) {
        if (mContainer != null) {
            mContainer.onStartInput(info);
            initInputView();
        }

        //// prevent accidental kbd pop-up on FireTV devices
        //// more info: https://forum.xda-developers.com/fire-tv/general/guide-change-screen-keyboard-to-leankey-t3527675/page2
        //int maskAction = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        //mShowInput = maskAction != 0;

        mShowInput = info.inputType != InputType.TYPE_NULL;
    }

    public boolean showInputView() {
        return mShowInput;
    }

    private void onHideIme() {
        mContext.requestHideSelf(InputMethodService.BACK_DISPOSITION_DEFAULT);
    }

    public void onStartInputView() {
        mKeyDownReceived = false;

        if (mContainer != null) {
            mContainer.onStartInputView();
        }

        mDoubleClickDetector.reset();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        Object tag = view.getTag();
        boolean isEnterKey = TAG_GO.equals(tag);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isEnterKey) {
                    break;
                }

                moveSelectorToPoint(event.getX(), event.getY());
                fakeClickDown();
                beginLongClickCountdown();
                break;
            case MotionEvent.ACTION_UP:
                if (isEnterKey) {
                    fakeKeyIndex(0, KeyFocus.TYPE_ACTION);
                    break;
                }

                if (!clickConsumed) {
                    clickConsumed = true;
                    if (isDoubleClick()) {
                        mContainer.onKeyLongPress();
                        break;
                    }

                    fakeClickUp();
                }

                fakeLongClickUp();
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public void onVoiceResult(String result) {
        mInputListener.onEntry(InputListener.ENTRY_TYPE_VOICE, 0, result);
    }

    @Override
    public void run() {
        if (!clickConsumed) {
            clickConsumed = true;
            fakeLongClickDown();
        }
    }

    public void setKeyboardContainer(LeanbackKeyboardContainer container) {
        mContainer = container;
        container.getView().addOnLayoutChangeListener(mOnLayoutChangeListener);
    }

    public void setSpaceTracker(TouchNavSpaceTracker tracker) {
        mSpaceTracker = tracker;
        tracker.setLPFEnabled(true);
        tracker.setKeyEventListener(mTouchEventListener);
    }

    public void updateAddonKeyboard() {
        mContainer.updateAddonKeyboard();
    }

    public void updateSuggestions(ArrayList<String> suggestions) {
        if (mContainer != null) {
            mContainer.updateSuggestions(suggestions);
        }

    }

    private class DoubleClickDetector {
        final long DOUBLE_CLICK_TIMEOUT_MS;
        boolean mFirstClickShiftLocked;
        long mFirstClickTime;

        private DoubleClickDetector() {
            DOUBLE_CLICK_TIMEOUT_MS = 200L;
            mFirstClickTime = 0L;
        }

        public void addEvent(long currTime) {
            if (currTime - mFirstClickTime > DOUBLE_CLICK_TIMEOUT_MS) {
                mFirstClickTime = currTime;
                mFirstClickShiftLocked = LeanbackKeyboardController.this.mContainer.isCapsLockOn();
                LeanbackKeyboardController.this.commitKey();
            } else {
                LeanbackKeyboardController.this.mContainer.onShiftDoubleClick(mFirstClickShiftLocked);
                reset();
            }
        }

        public void reset() {
            mFirstClickTime = 0L;
        }
    }

    public interface InputListener {
        int ENTRY_TYPE_ACTION = 5;
        int ENTRY_TYPE_BACKSPACE = 1;
        int ENTRY_TYPE_DISMISS = 7;
        int ENTRY_TYPE_LEFT = 3;
        int ENTRY_TYPE_RIGHT = 4;
        int ENTRY_TYPE_STRING = 0;
        int ENTRY_TYPE_SUGGESTION = 2;
        int ENTRY_TYPE_VOICE = 6;
        int ENTRY_TYPE_VOICE_DISMISS = 8;

        /**
         * User has typed something
         * @param type type e.g. {@link InputListener#ENTRY_TYPE_ACTION InputListener.ENTRY_TYPE_ACTION}
         * @param keyCode key code e.g. {@link LeanbackKeyboardView#SHIFT_ON LeanbackKeyboardView.SHIFT_ON}
         * @param text text
         */
        void onEntry(int type, int keyCode, CharSequence text);
    }

    private static final class KeyChange {
        public PointF position;
        public long time;

        public KeyChange(long time, PointF position) {
            this.time = time;
            this.position = position;
        }
    }

    private class TouchEventListener implements TouchNavSpaceTracker.KeyEventListener {
        private TouchEventListener() {
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (LeanbackKeyboardController.this.isEnterKey(keyCode)) {
                LeanbackKeyboardController.this.mKeyDownReceived = true;
                if (event.getRepeatCount() == 0) {
                    LeanbackKeyboardController.this.mContainer.setTouchState(3);
                    LeanbackKeyboardController.this.mSpaceTracker.blockMovementUntil(event.getEventTime() + CLICK_MOVEMENT_BLOCK_DURATION_MS);
                    LeanbackKeyboardController.this.performBestSnap(event.getEventTime());
                }
            }

            return LeanbackKeyboardController.this.handleKeyDownEvent(keyCode, event.getRepeatCount());
        }

        public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            return LeanbackKeyboardController.this.handleKeyLongPress(keyCode);
        }

        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (LeanbackKeyboardController.this.isEnterKey(keyCode)) {
                if (!LeanbackKeyboardController.this.mKeyDownReceived || LeanbackKeyboardController.this.mLongPressHandled) {
                    LeanbackKeyboardController.this.mLongPressHandled = false;
                    return true;
                }

                LeanbackKeyboardController.this.mKeyDownReceived = false;
                if (LeanbackKeyboardController.this.mContainer.getTouchState() == 3) {
                    LeanbackKeyboardController.this.mContainer.setTouchState(1);
                    LeanbackKeyboardController.this.mSpaceTracker.unblockMovement();
                }
            }

            return LeanbackKeyboardController.this.handleKeyUpEvent(keyCode, event.getEventTime());
        }
    }
}
