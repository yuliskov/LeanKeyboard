package com.google.android.leanback.ime.voice;

import android.animation.TimeAnimator;
import android.animation.TimeAnimator.TimeListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
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
        this.mEmptyPaint = new Paint();
        TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.BitmapSoundLevelView, defStyleAttr, 0);
        this.mEnableBackgroundColor = styledAttrs.getColor(R.styleable.BitmapSoundLevelView_enabledBackgroundColor, Color.parseColor("#66FFFFFF"));
        this.mDisableBackgroundColor = styledAttrs.getColor(R.styleable.BitmapSoundLevelView_disabledBackgroundColor, -1);
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

        this.mCenterTranslationX = styledAttrs.getDimensionPixelOffset(R.styleable.BitmapSoundLevelView_levelsCenterX, 0);
        this.mCenterTranslationY = styledAttrs.getDimensionPixelOffset(R.styleable.BitmapSoundLevelView_levelsCenterY, 0);
        this.mMinimumLevelSize = styledAttrs.getDimensionPixelOffset(R.styleable.BitmapSoundLevelView_minLevelRadius, 0);
        styledAttrs.recycle();
        if (primaryLevelEnabled) {
            this.mPrimaryLevel = BitmapFactory.decodeResource(this.getResources(), primaryLevelId);
        } else {
            this.mPrimaryLevel = null;
        }

        if (peakLevelEnabled) {
            this.mTrailLevel = BitmapFactory.decodeResource(this.getResources(), trailLevelId);
        } else {
            this.mTrailLevel = null;
        }

        this.mPaint = new Paint();
        this.mDestRect = new Rect();
        this.mEmptyPaint.setFilterBitmap(true);
        this.mLevelSource = new SpeechLevelSource();
        this.mLevelSource.setSpeechLevel(0);
        this.mAnimator = new TimeAnimator();
        this.mAnimator.setRepeatCount(-1);
        this.mAnimator.setTimeListener(new TimeListener() {
            public void onTimeUpdate(TimeAnimator animator, long l, long l1) {
                BitmapSoundLevelView.this.invalidate();
            }
        });
    }

    @TargetApi(16)
    private void startAnimator() {
        if (!this.mAnimator.isStarted()) {
            this.mAnimator.start();
        }

    }

    @TargetApi(16)
    private void stopAnimator() {
        this.mAnimator.cancel();
    }

    private void updateAnimatorState() {
        if (this.isEnabled()) {
            this.startAnimator();
        } else {
            this.stopAnimator();
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.updateAnimatorState();
    }

    protected void onDetachedFromWindow() {
        this.stopAnimator();
        super.onDetachedFromWindow();
    }

    public void onDraw(Canvas canvas) {
        if (this.isEnabled()) {
            canvas.drawColor(this.mEnableBackgroundColor);
            final int level = this.mLevelSource.getSpeechLevel();
            if (level > this.mPeakLevel) {
                this.mPeakLevel = level;
                this.mPeakLevelCountDown = 25;
            } else if (this.mPeakLevelCountDown == 0) {
                this.mPeakLevel = Math.max(0, this.mPeakLevel - 2);
            } else {
                --this.mPeakLevelCountDown;
            }

            if (level > this.mCurrentVolume) {
                this.mCurrentVolume += (level - this.mCurrentVolume) / 4;
            } else {
                this.mCurrentVolume = (int) ((float) this.mCurrentVolume * 0.95F);
            }

            final int centerX = this.mCenterTranslationX + this.getWidth() / 2;
            final int centerY = this.mCenterTranslationY + this.getWidth() / 2;
            int size;
            if (this.mTrailLevel != null) {
                size = (centerX - this.mMinimumLevelSize) * this.mPeakLevel / 100 + this.mMinimumLevelSize;
                this.mDestRect.set(centerX - size, centerY - size, centerX + size, centerY + size);
                canvas.drawBitmap(this.mTrailLevel, (Rect) null, this.mDestRect, this.mEmptyPaint);
            }

            if (this.mPrimaryLevel != null) {
                size = (centerX - this.mMinimumLevelSize) * this.mCurrentVolume / 100 + this.mMinimumLevelSize;
                this.mDestRect.set(centerX - size, centerY - size, centerX + size, centerY + size);
                canvas.drawBitmap(this.mPrimaryLevel, (Rect) null, this.mDestRect, this.mEmptyPaint);
                this.mPaint.setColor(this.getResources().getColor(R.color.search_mic_background));
                this.mPaint.setStyle(Style.FILL);
                canvas.drawCircle((float) centerX, (float) centerY, (float) (this.mMinimumLevelSize - 3), this.mPaint);
            }

            if (this.mTrailLevel != null && this.mPrimaryLevel != null) {
                this.mPaint.setColor(this.getResources().getColor(R.color.search_mic_levels_guideline));
                this.mPaint.setStyle(Style.STROKE);
                canvas.drawCircle((float) centerX, (float) centerY, (float) (centerX - 13), this.mPaint);
            }

        } else {
            canvas.drawColor(this.mDisableBackgroundColor);
        }
    }

    public void onWindowFocusChanged(boolean var1) {
        super.onWindowFocusChanged(var1);
        if (var1) {
            this.updateAnimatorState();
        } else {
            this.stopAnimator();
        }
    }

    public void setEnabled(boolean var1) {
        super.setEnabled(var1);
        this.updateAnimatorState();
    }

    public void setLevelSource(SpeechLevelSource var1) {
        this.mLevelSource = var1;
    }
}
