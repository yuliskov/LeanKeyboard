package com.google.android.leanback.ime;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
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
    private int mTouchState = 0;
    private final int mVoiceAnimDur;
    private final LeanbackKeyboardContainer.VoiceIntroAnimator mVoiceAnimator;
    private RecognizerView mVoiceButtonView;
    private boolean mVoiceEnabled;
    private AnimatorListener mVoiceEnterListener = new AnimatorListener() {
        public void onAnimationCancel(Animator var1) {
        }

        public void onAnimationEnd(Animator var1) {
        }

        public void onAnimationRepeat(Animator var1) {
        }

        public void onAnimationStart(Animator var1) {
            LeanbackKeyboardContainer.this.mSelector.setVisibility(View.INVISIBLE);
            LeanbackKeyboardContainer.this.startRecognition(LeanbackKeyboardContainer.this.mContext);
        }
    };
    private AnimatorListener mVoiceExitListener = new AnimatorListener() {
        public void onAnimationCancel(Animator var1) {
        }

        public void onAnimationEnd(Animator var1) {
            LeanbackKeyboardContainer.this.mSelector.setVisibility(View.VISIBLE);
        }

        public void onAnimationRepeat(Animator var1) {
        }

        public void onAnimationStart(Animator var1) {
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
        this.mContext = (LeanbackImeService) context;
        final Resources res = this.mContext.getResources();
        this.mVoiceAnimDur = res.getInteger(R.integer.voice_anim_duration);
        this.mAlphaIn = res.getFraction(R.fraction.alpha_in, 1, 1);
        this.mAlphaOut = res.getFraction(R.fraction.alpha_out, 1, 1);
        this.mVoiceAnimator = new LeanbackKeyboardContainer.VoiceIntroAnimator(this.mVoiceEnterListener, this.mVoiceExitListener);
        this.initKeyboards();
        this.mRootView = (RelativeLayout) this.mContext.getLayoutInflater().inflate(R.layout.root_leanback, null);
        this.mKeyboardsContainer = this.mRootView.findViewById(R.id.keyboard);
        this.mSuggestionsBg = this.mRootView.findViewById(R.id.candidate_background);
        this.mSuggestionsContainer = (HorizontalScrollView) this.mRootView.findViewById(R.id.suggestions_container);
        this.mSuggestions = (LinearLayout) this.mSuggestionsContainer.findViewById(R.id.suggestions);
        this.mMainKeyboardView = (LeanbackKeyboardView) this.mRootView.findViewById(R.id.main_keyboard);
        this.mVoiceButtonView = (RecognizerView) this.mRootView.findViewById(R.id.voice);
        this.mActionButtonView = (Button) this.mRootView.findViewById(R.id.enter);
        this.mSelector = this.mRootView.findViewById(R.id.selector);
        this.mSelectorAnimation = new LeanbackKeyboardContainer.ScaleAnimation((FrameLayout) this.mSelector);
        this.mOverestimate = this.mContext.getResources().getFraction(R.fraction.focused_scale, 1, 1);
        final float scale = context.getResources().getFraction(R.fraction.clicked_scale, 1, 1);
        this.mClickAnimDur = context.getResources().getInteger(R.integer.clicked_anim_duration);
        this.mSelectorAnimator = ValueAnimator.ofFloat(new float[]{1.0F, scale});
        this.mSelectorAnimator.setDuration((long) this.mClickAnimDur);
        this.mSelectorAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                final float value = (Float) animation.getAnimatedValue();
                LeanbackKeyboardContainer.this.mSelector.setScaleX(value);
                LeanbackKeyboardContainer.this.mSelector.setScaleY(value);
            }
        });
        this.mSpeechLevelSource = new SpeechLevelSource();
        this.mVoiceButtonView.setSpeechLevelSource(this.mSpeechLevelSource);
        this.mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.mContext);
        this.mVoiceButtonView.setCallback(new RecognizerView.Callback() {
            public void onCancelRecordingClicked() {
                LeanbackKeyboardContainer.this.cancelVoiceRecording();
            }

            public void onStartRecordingClicked() {
                LeanbackKeyboardContainer.this.startVoiceRecording();
            }

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
        result.x = posXCm / 12.0F * (width - size) + (float) this.mRootView.getPaddingLeft();
        result.y = posYCm / 5.0F * (height - size) + (float) this.mRootView.getPaddingTop();
        return result;
    }

    private void getPhysicalPosition(final float x, final float y, final PointF result) {
        float width = (float) (this.mSelector.getWidth() / 2);
        float height = (float) (this.mSelector.getHeight() / 2);
        float posXCm = (float) (this.mRootView.getWidth() - this.mRootView.getPaddingRight() - this.mRootView.getPaddingLeft());
        float posYCm = (float) (this.mRootView.getHeight() - this.mRootView.getPaddingTop() - this.mRootView.getPaddingBottom());
        float size = this.mContext.getResources().getDimension(R.dimen.selector_size);
        result.x = (x - width - (float) this.mRootView.getPaddingLeft()) * 12.0F / (posXCm - size);
        result.y = (y - height - (float) this.mRootView.getPaddingTop()) * 5.0F / (posYCm - size);
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

    private void moveFocusToIndex(int var1, int var2) {
        Key var3 = this.mMainKeyboardView.getKey(var1);
        this.configureFocus(this.mTempKeyInfo, this.mRect, var1, var3, var2);
        this.setTouchState(0);
        this.setKbFocus(this.mTempKeyInfo, true, true);
    }

    private void offsetRect(Rect var1, View var2) {
        var1.left = 0;
        var1.top = 0;
        var1.right = var2.getWidth();
        var1.bottom = var2.getHeight();
        this.mRootView.offsetDescendantRectToMyCoords(var2, var1);
    }

    private void onToggleCapsLock() {
        this.onShiftDoubleClick(this.isCapsLockOn());
    }

    private void setImeOptions(Resources var1, EditorInfo var2) {
        if (this.mInitialMainKeyboard == null) {
            this.mInitialMainKeyboard = this.mAbcKeyboard;
        }

        this.mSuggestionsEnabled = false;
        this.mAutoEnterSpaceEnabled = false;
        this.mVoiceEnabled = false;
        this.mEscapeNorthEnabled = false;
        this.mVoiceKeyDismissesEnabled = false;
        label67:
        switch (LeanbackUtils.getInputTypeClass(var2)) {
            case 1:
                switch (LeanbackUtils.getInputTypeVariation(var2)) {
                    case 16:
                    case 32:
                    case 160:
                    case 208:
                        this.mSuggestionsEnabled = false;
                        this.mAutoEnterSpaceEnabled = false;
                        this.mVoiceEnabled = false;
                        this.mInitialMainKeyboard = this.mAbcKeyboard;
                        break label67;
                    case 96:
                    case 128:
                    case 144:
                    case 224:
                        this.mSuggestionsEnabled = false;
                        this.mVoiceEnabled = false;
                        this.mInitialMainKeyboard = this.mAbcKeyboard;
                    default:
                        break label67;
                }
            case 2:
            case 3:
            case 4:
                this.mSuggestionsEnabled = false;
                this.mVoiceEnabled = false;
                this.mInitialMainKeyboard = this.mAbcKeyboard;
        }

        if (this.mSuggestionsEnabled) {
            if ((var2.inputType & 524288) == 0) {
                ;
            }

            this.mSuggestionsEnabled = false;
        }

        if (this.mAutoEnterSpaceEnabled) {
            if (this.mSuggestionsEnabled && this.mAutoEnterSpaceEnabled) {
                ;
            }

            this.mAutoEnterSpaceEnabled = false;
        }

        if ((var2.inputType & 16384) != 0) {
            ;
        }

        this.mCapSentences = false;
        if ((var2.inputType & 8192) == 0 && LeanbackUtils.getInputTypeVariation(var2) == 96) {
            ;
        }

        this.mCapWords = false;
        if ((var2.inputType & 4096) != 0) {
            ;
        }

        this.mCapCharacters = false;
        if (var2.privateImeOptions != null) {
            if (var2.privateImeOptions.contains("EscapeNorth=1")) {
                this.mEscapeNorthEnabled = false;
            }

            if (var2.privateImeOptions.contains("VoiceDismiss=1")) {
                this.mVoiceKeyDismissesEnabled = false;
            }
        }

        this.mEnterKeyText = var2.actionLabel;
        if (TextUtils.isEmpty(this.mEnterKeyText)) {
            switch (LeanbackUtils.getImeAction(var2)) {
                case 2:
                    this.mEnterKeyTextResId = R.string.label_go_key;
                    return;
                case 3:
                    this.mEnterKeyTextResId = R.string.label_search_key;
                    return;
                case 4:
                    this.mEnterKeyTextResId = R.string.label_send_key;
                    return;
                case 5:
                    this.mEnterKeyTextResId = R.string.label_next_key;
                    return;
                default:
                    this.mEnterKeyTextResId = R.string.label_done_key;
            }
        }

    }

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

    private void setShiftState(int var1) {
        this.mMainKeyboardView.setShiftState(var1);
    }

    private void setTouchStateInternal(int var1) {
        this.mTouchState = var1;
    }

    private void startRecognition(Context var1) {
        this.mRecognizerIntent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        this.mRecognizerIntent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        this.mRecognizerIntent.putExtra("android.speech.extra.PARTIAL_RESULTS", true);
        this.mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            float peakRmsLevel = 0.0F;
            int rmsCounter = 0;

            public void onBeginningOfSpeech() {
                LeanbackKeyboardContainer.this.mVoiceButtonView.showRecording();
            }

            public void onBufferReceived(byte[] var1) {
            }

            public void onEndOfSpeech() {
                LeanbackKeyboardContainer.this.mVoiceButtonView.showRecognizing();
                LeanbackKeyboardContainer.this.mVoiceOn = false;
            }

            public void onError(int var1) {
                LeanbackKeyboardContainer.this.cancelVoiceRecording();
                switch (var1) {
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
                        Log.d("LbKbContainer", "recognizer other error " + var1);
                }
            }

            public void onEvent(int var1, Bundle var2) {
            }

            public void onPartialResults(Bundle var1) {
                synchronized (this) {
                }
            }

            public void onReadyForSpeech(Bundle var1) {
                LeanbackKeyboardContainer.this.mVoiceButtonView.showListening();
            }

            public void onResults(Bundle var1) {
                ArrayList var2 = var1.getStringArrayList("results_recognition");
                if (var2 != null && LeanbackKeyboardContainer.this.mVoiceListener != null) {
                    LeanbackKeyboardContainer.this.mVoiceListener.onVoiceResult((String) var2.get(0));
                }

                LeanbackKeyboardContainer.this.cancelVoiceRecording();
            }

            public void onRmsChanged(float param1) {
                // $FF: Couldn't be decompiled
                throw new IllegalStateException("method not implemented");
            }
        });
        this.mSpeechRecognizer.startListening(this.mRecognizerIntent);
    }

    public void alignSelector(float var1, float var2, boolean var3) {
        var1 -= (float) (this.mSelector.getWidth() / 2);
        var2 -= (float) (this.mSelector.getHeight() / 2);
        if (!var3) {
            this.mSelector.setX(var1);
            this.mSelector.setY(var2);
        } else {
            this.mSelector.animate().x(var1).y(var2).setInterpolator(sMovementInterpolator).setDuration(150L).start();
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

    public Key getKey(int var1, int var2) {
        return var1 == 0 ? this.mMainKeyboardView.getKey(var2) : null;
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
        byte var1;
        if (this.mMainKeyboardView.isShifted()) {
            var1 = 0;
        } else {
            var1 = 1;
        }

        this.setShiftState(var1);
    }

    public void onShiftDoubleClick(boolean var1) {
        byte var2;
        if (var1) {
            var2 = 0;
        } else {
            var2 = 2;
        }

        this.setShiftState(var2);
    }

    public void onSpaceEntry() {
        if (this.mMainKeyboardView.isShifted()) {
            if (!this.isCapsLockOn() && !this.mCapCharacters && !this.mCapWords) {
                this.setShiftState(0);
            }
        } else if (this.isCapsLockOn() || this.mCapCharacters || this.mCapWords) {
            this.setShiftState(1);
            return;
        }

    }

    public void onStartInput(EditorInfo var1) {
        this.setImeOptions(this.mContext.getResources(), var1);
        this.mVoiceOn = false;
    }

    @TargetApi(17)
    public void onStartInputView() {
        this.clearSuggestions();
        LayoutParams params = (LayoutParams) this.mKeyboardsContainer.getLayoutParams();
        if (this.mSuggestionsEnabled) {
            params.removeRule(10);
            this.mSuggestionsContainer.setVisibility(View.VISIBLE);
            this.mSuggestionsBg.setVisibility(View.VISIBLE);
        } else {
            params.addRule(10);
            this.mSuggestionsContainer.setVisibility(View.GONE);
            this.mSuggestionsBg.setVisibility(View.GONE);
        }

        this.mKeyboardsContainer.setLayoutParams(params);
        this.mMainKeyboardView.setKeyboard(this.mInitialMainKeyboard);
        this.mVoiceButtonView.setMicEnabled(this.mVoiceEnabled);
        this.resetVoice();
        this.dismissMiniKeyboard();
        if (!TextUtils.isEmpty(this.mEnterKeyText)) {
            this.mActionButtonView.setText(this.mEnterKeyText);
            this.mActionButtonView.setContentDescription(this.mEnterKeyText);
        } else {
            this.mActionButtonView.setText(this.mEnterKeyTextResId);
            this.mActionButtonView.setContentDescription(this.mContext.getString(this.mEnterKeyTextResId));
        }

        if (this.mCapCharacters) {
            this.setShiftState(2);
        } else if (!this.mCapSentences && !this.mCapWords) {
            this.setShiftState(0);
        } else {
            this.setShiftState(1);
        }
    }

    public void onTextEntry() {
        if (this.mMainKeyboardView.isShifted()) {
            if (!this.isCapsLockOn() && !this.mCapCharacters) {
                this.setShiftState(0);
            }
        } else if (this.isCapsLockOn() || this.mCapCharacters) {
            this.setShiftState(2);
        }

        if (this.dismissMiniKeyboard()) {
            this.moveFocusToIndex(this.mMiniKbKeyIndex, 0);
        }

    }

    public void onVoiceClick() {
        if (this.mVoiceButtonView != null) {
            this.mVoiceButtonView.onClick();
        }

    }

    public void resetFocusCursor() {
        this.offsetRect(this.mRect, this.mMainKeyboardView);
        this.mX = (float) ((double) this.mRect.left + (double) this.mRect.width() * 0.45D);
        this.mY = (float) ((double) this.mRect.top + (double) this.mRect.height() * 0.375D);
        this.getBestFocus(this.mX, this.mY, this.mTempKeyInfo);
        this.setKbFocus(this.mTempKeyInfo, true, false);
        this.setTouchStateInternal(0);
        this.mSelectorAnimator.reverse();
        this.mSelectorAnimator.end();
    }

    public void resetVoice() {
        this.mMainKeyboardView.setAlpha(this.mAlphaIn);
        this.mActionButtonView.setAlpha(this.mAlphaIn);
        this.mVoiceButtonView.setAlpha(this.mAlphaOut);
        this.mMainKeyboardView.setVisibility(View.VISIBLE);
        this.mActionButtonView.setVisibility(View.VISIBLE);
        this.mVoiceButtonView.setVisibility(View.INVISIBLE);
    }

    public void setDismissListener(LeanbackKeyboardContainer.DismissListener listener) {
        this.mDismissListener = listener;
    }

    public void setFocus(LeanbackKeyboardContainer.KeyFocus focus) {
        this.setKbFocus(focus, false, true);
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

    public void setTouchState(int var1) {
        switch (var1) {
            case 0:
                if (this.mTouchState == 2 || this.mTouchState == 3) {
                    this.mSelectorAnimator.reverse();
                }
                break;
            case 1:
                if (this.mTouchState == 3) {
                    this.mSelectorAnimator.reverse();
                } else if (this.mTouchState == 2) {
                    this.mSelectorAnimator.reverse();
                }
                break;
            case 2:
                if (this.mTouchState == 0 || this.mTouchState == 1) {
                    this.mSelectorAnimator.start();
                }
                break;
            case 3:
                if (this.mTouchState == 0 || this.mTouchState == 1) {
                    this.mSelectorAnimator.start();
                }
        }

        this.setTouchStateInternal(var1);
        this.setKbFocus(this.mCurrKeyInfo, true, true);
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

        public boolean equals(Object var1) {
            if (this != var1) {
                if (var1 == null || this.getClass() != var1.getClass()) {
                    return false;
                }

                LeanbackKeyboardContainer.KeyFocus var2 = (LeanbackKeyboardContainer.KeyFocus) var1;
                if (this.code != var2.code) {
                    return false;
                }

                if (this.index != var2.index) {
                    return false;
                }

                if (this.type != var2.type) {
                    return false;
                }

                label31:
                {
                    if (this.label != null) {
                        if (this.label.equals(var2.label)) {
                            break label31;
                        }
                    } else if (var2.label == null) {
                        break label31;
                    }

                    return false;
                }

                if (!this.rect.equals(var2.rect)) {
                    return false;
                }
            }

            return true;
        }

        public int hashCode() {
            int var2 = this.rect.hashCode();
            int var3 = this.index;
            int var4 = this.type;
            int var5 = this.code;
            int var1;
            if (this.label != null) {
                var1 = this.label.hashCode();
            } else {
                var1 = 0;
            }

            return (((var2 * 31 + var3) * 31 + var4) * 31 + var5) * 31 + var1;
        }

        public void set(LeanbackKeyboardContainer.KeyFocus var1) {
            this.index = var1.index;
            this.type = var1.type;
            this.code = var1.code;
            this.label = var1.label;
            this.rect.set(var1.rect);
        }

        public String toString() {
            StringBuilder var1 = new StringBuilder();
            var1.append("[type: ").append(this.type).append(", index: ").append(this.index).append(", code: ").append(this.code).append(", label: " +
                    "").append(this.label).append(", rect: ").append(this.rect).append("]");
            return var1.toString();
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
            this.setDuration(150L);
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
