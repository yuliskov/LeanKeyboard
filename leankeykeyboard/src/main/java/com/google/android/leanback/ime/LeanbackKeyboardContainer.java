package com.google.android.leanback.ime;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.google.android.leanback.ime.voice.RecognizerView;
import com.google.android.leanback.ime.voice.SpeechLevelSource;
import com.google.leanback.ime.LeanbackImeService;
import com.liskovsoft.keyboardaddons.KeyboardManager;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.Locale;

public class LeanbackKeyboardContainer {
    private static final boolean DEBUG = false;
    public static final double DIRECTION_STEP_MULTIPLIER = 1.25D;
    private static final String IME_PRIVATE_OPTIONS_ESCAPE_NORTH = "EscapeNorth=1";
    private static final String IME_PRIVATE_OPTIONS_VOICE_DISMISS = "VoiceDismiss=1";
    private static final long MOVEMENT_ANIMATION_DURATION = 150L;
    private static final int MSG_START_INPUT_VIEW = 0;
    protected static final float PHYSICAL_HEIGHT_CM = 5.0F;
    protected static final float PHYSICAL_WIDTH_CM = 12.0F;
    private static final String TAG = "LbKbContainer";
    public static final double TOUCH_MOVE_MIN_DISTANCE = 0.1D;
    public static final int TOUCH_STATE_CLICK = 3;
    public static final int TOUCH_STATE_NO_TOUCH = 0;
    public static final int TOUCH_STATE_TOUCH_MOVE = 2;
    public static final int TOUCH_STATE_TOUCH_SNAP = 1;
    private static final boolean VOICE_SUPPORTED = true;
    public static final Interpolator sMovementInterpolator = new DecelerateInterpolator(1.5F);
    private Keyboard mAbcKeyboard;
    private Keyboard mAbcKeyboardRU;
    private Button mActionButtonView;
    private final float mAlphaIn;
    private final float mAlphaOut;
    private boolean mAutoEnterSpaceEnabled;
    private boolean mCapCharacters;
    private boolean mCapSentences;
    private boolean mCapWords;
    private final int mClickAnimDur;
    private LeanbackImeService mContext;
    private LeanbackKeyboardContainer.KeyFocus mCurrKeyInfo = new LeanbackKeyboardContainer.KeyFocus();
    private LeanbackKeyboardContainer.DismissListener mDismissListener;
    private LeanbackKeyboardContainer.KeyFocus mDownKeyInfo = new LeanbackKeyboardContainer.KeyFocus();
    private CharSequence mEnterKeyText;
    private int mEnterKeyTextResId;
    private boolean mEscapeNorthEnabled;
    private Keyboard mInitialMainKeyboard;
    private KeyboardManager mKeyboardManager;
    private View mKeyboardsContainer;
    private LeanbackKeyboardView mMainKeyboardView;
    private int mMiniKbKeyIndex;
    private Keyboard mNumKeyboard;
    private float mOverestimate;
    private PointF mPhysicalSelectPos = new PointF(2.0F, 0.5F);
    private PointF mPhysicalTouchPos = new PointF(2.0F, 0.5F);
    private LeanbackKeyboardView mPrevView;
    private Intent mRecognizerIntent;
    private Rect mRect = new Rect();
    private RelativeLayout mRootView;
    private View mSelector;
    private LeanbackKeyboardContainer.ScaleAnimation mSelectorAnimation;
    private ValueAnimator mSelectorAnimator;
    private SpeechLevelSource mSpeechLevelSource;
    private SpeechRecognizer mSpeechRecognizer;
    private LinearLayout mSuggestions;
    private View mSuggestionsBg;
    private HorizontalScrollView mSuggestionsContainer;
    private boolean mSuggestionsEnabled;
    private Keyboard mSymKeyboard;
    private LeanbackKeyboardContainer.KeyFocus mTempKeyInfo = new LeanbackKeyboardContainer.KeyFocus();
    private PointF mTempPoint = new PointF();
    private boolean mTouchDown = false;
    private int mTouchState = TOUCH_STATE_NO_TOUCH;
    private final int mVoiceAnimDur;
    private final LeanbackKeyboardContainer.VoiceIntroAnimator mVoiceAnimator;
    private RecognizerView mVoiceButtonView;
    private boolean mVoiceEnabled;
    private AnimatorListener mVoiceEnterListener = new AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
            LeanbackKeyboardContainer.this.mSelector.setVisibility(View.INVISIBLE);
            LeanbackKeyboardContainer.this.startRecognition(LeanbackKeyboardContainer.this.mContext);
        }
    };
    private AnimatorListener mVoiceExitListener = new AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            LeanbackKeyboardContainer.this.mSelector.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
            LeanbackKeyboardContainer.this.mVoiceButtonView.showNotListening();
            LeanbackKeyboardContainer.this.mSpeechRecognizer.cancel();
            LeanbackKeyboardContainer.this.mSpeechRecognizer.setRecognitionListener((RecognitionListener) null);
            LeanbackKeyboardContainer.this.mVoiceOn = false;
        }
    };
    private boolean mVoiceKeyDismissesEnabled;
    private LeanbackKeyboardContainer.VoiceListener mVoiceListener;
    private boolean mVoiceOn;
    private Float mX;
    private Float mY;

    public LeanbackKeyboardContainer(Context context) {
        mContext = (LeanbackImeService) context;
        final Resources res = mContext.getResources();
        mVoiceAnimDur = res.getInteger(R.integer.voice_anim_duration);
        mAlphaIn = res.getFraction(R.fraction.alpha_in, 1, 1);
        mAlphaOut = res.getFraction(R.fraction.alpha_out, 1, 1);
        mVoiceAnimator = new LeanbackKeyboardContainer.VoiceIntroAnimator(mVoiceEnterListener, mVoiceExitListener);
        initKeyboards();
        mRootView = (RelativeLayout) mContext.getLayoutInflater().inflate(R.layout.root_leanback, null);
        mKeyboardsContainer = mRootView.findViewById(R.id.keyboard);
        mSuggestionsBg = mRootView.findViewById(R.id.candidate_background);
        mSuggestionsContainer = (HorizontalScrollView) mRootView.findViewById(R.id.suggestions_container);
        mSuggestions = (LinearLayout) mSuggestionsContainer.findViewById(R.id.suggestions);
        mMainKeyboardView = (LeanbackKeyboardView) mRootView.findViewById(R.id.main_keyboard);
        mVoiceButtonView = (RecognizerView) mRootView.findViewById(R.id.voice);
        mActionButtonView = (Button) mRootView.findViewById(R.id.enter);
        mSelector = mRootView.findViewById(R.id.selector);
        mSelectorAnimation = new LeanbackKeyboardContainer.ScaleAnimation((FrameLayout) mSelector);
        mOverestimate = mContext.getResources().getFraction(R.fraction.focused_scale, 1, 1);
        final float scale = context.getResources().getFraction(R.fraction.clicked_scale, 1, 1);
        mClickAnimDur = context.getResources().getInteger(R.integer.clicked_anim_duration);
        mSelectorAnimator = ValueAnimator.ofFloat(new float[]{1.0F, scale});
        mSelectorAnimator.setDuration((long) mClickAnimDur);
        mSelectorAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                final float value = (Float) animation.getAnimatedValue();
                LeanbackKeyboardContainer.this.mSelector.setScaleX(value);
                LeanbackKeyboardContainer.this.mSelector.setScaleY(value);
            }
        });
        mSpeechLevelSource = new SpeechLevelSource();
        mVoiceButtonView.setSpeechLevelSource(mSpeechLevelSource);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        mVoiceButtonView.setCallback(new RecognizerView.Callback() {
            @Override
            public void onCancelRecordingClicked() {
                LeanbackKeyboardContainer.this.cancelVoiceRecording();
            }

            @Override
            public void onStartRecordingClicked() {
                LeanbackKeyboardContainer.this.startVoiceRecording();
            }

            @Override
            public void onStopRecordingClicked() {
                LeanbackKeyboardContainer.this.cancelVoiceRecording();
            }
        });
    }

    private void configureFocus(LeanbackKeyboardContainer.KeyFocus focus, Rect rect, int index, int type) {
        focus.type = type;
        focus.index = index;
        focus.rect.set(rect);
    }

    /**
     * Initialize {@link KeyFocus} with values
     * @param focus {@link KeyFocus} to configure
     * @param rect {@link Rect}
     * @param index key index
     * @param key {@link Key}
     * @param type {@link KeyFocus#type} constant
     */
    private void configureFocus(LeanbackKeyboardContainer.KeyFocus focus, Rect rect, int index, Key key, int type) {
        focus.type = type;
        if (key != null) {
            if (key.codes != null) {
                focus.code = key.codes[0];
            } else {
                focus.code = 0;
            }

            focus.index = index;
            focus.label = key.label;
            focus.rect.left = key.x + rect.left;
            focus.rect.top = key.y + rect.top;
            focus.rect.right = focus.rect.left + key.width;
            focus.rect.bottom = focus.rect.top + key.height;
        }
    }

    private void escapeNorth() {
        this.mDismissListener.onDismiss(false);
    }

    private PointF getAlignmentPosition(final float posXCm, final float posYCm, final PointF result) {
        final float width = (float) (this.mRootView.getWidth() - this.mRootView.getPaddingRight() - this.mRootView.getPaddingLeft());
        final float height = (float) (this.mRootView.getHeight() - this.mRootView.getPaddingTop() - this.mRootView.getPaddingBottom());
        final float size = this.mContext.getResources().getDimension(R.dimen.selector_size);
        result.x = posXCm / PHYSICAL_WIDTH_CM * (width - size) + (float) this.mRootView.getPaddingLeft();
        result.y = posYCm / PHYSICAL_HEIGHT_CM * (height - size) + (float) this.mRootView.getPaddingTop();
        return result;
    }

    private void getPhysicalPosition(final float x, final float y, final PointF result) {
        float width = (float) (this.mSelector.getWidth() / 2);
        float height = (float) (this.mSelector.getHeight() / 2);
        float posXCm = (float) (this.mRootView.getWidth() - this.mRootView.getPaddingRight() - this.mRootView.getPaddingLeft());
        float posYCm = (float) (this.mRootView.getHeight() - this.mRootView.getPaddingTop() - this.mRootView.getPaddingBottom());
        float size = this.mContext.getResources().getDimension(R.dimen.selector_size);
        result.x = (x - width - (float) this.mRootView.getPaddingLeft()) * PHYSICAL_WIDTH_CM / (posXCm - size);
        result.y = (y - height - (float) this.mRootView.getPaddingTop()) * PHYSICAL_HEIGHT_CM / (posYCm - size);
    }

    private PointF getTouchSnapPosition() {
        PointF var1 = new PointF();
        this.getPhysicalPosition((float) this.mCurrKeyInfo.rect.centerX(), (float) this.mCurrKeyInfo.rect.centerY(), var1);
        return var1;
    }

    private void initKeyboards() {
        this.mAbcKeyboard = new Keyboard(this.mContext, R.xml.qwerty_us);
        this.mSymKeyboard = new Keyboard(this.mContext, R.xml.sym_us);
        this.updateAddonKeyboard();
        this.mNumKeyboard = new Keyboard(this.mContext, R.xml.number);
    }

    private boolean isMatch(Locale var1, Locale[] var2) {
        int var4 = var2.length;

        for (int var3 = 0; var3 < var4; ++var3) {
            Locale var5 = var2[var3];
            if ((TextUtils.isEmpty(var5.getLanguage()) || TextUtils.equals(var1.getLanguage(), var5.getLanguage())) && (TextUtils.isEmpty
                    (var5.getCountry()) || TextUtils.equals(var1.getCountry(), var5.getCountry()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Move focus to specified key
     * @param index key index
     * @param type {@link KeyFocus#type} constant
     */
    private void moveFocusToIndex(int index, int type) {
        Key key = this.mMainKeyboardView.getKey(index);
        configureFocus(mTempKeyInfo, mRect, index, key, type);
        setTouchState(TOUCH_STATE_NO_TOUCH);
        setKbFocus(mTempKeyInfo, true, true);
    }

    private void offsetRect(Rect rect, View view) {
        rect.left = 0;
        rect.top = 0;
        rect.right = view.getWidth();
        rect.bottom = view.getHeight();
        mRootView.offsetDescendantRectToMyCoords(view, rect);
    }

    private void onToggleCapsLock() {
        onShiftDoubleClick(isCapsLockOn());
    }

    private void setImeOptions(Resources res, EditorInfo info) {
        if (mInitialMainKeyboard == null) {
            mInitialMainKeyboard = mAbcKeyboard;
        }

        mSuggestionsEnabled = false;
        mAutoEnterSpaceEnabled = false;
        mVoiceEnabled = false;
        mEscapeNorthEnabled = false;
        mVoiceKeyDismissesEnabled = false;

        switch (LeanbackUtils.getInputTypeClass(info)) {
            case InputType.TYPE_CLASS_TEXT:
                switch (LeanbackUtils.getInputTypeVariation(info)) {
                    case InputType.TYPE_DATETIME_VARIATION_DATE:
                    case InputType.TYPE_DATETIME_VARIATION_TIME:
                    case InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:
                    case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
                        mSuggestionsEnabled = false;
                        mAutoEnterSpaceEnabled = false;
                        mVoiceEnabled = false;
                        mInitialMainKeyboard = mAbcKeyboard;
                        break;
                    case InputType.TYPE_TEXT_VARIATION_PERSON_NAME:
                    case InputType.TYPE_TEXT_VARIATION_PASSWORD:
                    case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
                    case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
                        mSuggestionsEnabled = false;
                        mVoiceEnabled = false;
                        mInitialMainKeyboard = mAbcKeyboard;
                }
                break;
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_PHONE:
            case InputType.TYPE_CLASS_DATETIME:
                mSuggestionsEnabled = false;
                mVoiceEnabled = false;
                mInitialMainKeyboard = this.mAbcKeyboard;
        }

        if (mSuggestionsEnabled) {
            if ((info.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0) {
                ;
            }

            mSuggestionsEnabled = false;
        }

        if (mAutoEnterSpaceEnabled) {
            if (mSuggestionsEnabled && mAutoEnterSpaceEnabled) {
                ;
            }

            mAutoEnterSpaceEnabled = false;
        }

        if ((info.inputType & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) {
            ;
        }

        mCapSentences = false;
        if ((info.inputType & InputType.TYPE_TEXT_FLAG_CAP_WORDS) == 0 &&
                LeanbackUtils.getInputTypeVariation(info) == InputType.TYPE_TEXT_VARIATION_PERSON_NAME) {
            ;
        }

        mCapWords = false;
        if ((info.inputType & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
            ;
        }

        mCapCharacters = false;
        if (info.privateImeOptions != null) {
            if (info.privateImeOptions.contains(IME_PRIVATE_OPTIONS_ESCAPE_NORTH)) {
                mEscapeNorthEnabled = false;
            }

            if (info.privateImeOptions.contains(IME_PRIVATE_OPTIONS_VOICE_DISMISS)) {
                mVoiceKeyDismissesEnabled = false;
            }
        }

        mEnterKeyText = info.actionLabel;
        if (TextUtils.isEmpty(mEnterKeyText)) {
            switch (LeanbackUtils.getImeAction(info)) {
                case EditorInfo.IME_ACTION_GO:
                    this.mEnterKeyTextResId = R.string.label_go_key;
                    return;
                case EditorInfo.IME_ACTION_SEARCH:
                    this.mEnterKeyTextResId = R.string.label_search_key;
                    return;
                case EditorInfo.IME_ACTION_SEND:
                    this.mEnterKeyTextResId = R.string.label_send_key;
                    return;
                case EditorInfo.IME_ACTION_NEXT:
                    this.mEnterKeyTextResId = R.string.label_next_key;
                    return;
                default:
                    this.mEnterKeyTextResId = R.string.label_done_key;
            }
        }

    }

    /**
     * Move focus to specified key
     * @param focus key that will be focused
     * @param forceFocusChange force focus
     * @param animate animate transition
     */
    private void setKbFocus(final LeanbackKeyboardContainer.KeyFocus focus, final boolean forceFocusChange, final boolean animate) {
        boolean clicked = true;
        if (!focus.equals(this.mCurrKeyInfo) || forceFocusChange) {
            LeanbackKeyboardView prevView = this.mPrevView;
            this.mPrevView = null;
            boolean overestimateWidth = false;
            boolean overestimateHeight = false;
            switch (focus.type) {
                case KeyFocus.TYPE_MAIN:
                    if (focus.code != 32) {
                        overestimateWidth = true;
                    } else {
                        overestimateWidth = false;
                    }

                    LeanbackKeyboardView mainView = this.mMainKeyboardView;
                    int index = focus.index;
                    if (this.mTouchState == 3) {
                        overestimateHeight = true;
                    } else {
                        overestimateHeight = false;
                    }

                    mainView.setFocus(index, overestimateHeight, overestimateWidth);
                    this.mPrevView = this.mMainKeyboardView;
                    overestimateHeight = true;
                    break;
                case KeyFocus.TYPE_VOICE:
                    this.mVoiceButtonView.setMicFocused(true);
                    this.dismissMiniKeyboard();
                    break;
                case KeyFocus.TYPE_ACTION:
                    LeanbackUtils.sendAccessibilityEvent(this.mActionButtonView, true);
                    this.dismissMiniKeyboard();
                    break;
                case KeyFocus.TYPE_SUGGESTION:
                    this.dismissMiniKeyboard();
            }

            if (prevView != null && prevView != this.mPrevView) {
                if (this.mTouchState != 3) {
                    clicked = false;
                }

                prevView.setFocus(-1, clicked);
            }

            this.setSelectorToFocus(focus.rect, overestimateWidth, overestimateHeight, animate);
            this.mCurrKeyInfo.set(focus);
        }
    }

    private void setShiftState(int state) {
        this.mMainKeyboardView.setShiftState(state);
    }

    private void setTouchStateInternal(int state) {
        this.mTouchState = state;
    }

    private void startRecognition(Context context) {
        this.mRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        this.mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "free_form");
        this.mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        this.mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            float peakRmsLevel = 0.0F;
            int rmsCounter = 0;

            @Override
            public void onBeginningOfSpeech() {
                LeanbackKeyboardContainer.this.mVoiceButtonView.showRecording();
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
                LeanbackKeyboardContainer.this.mVoiceButtonView.showRecognizing();
                LeanbackKeyboardContainer.this.mVoiceOn = false;
            }

            @Override
            public void onError(int i) {
                LeanbackKeyboardContainer.this.cancelVoiceRecording();
                switch (i) {
                    case 4:
                        Log.d("LbKbContainer", "recognizer error server error");
                        return;
                    case 5:
                        Log.d("LbKbContainer", "recognizer error client error");
                        return;
                    case 6:
                        Log.d("LbKbContainer", "recognizer error speech timeout");
                        return;
                    case 7:
                        Log.d("LbKbContainer", "recognizer error no match");
                        return;
                    default:
                        Log.d("LbKbContainer", "recognizer other error " + i);
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                synchronized (this) {
                }
            }

            @Override
            public void onReadyForSpeech(Bundle bundle) {
                LeanbackKeyboardContainer.this.mVoiceButtonView.showListening();
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList var2 = bundle.getStringArrayList("results_recognition");
                if (var2 != null && LeanbackKeyboardContainer.this.mVoiceListener != null) {
                    LeanbackKeyboardContainer.this.mVoiceListener.onVoiceResult((String) var2.get(0));
                }

                LeanbackKeyboardContainer.this.cancelVoiceRecording();
            }

            @Override
            public void onRmsChanged(float v) {
                // $FF: Couldn't be decompiled
                throw new IllegalStateException("method not implemented");
            }
        });
        this.mSpeechRecognizer.startListening(this.mRecognizerIntent);
    }

    public void alignSelector(final float x, final float y, final boolean playAnimation) {
        final float translatedX = x - (float) (this.mSelector.getWidth() / 2);
        final float translatedY = y - (float) (this.mSelector.getHeight() / 2);
        if (!playAnimation) {
            this.mSelector.setX(translatedX);
            this.mSelector.setY(translatedY);
        } else {
            this.mSelector.animate().x(translatedX).y(translatedY).setInterpolator(sMovementInterpolator).setDuration(MOVEMENT_ANIMATION_DURATION).start();
        }
    }

    public boolean areSuggestionsEnabled() {
        return this.mSuggestionsEnabled;
    }

    public void cancelVoiceRecording() {
        this.mVoiceAnimator.startExitAnimation();
    }

    public void clearSuggestions() {
        this.mSuggestions.removeAllViews();
        if (this.getCurrFocus().type == 3) {
            this.resetFocusCursor();
        }

    }

    public boolean dismissMiniKeyboard() {
        return this.mMainKeyboardView.dismissMiniKeyboard();
    }

    public boolean enableAutoEnterSpace() {
        return this.mAutoEnterSpaceEnabled;
    }

    public boolean getBestFocus(final Float x, final Float y, final LeanbackKeyboardContainer.KeyFocus focus) {
        this.offsetRect(this.mRect, this.mActionButtonView);
        int actionLeft = this.mRect.left;
        this.offsetRect(this.mRect, this.mMainKeyboardView);
        int keyboardTop = this.mRect.top;
        Float newX = x;
        if (x == null) {
            newX = this.mX;
        }

        Float newY = y;
        if (y == null) {
            newY = this.mY;
        }

        int count = this.mSuggestions.getChildCount();
        if (newY < (float) keyboardTop && count > 0 && this.mSuggestionsEnabled) {
            for (actionLeft = 0; actionLeft < count; ++actionLeft) {
                View view = this.mSuggestions.getChildAt(actionLeft);
                this.offsetRect(this.mRect, view);
                if (newX < (float) this.mRect.right || actionLeft + 1 == count) {
                    view.requestFocus();
                    LeanbackUtils.sendAccessibilityEvent(view.findViewById(R.id.text), true);
                    this.configureFocus(focus, this.mRect, actionLeft, 3);
                    break;
                }
            }

            return true;
        } else if (newY < (float) keyboardTop && this.mEscapeNorthEnabled) {
            this.escapeNorth();
            return false;
        } else if (newX > (float) actionLeft) {
            this.offsetRect(this.mRect, this.mActionButtonView);
            this.configureFocus(focus, this.mRect, 0, 2);
            return true;
        } else {
            this.mX = newX;
            this.mY = newY;
            this.offsetRect(this.mRect, this.mMainKeyboardView);
            final float left = (float) this.mRect.left;
            final float top = (float) this.mRect.top;
            actionLeft = this.mMainKeyboardView.getNearestIndex(Float.valueOf(newX - left), Float.valueOf(newY - top));
            Key key = this.mMainKeyboardView.getKey(actionLeft);
            this.configureFocus(focus, this.mRect, actionLeft, key, 0);
            return true;
        }
    }

    public LeanbackKeyboardContainer.KeyFocus getCurrFocus() {
        return this.mCurrKeyInfo;
    }

    public int getCurrKeyCode() {
        int var1 = 0;
        Key var2 = this.getKey(this.mCurrKeyInfo.type, this.mCurrKeyInfo.index);
        if (var2 != null) {
            var1 = var2.codes[0];
        }

        return var1;
    }

    public Button getGoButton() {
        return this.mActionButtonView;
    }

    public Key getKey(int type, int index) {
        return type == KeyFocus.TYPE_MAIN ? this.mMainKeyboardView.getKey(index) : null;
    }

    public boolean getNextFocusInDirection(int direction, LeanbackKeyboardContainer.KeyFocus startFocus, LeanbackKeyboardContainer.KeyFocus nextFocus) {
        switch (startFocus.type) {
            case KeyFocus.TYPE_MAIN:
                Key key = this.getKey(startFocus.type, startFocus.index);
                float var5 = (float) startFocus.rect.height() / 2.0F;
                float var4 = (float) startFocus.rect.centerX();
                float var6 = (float) startFocus.rect.centerY();
                if (startFocus.code == 32) {
                    var4 = this.mX;
                }

                if ((direction & 1) != 0) {
                    if ((key.edgeFlags & 1) == 0) {
                        var4 = (float) startFocus.rect.left - var5;
                    }
                } else if ((direction & 4) != 0) {
                    if ((key.edgeFlags & 2) != 0) {
                        this.offsetRect(this.mRect, this.mActionButtonView);
                        var4 = (float) this.mRect.centerX();
                    } else {
                        var4 = (float) startFocus.rect.right + var5;
                    }
                }

                if ((direction & 8) != 0) {
                    var5 = (float) ((double) var6 - (double) startFocus.rect.height() * DIRECTION_STEP_MULTIPLIER);
                } else {
                    var5 = var6;
                    if ((direction & 2) != 0) {
                        var5 = (float) ((double) var6 + (double) startFocus.rect.height() * DIRECTION_STEP_MULTIPLIER);
                    }
                }

                this.getPhysicalPosition(var4, var5, this.mTempPoint);
                return this.getBestFocus(var4, var5, nextFocus);
            case KeyFocus.TYPE_VOICE:
            default:
                break;
            case KeyFocus.TYPE_ACTION:
                this.offsetRect(this.mRect, this.mMainKeyboardView);
                if ((direction & 1) != 0) {
                    return this.getBestFocus((float) this.mRect.right, null, nextFocus);
                }

                if ((direction & 8) != 0) {
                    this.offsetRect(this.mRect, this.mSuggestions);
                    return this.getBestFocus((float) startFocus.rect.centerX(), (float) this.mRect.centerY(), nextFocus);
                }
                break;
            case KeyFocus.TYPE_SUGGESTION:
                if ((direction & 2) != 0) {
                    this.offsetRect(this.mRect, this.mMainKeyboardView);
                    return this.getBestFocus((float) startFocus.rect.centerX(), (float) this.mRect.top, nextFocus);
                }

                if ((direction & 8) != 0) {
                    if (this.mEscapeNorthEnabled) {
                        this.escapeNorth();
                        return true;
                    }
                } else {
                    boolean var7;
                    if ((direction & 1) != 0) {
                        var7 = true;
                    } else {
                        var7 = false;
                    }

                    boolean var12;
                    if ((direction & 4) != 0) {
                        var12 = true;
                    } else {
                        var12 = false;
                    }

                    if (var7 || var12) {
                        this.offsetRect(this.mRect, this.mRootView);
                        MarginLayoutParams var11 = (MarginLayoutParams) this.mSuggestionsContainer.getLayoutParams();
                        int var8 = this.mRect.left + var11.leftMargin;
                        int var9 = this.mRect.right - var11.rightMargin;
                        int var10 = startFocus.index;
                        byte var13;
                        if (var7) {
                            var13 = -1;
                        } else {
                            var13 = 1;
                        }

                        direction = var10 + var13;
                        View var14 = this.mSuggestions.getChildAt(direction);
                        if (var14 != null) {
                            this.offsetRect(this.mRect, var14);
                            if (this.mRect.left < var8 && this.mRect.right > var9) {
                                this.mRect.left = var8;
                                this.mRect.right = var9;
                            } else if (this.mRect.left < var8) {
                                this.mRect.right = this.mRect.width() + var8;
                                this.mRect.left = var8;
                            } else if (this.mRect.right > var9) {
                                this.mRect.left = var9 - this.mRect.width();
                                this.mRect.right = var9;
                            }

                            var14.requestFocus();
                            LeanbackUtils.sendAccessibilityEvent(var14.findViewById(R.id.text), true);
                            this.configureFocus(nextFocus, this.mRect, direction, 3);
                            return true;
                        }
                    }
                }
        }

        return true;
    }

    public CharSequence getSuggestionText(int var1) {
        Object var3 = null;
        CharSequence var2 = (CharSequence) var3;
        if (var1 >= 0) {
            var2 = (CharSequence) var3;
            if (var1 < this.mSuggestions.getChildCount()) {
                Button var4 = (Button) this.mSuggestions.getChildAt(var1).findViewById(R.id.text);
                var2 = (CharSequence) var3;
                if (var4 != null) {
                    var2 = var4.getText();
                }
            }
        }

        return var2;
    }

    public int getTouchState() {
        return this.mTouchState;
    }

    public RelativeLayout getView() {
        return this.mRootView;
    }

    public boolean isCapsLockOn() {
        return this.mMainKeyboardView.getShiftState() == 2;
    }

    public boolean isCurrKeyShifted() {
        return this.mMainKeyboardView.isShifted();
    }

    public boolean isMiniKeyboardOnScreen() {
        return this.mMainKeyboardView.isMiniKeyboardOnScreen();
    }

    public boolean isVoiceEnabled() {
        return this.mVoiceEnabled;
    }

    public boolean isVoiceVisible() {
        return this.mVoiceButtonView.getVisibility() == 0;
    }

    public void onInitInputView() {
        this.resetFocusCursor();
        this.mSelector.setVisibility(View.VISIBLE);
    }

    public boolean onKeyLongPress() {
        int var1 = this.mCurrKeyInfo.code;
        if (var1 == -1) {
            this.onToggleCapsLock();
            this.setTouchState(0);
            return true;
        } else if (var1 == 32) {
            this.switchToNextKeyboard();
            this.setTouchState(0);
            return true;
        } else {
            if (this.mCurrKeyInfo.type == 0) {
                this.mMainKeyboardView.onKeyLongPress();
                if (this.mMainKeyboardView.isMiniKeyboardOnScreen()) {
                    this.mMiniKbKeyIndex = this.mCurrKeyInfo.index;
                    this.moveFocusToIndex(this.mMainKeyboardView.getBaseMiniKbIndex(), 0);
                    return true;
                }
            }

            return false;
        }
    }

    public void onModeChangeClick() {
        this.dismissMiniKeyboard();
        if (this.mMainKeyboardView.getKeyboard().equals(this.mSymKeyboard)) {
            this.mMainKeyboardView.setKeyboard(this.mInitialMainKeyboard);
        } else {
            this.mMainKeyboardView.setKeyboard(this.mSymKeyboard);
        }
    }

    public void onPeriodEntry() {
        if (this.mMainKeyboardView.isShifted()) {
            if (!this.isCapsLockOn() && !this.mCapCharacters && !this.mCapWords && !this.mCapSentences) {
                this.setShiftState(0);
            }
        } else if (this.isCapsLockOn() || this.mCapCharacters || this.mCapWords || this.mCapSentences) {
            this.setShiftState(1);
            return;
        }

    }

    public void onShiftClick() {
        byte state;
        if (mMainKeyboardView.isShifted()) {
            state = LeanbackKeyboardView.SHIFT_OFF;
        } else {
            state = LeanbackKeyboardView.SHIFT_ON;
        }

        setShiftState(state);
    }

    public void onShiftDoubleClick(boolean wasCapsLockOn) {
        byte state;
        if (wasCapsLockOn) {
            state = LeanbackKeyboardView.SHIFT_OFF;
        } else {
            state = LeanbackKeyboardView.SHIFT_LOCKED;
        }

        setShiftState(state);
    }

    public void onSpaceEntry() {
        if (mMainKeyboardView.isShifted()) {
            if (!isCapsLockOn() && !mCapCharacters && !mCapWords) {
                setShiftState(LeanbackKeyboardView.SHIFT_OFF);
            }
        } else if (isCapsLockOn() || mCapCharacters || mCapWords) {
            setShiftState(LeanbackKeyboardView.SHIFT_ON);
        }
    }

    public void onStartInput(EditorInfo info) {
        setImeOptions(mContext.getResources(), info);
        mVoiceOn = false;
    }

    @SuppressLint("NewApi")
    public void onStartInputView() {
        clearSuggestions();
        LayoutParams params = (LayoutParams) mKeyboardsContainer.getLayoutParams();
        if (mSuggestionsEnabled) {
            params.removeRule(10);
            mSuggestionsContainer.setVisibility(View.VISIBLE);
            mSuggestionsBg.setVisibility(View.VISIBLE);
        } else {
            params.addRule(10);
            mSuggestionsContainer.setVisibility(View.GONE);
            mSuggestionsBg.setVisibility(View.GONE);
        }

        mKeyboardsContainer.setLayoutParams(params);
        mMainKeyboardView.setKeyboard(mInitialMainKeyboard);
        mVoiceButtonView.setMicEnabled(mVoiceEnabled);
        resetVoice();
        dismissMiniKeyboard();
        if (!TextUtils.isEmpty(mEnterKeyText)) {
            mActionButtonView.setText(mEnterKeyText);
            mActionButtonView.setContentDescription(mEnterKeyText);
        } else {
            mActionButtonView.setText(mEnterKeyTextResId);
            mActionButtonView.setContentDescription(mContext.getString(mEnterKeyTextResId));
        }

        if (mCapCharacters) {
            setShiftState(LeanbackKeyboardView.SHIFT_LOCKED);
        } else if (!mCapSentences && !mCapWords) {
            setShiftState(LeanbackKeyboardView.SHIFT_OFF);
        } else {
            setShiftState(LeanbackKeyboardView.SHIFT_ON);
        }
    }

    public void onTextEntry() {
        if (mMainKeyboardView.isShifted()) {
            if (!isCapsLockOn() && !mCapCharacters) {
                setShiftState(LeanbackKeyboardView.SHIFT_OFF);
            }
        } else if (isCapsLockOn() || mCapCharacters) {
            setShiftState(LeanbackKeyboardView.SHIFT_LOCKED);
        }

        if (dismissMiniKeyboard()) {
            moveFocusToIndex(mMiniKbKeyIndex, KeyFocus.TYPE_MAIN);
        }

    }

    public void onVoiceClick() {
        if (mVoiceButtonView != null) {
            mVoiceButtonView.onClick();
        }

    }

    public void resetFocusCursor() {
        offsetRect(mRect, mMainKeyboardView);
        mX = (float) ((double) mRect.left + (double) mRect.width() * 0.45D);
        mY = (float) ((double) mRect.top + (double) mRect.height() * 0.375D);
        getBestFocus(mX, mY, mTempKeyInfo);
        setKbFocus(mTempKeyInfo, true, false);
        setTouchStateInternal(0);
        mSelectorAnimator.reverse();
        mSelectorAnimator.end();
    }

    public void resetVoice() {
        mMainKeyboardView.setAlpha(mAlphaIn);
        mActionButtonView.setAlpha(mAlphaIn);
        mVoiceButtonView.setAlpha(mAlphaOut);
        mMainKeyboardView.setVisibility(View.VISIBLE);
        mActionButtonView.setVisibility(View.VISIBLE);
        mVoiceButtonView.setVisibility(View.INVISIBLE);
    }

    public void setDismissListener(LeanbackKeyboardContainer.DismissListener listener) {
        mDismissListener = listener;
    }

    public void setFocus(LeanbackKeyboardContainer.KeyFocus focus) {
        setKbFocus(focus, false, true);
    }

    public void setSelectorToFocus(Rect rect, boolean overestimateWidth, boolean overestimateHeight, boolean animate) {
        if (this.mSelector.getWidth() != 0 && this.mSelector.getHeight() != 0 && rect.width() != 0 && rect.height() != 0) {
            final float width = (float) rect.width();
            final float height = (float) rect.height();
            float heightOver = height;
            if (overestimateHeight) {
                heightOver = height * this.mOverestimate;
            }

            float widthOver = width;
            if (overestimateWidth) {
                widthOver = width * this.mOverestimate;
            }

            float deltaY = heightOver;
            float deltaX = widthOver;
            if ((double) (Math.max(widthOver, heightOver) / Math.min(widthOver, heightOver)) < 1.1D) {
                deltaY = Math.max(widthOver, heightOver);
                deltaX = deltaY;
            }

            final float x = rect.exactCenterX() - deltaX / 2.0F;
            final float y = rect.exactCenterY() - deltaY / 2.0F;
            this.mSelectorAnimation.cancel();
            if (animate) {
                this.mSelectorAnimation.reset();
                this.mSelectorAnimation.setAnimationBounds(x, y, deltaX, deltaY);
                this.mSelector.startAnimation(this.mSelectorAnimation);
            } else {
                this.mSelectorAnimation.setValues(x, y, deltaX, deltaY);
            }
        }
    }

    public void setTouchState(final int state) {
        switch (state) {
            case TOUCH_STATE_NO_TOUCH:
                if (mTouchState == TOUCH_STATE_TOUCH_MOVE || mTouchState == TOUCH_STATE_CLICK) {
                    mSelectorAnimator.reverse();
                }
                break;
            case TOUCH_STATE_TOUCH_SNAP:
                if (mTouchState == TOUCH_STATE_CLICK) {
                    mSelectorAnimator.reverse();
                } else if (mTouchState == TOUCH_STATE_TOUCH_MOVE) {
                    mSelectorAnimator.reverse();
                }
                break;
            case TOUCH_STATE_TOUCH_MOVE:
                if (mTouchState == TOUCH_STATE_NO_TOUCH || mTouchState == TOUCH_STATE_TOUCH_SNAP) {
                    mSelectorAnimator.start();
                }
                break;
            case TOUCH_STATE_CLICK:
                if (mTouchState == TOUCH_STATE_NO_TOUCH || mTouchState == TOUCH_STATE_TOUCH_SNAP) {
                    mSelectorAnimator.start();
                }
        }

        setTouchStateInternal(state);
        setKbFocus(mCurrKeyInfo, true, true);
    }

    public void setVoiceListener(LeanbackKeyboardContainer.VoiceListener var1) {
        this.mVoiceListener = var1;
    }

    public void startVoiceRecording() {
        if (this.mVoiceEnabled) {
            if (!this.mVoiceKeyDismissesEnabled) {
                this.mVoiceAnimator.startEnterAnimation();
                return;
            }

            this.mDismissListener.onDismiss(true);
        }

    }

    public void switchToNextKeyboard() {
        LeanbackKeyboardView var1 = this.mMainKeyboardView;
        Keyboard var2 = this.mKeyboardManager.getNextKeyboard();
        this.mInitialMainKeyboard = var2;
        var1.setKeyboard(var2);
    }

    public void updateAddonKeyboard() {
        KeyboardManager var1 = new KeyboardManager(this.mContext, this.mAbcKeyboard);
        this.mKeyboardManager = var1;
        this.mInitialMainKeyboard = var1.getNextKeyboard();
    }

    public void updateSuggestions(ArrayList<String> var1) {
        int var2 = this.mSuggestions.getChildCount();
        int var3 = var1.size();
        if (var3 < var2) {
            this.mSuggestions.removeViews(var3, var2 - var3);
        } else if (var3 > var2) {
            while (var2 < var3) {
                View var4 = this.mContext.getLayoutInflater().inflate(R.layout.candidate, (ViewGroup) null);
                this.mSuggestions.addView(var4);
                ++var2;
            }
        }

        for (var2 = 0; var2 < var3; ++var2) {
            Button var5 = (Button) this.mSuggestions.getChildAt(var2).findViewById(R.id.text);
            var5.setText((CharSequence) var1.get(var2));
            var5.setContentDescription((CharSequence) var1.get(var2));
        }

        if (this.getCurrFocus().type == 3) {
            this.resetFocusCursor();
        }

    }

    public interface DismissListener {
        void onDismiss(boolean var1);
    }

    public static class KeyFocus {
        public static final int TYPE_ACTION = 2;
        public static final int TYPE_INVALID = -1;
        public static final int TYPE_MAIN = 0;
        public static final int TYPE_SUGGESTION = 3;
        public static final int TYPE_VOICE = 1;
        int code;
        int index;
        CharSequence label;
        final Rect rect = new Rect();
        int type = -1;

        @Override
        public boolean equals(Object obj) {
            if (this != obj) {
                if (obj == null || this.getClass() != obj.getClass()) {
                    return false;
                }

                LeanbackKeyboardContainer.KeyFocus focus = (LeanbackKeyboardContainer.KeyFocus) obj;
                if (this.code != focus.code) {
                    return false;
                }

                if (this.index != focus.index) {
                    return false;
                }

                if (this.type != focus.type) {
                    return false;
                }

                // equality must be commutative
                if (this.label == null && focus.label != null) {
                    return false;
                }

                if (this.label != null && !this.label.equals(focus.label)) {
                    return false;
                }

                if (!this.rect.equals(focus.rect)) {
                    return false;
                }
            }

            return true;
        }

        public int hashCode() {
            int hash = this.rect.hashCode();
            int index = this.index;
            int type = this.type;
            int code = this.code;
            int salt;
            if (this.label != null) {
                salt = this.label.hashCode();
            } else {
                salt = 0;
            }

            return (((hash * 31 + index) * 31 + type) * 31 + code) * 31 + salt;
        }

        public void set(LeanbackKeyboardContainer.KeyFocus focus) {
            this.index = focus.index;
            this.type = focus.type;
            this.code = focus.code;
            this.label = focus.label;
            this.rect.set(focus.rect);
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[type: ").append(this.type).append(", index: ").append(this.index).append(", code: ").append(this.code).append(", label: " +
                    "").append(this.label).append(", rect: ").append(this.rect).append("]");
            return builder.toString();
        }
    }

    private class ScaleAnimation extends Animation {
        private float mEndHeight;
        private float mEndWidth;
        private float mEndX;
        private float mEndY;
        private final android.view.ViewGroup.LayoutParams mParams;
        private float mStartHeight;
        private float mStartWidth;
        private float mStartX;
        private float mStartY;
        private final View mView;

        public ScaleAnimation(FrameLayout var2) {
            this.mView = var2;
            this.mParams = var2.getLayoutParams();
            this.setDuration(MOVEMENT_ANIMATION_DURATION);
            this.setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator);
        }

        protected void applyTransformation(float var1, Transformation var2) {
            if (var1 == 0.0F) {
                this.mStartX = this.mView.getX();
                this.mStartY = this.mView.getY();
                this.mStartWidth = (float) this.mParams.width;
                this.mStartHeight = (float) this.mParams.height;
            } else {
                this.setValues((this.mEndX - this.mStartX) * var1 + this.mStartX, (this.mEndY - this.mStartY) * var1 + this.mStartY, (float) ((int)
                        ((this.mEndWidth - this.mStartWidth) * var1 + this.mStartWidth)), (float) ((int) ((this.mEndHeight - this.mStartHeight) *
                        var1 + this.mStartHeight)));
            }
        }

        public void setAnimationBounds(float var1, float var2, float var3, float var4) {
            this.mEndX = var1;
            this.mEndY = var2;
            this.mEndWidth = var3;
            this.mEndHeight = var4;
        }

        public void setValues(float var1, float var2, float var3, float var4) {
            this.mView.setX(var1);
            this.mView.setY(var2);
            this.mParams.width = (int) var3;
            this.mParams.height = (int) var4;
            this.mView.setLayoutParams(this.mParams);
            this.mView.requestLayout();
        }
    }

    private class VoiceIntroAnimator {
        private AnimatorListener mEnterListener;
        private AnimatorListener mExitListener;
        private ValueAnimator mValueAnimator;

        public VoiceIntroAnimator(AnimatorListener var2, AnimatorListener var3) {
            this.mEnterListener = var2;
            this.mExitListener = var3;
            this.mValueAnimator = ValueAnimator.ofFloat(new float[]{LeanbackKeyboardContainer.this.mAlphaOut, LeanbackKeyboardContainer.this.mAlphaIn});
            this.mValueAnimator.setDuration((long) LeanbackKeyboardContainer.this.mVoiceAnimDur);
            this.mValueAnimator.setInterpolator(new AccelerateInterpolator());
        }

        private void start(final boolean var1) {
            this.mValueAnimator.cancel();
            this.mValueAnimator.removeAllListeners();
            ValueAnimator var3 = this.mValueAnimator;
            AnimatorListener var2;
            if (var1) {
                var2 = this.mEnterListener;
            } else {
                var2 = this.mExitListener;
            }

            var3.addListener(var2);
            this.mValueAnimator.removeAllUpdateListeners();
            this.mValueAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator var1x) {
                    float var2 = (Float) VoiceIntroAnimator.this.mValueAnimator.getAnimatedValue();
                    float var4 = LeanbackKeyboardContainer.this.mAlphaIn + LeanbackKeyboardContainer.this.mAlphaOut - var2;
                    float var3;
                    if (var1) {
                        var3 = var4;
                    } else {
                        var3 = var2;
                    }

                    if (var1) {
                        var4 = var2;
                    }

                    LeanbackKeyboardContainer.this.mMainKeyboardView.setAlpha(var3);
                    LeanbackKeyboardContainer.this.mActionButtonView.setAlpha(var3);
                    LeanbackKeyboardContainer.this.mVoiceButtonView.setAlpha(var4);
                    if (var2 == LeanbackKeyboardContainer.this.mAlphaOut) {
                        if (!var1) {
                            LeanbackKeyboardContainer.this.mMainKeyboardView.setVisibility(View.VISIBLE);
                            LeanbackKeyboardContainer.this.mActionButtonView.setVisibility(View.VISIBLE);
                            return;
                        }

                        LeanbackKeyboardContainer.this.mVoiceButtonView.setVisibility(View.VISIBLE);
                    } else if (var2 == LeanbackKeyboardContainer.this.mAlphaIn) {
                        if (var1) {
                            LeanbackKeyboardContainer.this.mMainKeyboardView.setVisibility(View.INVISIBLE);
                            LeanbackKeyboardContainer.this.mActionButtonView.setVisibility(View.INVISIBLE);
                            return;
                        }

                        LeanbackKeyboardContainer.this.mVoiceButtonView.setVisibility(View.INVISIBLE);
                        return;
                    }

                }
            });
            this.mValueAnimator.start();
        }

        void startEnterAnimation() {
            if (!LeanbackKeyboardContainer.this.isVoiceVisible() && !this.mValueAnimator.isRunning()) {
                this.start(true);
            }

        }

        void startExitAnimation() {
            if (LeanbackKeyboardContainer.this.isVoiceVisible() && !this.mValueAnimator.isRunning()) {
                this.start(false);
            }

        }
    }

    public interface VoiceListener {
        void onVoiceResult(String var1);
    }
}
