package com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.voice;

import android.animation.TimeAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.liskovsoft.leankeykeyboard.R;

public class BitmapSoundLevelView extends View {
    private static final boolean DEBUG = false;
    private static final int MIC_LEVEL_GUIDELINE_OFFSET = 13;
    private static final int MIC_PRIMARY_LEVEL_IMAGE_OFFSET = 3;
    private static final String TAG = "BitmapSoundLevelsView";
    private TimeAnimator mAnimator;
    private final int mCenterTranslationX;
    private final int mCenterTranslationY;
    private int mCurrentVolume;
    private Rect mDestRect;
    private final int mDisableBackgroundColor;
    private final Paint mEmptyPaint;
    private final int mEnableBackgroundColor;
    private SpeechLevelSource mLevelSource;
    private final int mMinimumLevelSize;
    private Paint mPaint;
    private int mPeakLevel;
    private int mPeakLevelCountDown;
    private final Bitmap mPrimaryLevel;
    private final Bitmap mTrailLevel;

    public BitmapSoundLevelView(Context context) {
        this(context, null);
    }

    public BitmapSoundLevelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @TargetApi(16)
    public BitmapSoundLevelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mEmptyPaint = new Paint();
        TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.BitmapSoundLevelView, defStyleAttr, 0);
        mEnableBackgroundColor = styledAttrs.getColor(R.styleable.BitmapSoundLevelView_enabledBackgroundColor, Color.parseColor("#66FFFFFF"));
        mDisableBackgroundColor = styledAttrs.getColor(R.styleable.BitmapSoundLevelView_disabledBackgroundColor, -1);
        boolean primaryLevelEnabled = false;
        boolean peakLevelEnabled = false;
        int primaryLevelId = 0;
        if (styledAttrs.hasValue(R.styleable.BitmapSoundLevelView_primaryLevels)) {
            primaryLevelId = styledAttrs.getResourceId(R.styleable.BitmapSoundLevelView_primaryLevels, R.drawable.vs_reactive_dark);
            primaryLevelEnabled = true;
        }

        int trailLevelId = 0;
        if (styledAttrs.hasValue(R.styleable.BitmapSoundLevelView_trailLevels)) {
            trailLevelId = styledAttrs.getResourceId(R.styleable.BitmapSoundLevelView_trailLevels, R.drawable.vs_reactive_light);
            peakLevelEnabled = true;
        }

        mCenterTranslationX = styledAttrs.getDimensionPixelOffset(R.styleable.BitmapSoundLevelView_levelsCenterX, 0);
        mCenterTranslationY = styledAttrs.getDimensionPixelOffset(R.styleable.BitmapSoundLevelView_levelsCenterY, 0);
        mMinimumLevelSize = styledAttrs.getDimensionPixelOffset(R.styleable.BitmapSoundLevelView_minLevelRadius, 0);
        styledAttrs.recycle();
        if (primaryLevelEnabled) {
            mPrimaryLevel = BitmapFactory.decodeResource(getResources(), primaryLevelId);
        } else {
            mPrimaryLevel = null;
        }

        if (peakLevelEnabled) {
            mTrailLevel = BitmapFactory.decodeResource(getResources(), trailLevelId);
        } else {
            mTrailLevel = null;
        }

        mPaint = new Paint();
        mDestRect = new Rect();
        mEmptyPaint.setFilterBitmap(true);
        mLevelSource = new SpeechLevelSource();
        mLevelSource.setSpeechLevel(0);
        mAnimator = new TimeAnimator();
        mAnimator.setRepeatCount(-1);
        mAnimator.setTimeListener((animation, totalTime, deltaTime) -> invalidate());
    }

    @TargetApi(16)
    private void startAnimator() {
        if (!mAnimator.isStarted()) {
            mAnimator.start();
        }

    }

    @TargetApi(16)
    private void stopAnimator() {
        mAnimator.cancel();
    }

    private void updateAnimatorState() {
        if (isEnabled()) {
            startAnimator();
        } else {
            stopAnimator();
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimatorState();
    }

    protected void onDetachedFromWindow() {
        stopAnimator();
        super.onDetachedFromWindow();
    }

    public void onDraw(Canvas canvas) {
        if (isEnabled()) {
            canvas.drawColor(mEnableBackgroundColor);
            final int level = mLevelSource.getSpeechLevel();
            if (level > mPeakLevel) {
                mPeakLevel = level;
                mPeakLevelCountDown = 25;
            } else if (mPeakLevelCountDown == 0) {
                mPeakLevel = Math.max(0, mPeakLevel - 2);
            } else {
                --mPeakLevelCountDown;
            }

            if (level > mCurrentVolume) {
                mCurrentVolume += (level - mCurrentVolume) / 4;
            } else {
                mCurrentVolume = (int) ((float) mCurrentVolume * 0.95F);
            }

            final int centerX = mCenterTranslationX + getWidth() / 2;
            final int centerY = mCenterTranslationY + getWidth() / 2;
            int size;
            if (mTrailLevel != null) {
                size = (centerX - mMinimumLevelSize) * mPeakLevel / 100 + mMinimumLevelSize;
                mDestRect.set(centerX - size, centerY - size, centerX + size, centerY + size);
                canvas.drawBitmap(mTrailLevel, null, mDestRect, mEmptyPaint);
            }

            if (mPrimaryLevel != null) {
                size = (centerX - mMinimumLevelSize) * mCurrentVolume / 100 + mMinimumLevelSize;
                mDestRect.set(centerX - size, centerY - size, centerX + size, centerY + size);
                canvas.drawBitmap(mPrimaryLevel, null, mDestRect, mEmptyPaint);
                mPaint.setColor(ContextCompat.getColor(getContext(), R.color.search_mic_background));
                mPaint.setStyle(Style.FILL);
                canvas.drawCircle((float) centerX, (float) centerY, (float) (mMinimumLevelSize - 3), mPaint);
            }

            if (mTrailLevel != null && mPrimaryLevel != null) {
                mPaint.setColor(ContextCompat.getColor(getContext(), R.color.search_mic_levels_guideline));
                mPaint.setStyle(Style.STROKE);
                canvas.drawCircle((float) centerX, (float) centerY, (float) (centerX - 13), mPaint);
            }

        } else {
            canvas.drawColor(mDisableBackgroundColor);
        }
    }

    public void onWindowFocusChanged(boolean var1) {
        super.onWindowFocusChanged(var1);
        if (var1) {
            updateAnimatorState();
        } else {
            stopAnimator();
        }
    }

    public void setEnabled(boolean var1) {
        super.setEnabled(var1);
        updateAnimatorState();
    }

    public void setLevelSource(SpeechLevelSource var1) {
        mLevelSource = var1;
    }
}
