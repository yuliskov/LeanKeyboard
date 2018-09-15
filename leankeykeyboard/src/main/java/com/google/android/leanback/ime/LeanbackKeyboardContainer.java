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
import com.liskovsoft.other.ChooseKeyboardDialog;
import com.liskovsoft.utils.LeanKeyPreferences;
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
    public static final int DIRECTION_UP = 8;
    public static final int DIRECTION_DOWN = 2;
    public static final int DIRECTION_LEFT = 1;
    public static final int DIRECTION_RIGHT = 4;
    private Keyboard mAbcKeyboard;
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
        initKeyboards();
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
        mAbcKeyboard = new Keyboard(mContext, R.xml.qwerty_us);
        mSymKeyboard = new Keyboard(mContext, R.xml.sym_us);
        updateAddonKeyboard();
        mNumKeyboard = new Keyboard(mContext, R.xml.number);
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

    /**
     * Init currently displayed keyboard<br/>
     * Note: all keyboard settings applied here<br/>
     * Note: this method is called constantly on new field
     * @param res resources (not used)
     * @param info current ime attributes
     */
    private void setImeOptions(Resources res, EditorInfo info) {
        // do not erase last keyboard
        if (mInitialMainKeyboard == null) {
            mInitialMainKeyboard = mAbcKeyboard;
        }

        mSuggestionsEnabled = false;
        // fix auto space after period
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

        // TODO: many empty ifs

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
                    mEnterKeyTextResId = R.string.label_go_key;
                    return;
                case EditorInfo.IME_ACTION_SEARCH:
                    mEnterKeyTextResId = R.string.label_search_key;
                    return;
                case EditorInfo.IME_ACTION_SEND:
                    mEnterKeyTextResId = R.string.label_send_key;
                    return;
                case EditorInfo.IME_ACTION_NEXT:
                    mEnterKeyTextResId = R.string.label_next_key;
                    return;
                default:
                    mEnterKeyTextResId = R.string.label_done_key;
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
        if (!focus.equals(mCurrKeyInfo) || forceFocusChange) {
            LeanbackKeyboardView prevView = mPrevView;
            mPrevView = null;
            boolean overestimateWidth = false;
            boolean overestimateHeight = false;
            switch (focus.type) {
                case KeyFocus.TYPE_MAIN:
                    if (focus.code != LeanbackKeyboardView.ASCII_SPACE) {
                        overestimateWidth = true;
                    } else {
                        overestimateWidth = false;
                    }

                    LeanbackKeyboardView mainView = mMainKeyboardView;
                    int index = focus.index;
                    if (mTouchState == TOUCH_STATE_CLICK) {
                        overestimateHeight = true;
                    } else {
                        overestimateHeight = false;
                    }

                    mainView.setFocus(index, overestimateHeight, overestimateWidth);
                    mPrevView = mMainKeyboardView;
                    overestimateHeight = true;
                    break;
                case KeyFocus.TYPE_VOICE:
                    mVoiceButtonView.setMicFocused(true);
                    dismissMiniKeyboard();
                    break;
                case KeyFocus.TYPE_ACTION:
                    LeanbackUtils.sendAccessibilityEvent(mActionButtonView, true);
                    dismissMiniKeyboard();
                    break;
                case KeyFocus.TYPE_SUGGESTION:
                    dismissMiniKeyboard();
            }

            if (prevView != null && prevView != mPrevView) {
                if (mTouchState != TOUCH_STATE_CLICK) {
                    clicked = false;
                }

                prevView.setFocus(-1, clicked);
            }

            setSelectorToFocus(focus.rect, overestimateWidth, overestimateHeight, animate);
            mCurrKeyInfo.set(focus);
        }
    }

    /**
     * Set keyboard shift sate
     * @param state one of the
     * {@link LeanbackKeyboardView#SHIFT_ON SHIFT_ON},
     * {@link LeanbackKeyboardView#SHIFT_OFF SHIFT_OFF},
     * {@link LeanbackKeyboardView#SHIFT_LOCKED SHIFT_LOCKED}
     * constants
     */
    private void setShiftState(int state) {
        mMainKeyboardView.setShiftState(state);
    }

    private void setTouchStateInternal(int state) {
        mTouchState = state;
    }

    /**
     * Speech recognizer routine
     * @param context context
     */
    private void startRecognition(Context context) {
        mRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "free_form");
        mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
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
                ArrayList results = bundle.getStringArrayList("results_recognition");
                if (results != null && LeanbackKeyboardContainer.this.mVoiceListener != null) {
                    LeanbackKeyboardContainer.this.mVoiceListener.onVoiceResult((String) results.get(0));
                }

                LeanbackKeyboardContainer.this.cancelVoiceRecording();
            }

            @Override
            public void onRmsChanged(float v) {
                // $FF: Couldn't be decompiled
                throw new IllegalStateException("method not implemented");
            }
        });
        mSpeechRecognizer.startListening(this.mRecognizerIntent);
    }

    public void alignSelector(final float x, final float y, final boolean playAnimation) {
        final float translatedX = x - (float) (mSelector.getWidth() / 2);
        final float translatedY = y - (float) (mSelector.getHeight() / 2);
        if (!playAnimation) {
            mSelector.setX(translatedX);
            mSelector.setY(translatedY);
        } else {
            mSelector.animate().x(translatedX).y(translatedY).setInterpolator(sMovementInterpolator).setDuration(MOVEMENT_ANIMATION_DURATION).start();
        }
    }

    public boolean areSuggestionsEnabled() {
        return mSuggestionsEnabled;
    }

    public void cancelVoiceRecording() {
        mVoiceAnimator.startExitAnimation();
    }

    public void clearSuggestions() {
        mSuggestions.removeAllViews();
        if (getCurrFocus().type == KeyFocus.TYPE_SUGGESTION) {
            resetFocusCursor();
        }
    }

    public boolean dismissMiniKeyboard() {
        return mMainKeyboardView.dismissMiniKeyboard();
    }

    public boolean enableAutoEnterSpace() {
        return mAutoEnterSpaceEnabled;
    }

    /**
     * Initialize {@link KeyFocus focus} variable based on supplied coordinates
     * @param x x coordinates
     * @param y y coordinates
     * @param focus result focus
     * @return whether focus is found or not
     */
    public boolean getBestFocus(final Float x, final Float y, final LeanbackKeyboardContainer.KeyFocus focus) {
        offsetRect(mRect, mActionButtonView);
        int actionLeft = mRect.left;
        offsetRect(mRect, mMainKeyboardView);
        int keyboardTop = mRect.top;
        Float newX = x;
        if (x == null) {
            newX = mX;
        }

        Float newY = y;
        if (y == null) {
            newY = mY;
        }

        int count = mSuggestions.getChildCount();
        if (newY < (float) keyboardTop && count > 0 && mSuggestionsEnabled) {
            for (actionLeft = 0; actionLeft < count; ++actionLeft) {
                View view = mSuggestions.getChildAt(actionLeft);
                offsetRect(mRect, view);
                if (newX < (float) mRect.right || actionLeft + 1 == count) {
                    view.requestFocus();
                    LeanbackUtils.sendAccessibilityEvent(view.findViewById(R.id.text), true);
                    configureFocus(focus, mRect, actionLeft, KeyFocus.TYPE_SUGGESTION);
                    break;
                }
            }

            return true;
        } else if (newY < (float) keyboardTop && mEscapeNorthEnabled) {
            escapeNorth();
            return false;
        } else if (newX > (float) actionLeft) {
            offsetRect(mRect, mActionButtonView);
            configureFocus(focus, mRect, 0, KeyFocus.TYPE_ACTION);
            return true;
        } else {
            mX = newX;
            mY = newY;
            offsetRect(mRect, mMainKeyboardView);
            final float left = (float) mRect.left;
            final float top = (float) mRect.top;
            actionLeft = mMainKeyboardView.getNearestIndex(Float.valueOf(newX - left), Float.valueOf(newY - top));
            Key key = mMainKeyboardView.getKey(actionLeft);
            configureFocus(focus, mRect, actionLeft, key, 0);
            return true;
        }
    }

    public LeanbackKeyboardContainer.KeyFocus getCurrFocus() {
        return mCurrKeyInfo;
    }

    public int getCurrKeyCode() {
        int keyCode = 0;
        Key key = getKey(mCurrKeyInfo.type, mCurrKeyInfo.index);
        if (key != null) {
            keyCode = key.codes[0];
        }

        return keyCode;
    }

    public Button getGoButton() {
        return mActionButtonView;
    }

    public Key getKey(int type, int index) {
        return type == KeyFocus.TYPE_MAIN ? this.mMainKeyboardView.getKey(index) : null;
    }

    public boolean getNextFocusInDirection(int direction, LeanbackKeyboardContainer.KeyFocus startFocus, LeanbackKeyboardContainer.KeyFocus nextFocus) {
        switch (startFocus.type) {
            case KeyFocus.TYPE_MAIN:
                Key key = getKey(startFocus.type, startFocus.index);
                float centerDelta = (float) startFocus.rect.height() / 2.0F;
                float centerX = (float) startFocus.rect.centerX();
                float centerY = (float) startFocus.rect.centerY();
                if (startFocus.code == 32) {
                    centerX = mX;
                }

                if ((direction & DIRECTION_LEFT) != 0) {
                    if ((key.edgeFlags & 1) == 0) {
                        centerX = (float) startFocus.rect.left - centerDelta;
                    }
                } else if ((direction & DIRECTION_RIGHT) != 0) {
                    if ((key.edgeFlags & 2) != 0) {
                        offsetRect(mRect, mActionButtonView);
                        centerX = (float) mRect.centerX();
                    } else {
                        centerX = (float) startFocus.rect.right + centerDelta;
                    }
                }

                if ((direction & DIRECTION_UP) != 0) {
                    centerDelta = (float) ((double) centerY - (double) startFocus.rect.height() * DIRECTION_STEP_MULTIPLIER);
                } else {
                    centerDelta = centerY;
                    if ((direction & DIRECTION_DOWN) != 0) {
                        centerDelta = (float) ((double) centerY + (double) startFocus.rect.height() * DIRECTION_STEP_MULTIPLIER);
                    }
                }

                getPhysicalPosition(centerX, centerDelta, mTempPoint);
                return getBestFocus(centerX, centerDelta, nextFocus);
            case KeyFocus.TYPE_VOICE:
            default:
                break;
            case KeyFocus.TYPE_ACTION:
                offsetRect(mRect, mMainKeyboardView);
                if ((direction & DIRECTION_LEFT) != 0) {
                    return getBestFocus((float) mRect.right, null, nextFocus);
                }

                if ((direction & DIRECTION_UP) != 0) {
                    offsetRect(mRect, mSuggestions);
                    return getBestFocus((float) startFocus.rect.centerX(), (float) mRect.centerY(), nextFocus);
                }
                break;
            case KeyFocus.TYPE_SUGGESTION:
                if ((direction & DIRECTION_DOWN) != 0) {
                    offsetRect(mRect, mMainKeyboardView);
                    return getBestFocus((float) startFocus.rect.centerX(), (float) mRect.top, nextFocus);
                }

                if ((direction & DIRECTION_UP) != 0) {
                    if (mEscapeNorthEnabled) {
                        escapeNorth();
                        return true;
                    }
                } else {
                    boolean left;
                    if ((direction & DIRECTION_LEFT) != 0) {
                        left = true;
                    } else {
                        left = false;
                    }

                    boolean right;
                    if ((direction & DIRECTION_RIGHT) != 0) {
                        right = true;
                    } else {
                        right = false;
                    }

                    if (left || right) {
                        offsetRect(mRect, mRootView);
                        MarginLayoutParams params = (MarginLayoutParams) mSuggestionsContainer.getLayoutParams();
                        int leftCalc = mRect.left + params.leftMargin;
                        int rightCalc = mRect.right - params.rightMargin;
                        int focusIdx = startFocus.index;
                        byte delta;
                        if (left) {
                            delta = -1;
                        } else {
                            delta = 1;
                        }

                        int suggestIdx = focusIdx + delta;
                        View suggestion = mSuggestions.getChildAt(suggestIdx);
                        if (suggestion != null) {
                            offsetRect(mRect, suggestion);
                            if (mRect.left < leftCalc && mRect.right > rightCalc) {
                                mRect.left = leftCalc;
                                mRect.right = rightCalc;
                            } else if (mRect.left < leftCalc) {
                                mRect.right = mRect.width() + leftCalc;
                                mRect.left = leftCalc;
                            } else if (mRect.right > rightCalc) {
                                mRect.left = rightCalc - mRect.width();
                                mRect.right = rightCalc;
                            }

                            suggestion.requestFocus();
                            LeanbackUtils.sendAccessibilityEvent(suggestion.findViewById(R.id.text), true);
                            configureFocus(nextFocus, mRect, suggestIdx, KeyFocus.TYPE_SUGGESTION);
                            return true;
                        }
                    }
                }
        }

        return true;
    }

    public CharSequence getSuggestionText(int idx) {
        CharSequence result = null;
        if (idx >= 0) {
            if (idx < this.mSuggestions.getChildCount()) {
                Button btn = (Button) this.mSuggestions.getChildAt(idx).findViewById(R.id.text);
                if (btn != null) {
                    result = btn.getText();
                }
            }
        }

        return result;
    }

    public int getTouchState() {
        return mTouchState;
    }

    public RelativeLayout getView() {
        return mRootView;
    }

    public boolean isCapsLockOn() {
        return mMainKeyboardView.getShiftState() == 2;
    }

    public boolean isCurrKeyShifted() {
        return mMainKeyboardView.isShifted();
    }

    public boolean isMiniKeyboardOnScreen() {
        return mMainKeyboardView.isMiniKeyboardOnScreen();
    }

    public boolean isVoiceEnabled() {
        return mVoiceEnabled;
    }

    public boolean isVoiceVisible() {
        return mVoiceButtonView.getVisibility() == 0;
    }

    public void onInitInputView() {
        resetFocusCursor();
        mSelector.setVisibility(View.VISIBLE);
    }

    public boolean onKeyLongPress() {
        int keyCode = mCurrKeyInfo.code;
        if (keyCode == LeanbackKeyboardView.KEYCODE_SHIFT) {
            onToggleCapsLock();
            setTouchState(LeanbackKeyboardContainer.TOUCH_STATE_NO_TOUCH);
            return true;
        } else if (keyCode == LeanbackKeyboardView.ASCII_SPACE) {
            onLangKeyPress();
            setTouchState(LeanbackKeyboardContainer.TOUCH_STATE_NO_TOUCH);
            return true;
        } else if (keyCode == LeanbackKeyboardView.KEYCODE_LANG_TOGGLE) {
            // NOTE: normal constructor cannot be applied here
            new ChooseKeyboardDialog(mContext, mMainKeyboardView).run();
            return true;
        } else {
            if (mCurrKeyInfo.type == KeyFocus.TYPE_MAIN) {
                mMainKeyboardView.onKeyLongPress();
                if (mMainKeyboardView.isMiniKeyboardOnScreen()) {
                    mMiniKbKeyIndex = mCurrKeyInfo.index;
                    moveFocusToIndex(mMainKeyboardView.getBaseMiniKbIndex(), KeyFocus.TYPE_MAIN);
                    return true;
                }
            }

            return false;
        }
    }

    public void onModeChangeClick() {
        dismissMiniKeyboard();
        if (mMainKeyboardView.getKeyboard().equals(mSymKeyboard)) {
            mMainKeyboardView.setKeyboard(mInitialMainKeyboard);
        } else {
            mMainKeyboardView.setKeyboard(mSymKeyboard);
        }
    }

    public void onPeriodEntry() {
        if (mMainKeyboardView.isShifted()) {
            if (!isCapsLockOn() && !mCapCharacters && !mCapWords && !mCapSentences) {
                setShiftState(LeanbackKeyboardView.SHIFT_OFF);
            }
        } else if (isCapsLockOn() || mCapCharacters || mCapWords || mCapSentences) {
            setShiftState(LeanbackKeyboardView.SHIFT_ON);
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

    /**
     * Set touch state
     * @param state state e.g. {@link LeanbackKeyboardContainer#TOUCH_STATE_CLICK LeanbackKeyboardContainer.TOUCH_STATE_CLICK}
     */
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

    public void setVoiceListener(LeanbackKeyboardContainer.VoiceListener listener) {
        mVoiceListener = listener;
    }

    public void startVoiceRecording() {
        if (mVoiceEnabled) {
            if (!mVoiceKeyDismissesEnabled) {
                mVoiceAnimator.startEnterAnimation();
                return;
            }

            mDismissListener.onDismiss(true);
        }
    }

    /**
     * Switch to next keyboard (looped).
     * {@link KeyboardManager KeyboardManager} is the source behind all keyboard implementations
     */
    public void onLangKeyPress() {
        switchToNextKeyboard();

        showRunOnceDialog();
    }

    private void switchToNextKeyboard() {
        LeanbackKeyboardView keyboardView = mMainKeyboardView;
        Keyboard keyboard = mKeyboardManager.getNextKeyboard();
        mInitialMainKeyboard = keyboard;
        mAbcKeyboard = keyboard;
        keyboardView.setKeyboard(keyboard);
    }

    public void updateAddonKeyboard() {
        mKeyboardManager = new KeyboardManager(mContext);
        switchToNextKeyboard();
    }

    public void updateSuggestions(ArrayList<String> suggestions) {
        int oldCount = mSuggestions.getChildCount();
        int newCount = suggestions.size();
        if (newCount < oldCount) {
            mSuggestions.removeViews(newCount, oldCount - newCount);
        } else if (newCount > oldCount) {
            while (oldCount < newCount) {
                View suggestion = mContext.getLayoutInflater().inflate(R.layout.candidate, null);
                mSuggestions.addView(suggestion);
                ++oldCount;
            }
        }

        for (oldCount = 0; oldCount < newCount; ++oldCount) {
            Button suggestion = mSuggestions.getChildAt(oldCount).findViewById(R.id.text);
            suggestion.setText(suggestions.get(oldCount));
            suggestion.setContentDescription(suggestions.get(oldCount));
        }

        if (getCurrFocus().type == KeyFocus.TYPE_SUGGESTION) {
            resetFocusCursor();
        }
    }

    public void onLangKeyClick() {
        onLangKeyPress();
        // setTouchState(LeanbackKeyboardContainer.TOUCH_STATE_NO_TOUCH);
    }

    private void showRunOnceDialog() {
        LeanKeyPreferences prefs = LeanKeyPreferences.instance(mContext);
        boolean runOnce = prefs.isRunOnce();

        if (runOnce) {
            return;
        }

        prefs.setRunOnce(true);

        // NOTE: normal constructor cannot be applied here
        new ChooseKeyboardDialog(mContext, mMainKeyboardView).run();
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

        public ScaleAnimation(FrameLayout view) {
            mView = view;
            mParams = view.getLayoutParams();
            setDuration(MOVEMENT_ANIMATION_DURATION);
            setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {
            if (interpolatedTime == 0.0F) {
                mStartX = mView.getX();
                mStartY = mView.getY();
                mStartWidth = (float) mParams.width;
                mStartHeight = (float) mParams.height;
            } else {
                setValues((mEndX - mStartX) * interpolatedTime + mStartX,
                            (mEndY - mStartY) * interpolatedTime + mStartY,
                            (float) ((int) ((mEndWidth - mStartWidth) * interpolatedTime + mStartWidth)),
                            (float) ((int) ((mEndHeight - mStartHeight) * interpolatedTime + mStartHeight)));
            }
        }

        public void setAnimationBounds(float x, float y, float width, float height) {
            this.mEndX = x;
            this.mEndY = y;
            this.mEndWidth = width;
            this.mEndHeight = height;
        }

        public void setValues(float x, float y, float width, float height) {
            this.mView.setX(x);
            this.mView.setY(y);
            this.mParams.width = (int) width;
            this.mParams.height = (int) height;
            this.mView.setLayoutParams(this.mParams);
            this.mView.requestLayout();
        }
    }

    private class VoiceIntroAnimator {
        private AnimatorListener mEnterListener;
        private AnimatorListener mExitListener;
        private ValueAnimator mValueAnimator;

        public VoiceIntroAnimator(AnimatorListener enterListener, AnimatorListener exitListener) {
            mEnterListener = enterListener;
            mExitListener = exitListener;
            mValueAnimator = ValueAnimator.ofFloat(LeanbackKeyboardContainer.this.mAlphaOut, LeanbackKeyboardContainer.this.mAlphaIn);
            mValueAnimator.setDuration((long) LeanbackKeyboardContainer.this.mVoiceAnimDur);
            mValueAnimator.setInterpolator(new AccelerateInterpolator());
        }

        private void start(final boolean enterVoice) {
            mValueAnimator.cancel();
            mValueAnimator.removeAllListeners();
            ValueAnimator animation = mValueAnimator;
            AnimatorListener listener;
            if (enterVoice) {
                listener = mEnterListener;
            } else {
                listener = mExitListener;
            }

            animation.addListener(listener);
            mValueAnimator.removeAllUpdateListeners();
            mValueAnimator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(final ValueAnimator animation) {
                    float scale = (Float) VoiceIntroAnimator.this.mValueAnimator.getAnimatedValue();
                    float calcOpacity = LeanbackKeyboardContainer.this.mAlphaIn + LeanbackKeyboardContainer.this.mAlphaOut - scale;
                    float opacity;
                    if (enterVoice) {
                        opacity = calcOpacity;
                    } else {
                        opacity = scale;
                    }

                    if (enterVoice) {
                        calcOpacity = scale;
                    }

                    LeanbackKeyboardContainer.this.mMainKeyboardView.setAlpha(opacity);
                    LeanbackKeyboardContainer.this.mActionButtonView.setAlpha(opacity);
                    LeanbackKeyboardContainer.this.mVoiceButtonView.setAlpha(calcOpacity);
                    if (scale == LeanbackKeyboardContainer.this.mAlphaOut) {
                        if (!enterVoice) {
                            LeanbackKeyboardContainer.this.mMainKeyboardView.setVisibility(View.VISIBLE);
                            LeanbackKeyboardContainer.this.mActionButtonView.setVisibility(View.VISIBLE);
                            return;
                        }

                        LeanbackKeyboardContainer.this.mVoiceButtonView.setVisibility(View.VISIBLE);
                    } else if (scale == LeanbackKeyboardContainer.this.mAlphaIn) {
                        if (enterVoice) {
                            LeanbackKeyboardContainer.this.mMainKeyboardView.setVisibility(View.INVISIBLE);
                            LeanbackKeyboardContainer.this.mActionButtonView.setVisibility(View.INVISIBLE);
                            return;
                        }

                        LeanbackKeyboardContainer.this.mVoiceButtonView.setVisibility(View.INVISIBLE);
                    }
                }
            });
            mValueAnimator.start();
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
        void onVoiceResult(String result);
    }
}
