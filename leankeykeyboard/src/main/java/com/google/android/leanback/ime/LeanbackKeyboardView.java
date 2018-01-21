package com.google.android.leanback.ime;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.liskovsoft.leankeykeyboard.R;

import java.util.Iterator;
import java.util.List;

public class LeanbackKeyboardView extends FrameLayout {
    public static final int ASCII_PERIOD = 46;
    public static final int ASCII_SPACE = 32;
    private static final boolean DEBUG = false;
    public static final int KEYCODE_CAPS_LOCK = -6;
    public static final int KEYCODE_DELETE = -5;
    public static final int KEYCODE_DISMISS_MINI_KEYBOARD = -8;
    public static final int KEYCODE_LEFT = -3;
    public static final int KEYCODE_RIGHT = -4;
    public static final int KEYCODE_SHIFT = -1;
    public static final int KEYCODE_SYM_TOGGLE = -2;
    public static final int KEYCODE_VOICE = -7;
    private static final int NOT_A_KEY = -1;
    public static final int SHIFT_LOCKED = 2;
    public static final int SHIFT_OFF = 0;
    public static final int SHIFT_ON = 1;
    private static final String TAG = "LbKbView";
    private int mBaseMiniKbIndex = -1;
    private final int mClickAnimDur;
    private final float mClickedScale;
    private int mColCount;
    private View mCurrentFocusView;
    private boolean mFocusClicked;
    private int mFocusIndex;
    private final float mFocusedScale;
    private final int mInactiveMiniKbAlpha;
    private ImageView[] mKeyImageViews;
    private int mKeyTextColor;
    private int mKeyTextSize;
    private Keyboard mKeyboard;
    private LeanbackKeyboardView.KeyHolder[] mKeys;
    private boolean mMiniKeyboardOnScreen;
    private int mModeChangeTextSize;
    private Rect mPadding;
    private Paint mPaint;
    private int mRowCount;
    private int mShiftState;
    private final int mUnfocusStartDelay;

    public LeanbackKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        TypedArray styledAttrs = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LeanbackKeyboardView, 0, 0);
        this.mRowCount = styledAttrs.getInteger(R.styleable.LeanbackKeyboardView_rowCount, -1);
        this.mColCount = styledAttrs.getInteger(R.styleable.LeanbackKeyboardView_columnCount, -1);
        this.mKeyTextSize = (int) res.getDimension(R.dimen.key_font_size);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTextSize((float) this.mKeyTextSize);
        this.mPaint.setTextAlign(Align.CENTER);
        this.mPaint.setAlpha(255);
        this.mPadding = new Rect(0, 0, 0, 0);
        this.mModeChangeTextSize = (int) res.getDimension(R.dimen.function_key_mode_change_font_size);
        this.mKeyTextColor = res.getColor(R.color.key_text_default);
        this.mFocusIndex = -1;
        this.mShiftState = 0;
        this.mFocusedScale = res.getFraction(R.fraction.focused_scale, 1, 1);
        this.mClickedScale = res.getFraction(R.fraction.clicked_scale, 1, 1);
        this.mClickAnimDur = res.getInteger(R.integer.clicked_anim_duration);
        this.mUnfocusStartDelay = res.getInteger(R.integer.unfocused_anim_delay);
        this.mInactiveMiniKbAlpha = res.getInteger(R.integer.inactive_mini_kb_alpha);
    }

    private CharSequence adjustCase(LeanbackKeyboardView.KeyHolder keyHolder) {
        CharSequence label = keyHolder.key.label;
        CharSequence result = label;
        if (label != null && label.length() < 3) {
            boolean flag;
            if (keyHolder.isInMiniKb && keyHolder.isInvertible) {
                flag = true;
            } else {
                flag = false;
            }

            if (this.mKeyboard.isShifted() ^ flag) {
                result = label.toString().toUpperCase();
            } else {
                result = label.toString().toLowerCase();
            }

            keyHolder.key.label = result;
        }

        return result;
    }

    @TargetApi(16)
    private ImageView createKeyImageView(int keyIndex) {
        Rect var8 = this.mPadding;
        int var2 = this.getPaddingLeft();
        int var3 = this.getPaddingTop();
        LeanbackKeyboardView.KeyHolder var6 = this.mKeys[keyIndex];
        Key var7 = var6.key;
        this.adjustCase(var6);
        String var5;
        if (var7.label == null) {
            var5 = null;
        } else {
            var5 = var7.label.toString();
        }

        if (Log.isLoggable("LbKbView", Log.DEBUG)) {
            Log.d("LbKbView", "LABEL: " + var7.label + "->" + var5);
        }

        Bitmap var9 = Bitmap.createBitmap(var7.width, var7.height, Config.ARGB_8888);
        Canvas var10 = new Canvas(var9);
        Paint var11 = this.mPaint;
        var11.setColor(this.mKeyTextColor);
        var10.drawARGB(0, 0, 0, 0);
        if (var7.icon != null) {
            if (var7.codes[0] == -1) {
                switch (this.mShiftState) {
                    case 0:
                        var7.icon = this.getContext().getResources().getDrawable(R.drawable.ic_ime_shift_off);
                        break;
                    case 1:
                        var7.icon = this.getContext().getResources().getDrawable(R.drawable.ic_ime_shift_on);
                        break;
                    case 2:
                        var7.icon = this.getContext().getResources().getDrawable(R.drawable.ic_ime_shift_lock_on);
                }
            }

            keyIndex = (var7.width - var8.left - var8.right - var7.icon.getIntrinsicWidth()) / 2 + var8.left;
            int var4 = (var7.height - var8.top - var8.bottom - var7.icon.getIntrinsicHeight()) / 2 + var8.top;
            var10.translate((float) keyIndex, (float) var4);
            var7.icon.setBounds(0, 0, var7.icon.getIntrinsicWidth(), var7.icon.getIntrinsicHeight());
            var7.icon.draw(var10);
            var10.translate((float) (-keyIndex), (float) (-var4));
        } else if (var5 != null) {
            if (var5.length() > 1) {
                var11.setTextSize((float) this.mModeChangeTextSize);
                var11.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            } else {
                var11.setTextSize((float) this.mKeyTextSize);
                var11.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            }

            var10.drawText(var5, (float) ((var7.width - var8.left - var8.right) / 2 + var8.left), (float) ((var7.height - var8.top - var8.bottom) /
                    2) + (var11.getTextSize() - var11.descent()) / 2.0F + (float) var8.top, var11);
            var11.setShadowLayer(0.0F, 0.0F, 0.0F, 0);
        }

        ImageView var12 = new ImageView(this.getContext());
        var12.setImageBitmap(var9);
        var12.setContentDescription(var5);
        this.addView(var12, new LayoutParams(-2, -2));
        var12.setX((float) (var7.x + var2));
        var12.setY((float) (var7.y + var3));
        if (this.mMiniKeyboardOnScreen && !var6.isInMiniKb) {
            keyIndex = this.mInactiveMiniKbAlpha;
        } else {
            keyIndex = 255;
        }

        var12.setImageAlpha(keyIndex);
        var12.setVisibility(View.VISIBLE);
        return var12;
    }

    private void createKeyImageViews(LeanbackKeyboardView.KeyHolder[] keys) {
        int var3 = keys.length;
        int var2;
        if (this.mKeyImageViews != null) {
            ImageView[] var5 = this.mKeyImageViews;
            int var4 = var5.length;

            for (var2 = 0; var2 < var4; ++var2) {
                this.removeView(var5[var2]);
            }

            this.mKeyImageViews = null;
        }

        for (var2 = 0; var2 < var3; ++var2) {
            if (this.mKeyImageViews == null) {
                this.mKeyImageViews = new ImageView[var3];
            } else if (this.mKeyImageViews[var2] != null) {
                this.removeView(this.mKeyImageViews[var2]);
            }

            this.mKeyImageViews[var2] = this.createKeyImageView(var2);
        }

    }

    private void removeMessages() {
    }

    private void setKeys(List<Key> keys) {
        this.mKeys = new LeanbackKeyboardView.KeyHolder[keys.size()];
        Iterator var4 = keys.iterator();

        for (int var2 = 0; var2 < this.mKeys.length && var4.hasNext(); ++var2) {
            Key var3 = (Key) var4.next();
            this.mKeys[var2] = new LeanbackKeyboardView.KeyHolder(var3);
        }

    }

    public boolean dismissMiniKeyboard() {
        boolean var1 = false;
        if (this.mMiniKeyboardOnScreen) {
            this.mMiniKeyboardOnScreen = false;
            this.setKeys(this.mKeyboard.getKeys());
            this.invalidateAllKeys();
            var1 = true;
        }

        return var1;
    }

    public int getBaseMiniKbIndex() {
        return this.mBaseMiniKbIndex;
    }

    public int getColCount() {
        return this.mColCount;
    }

    public Key getFocusedKey() {
        return this.mFocusIndex == -1 ? null : this.mKeys[this.mFocusIndex].key;
    }

    public Key getKey(int index) {
        return this.mKeys != null && this.mKeys.length != 0 && index >= 0 && index <= this.mKeys.length ? this.mKeys[index].key : null;
    }

    public Keyboard getKeyboard() {
        return this.mKeyboard;
    }

    public int getNearestIndex(final float x, final float y) {
        int result;
        if (this.mKeys != null && this.mKeys.length != 0) {
            float var3 = (float) this.getPaddingLeft();
            float var4 = (float) this.getPaddingTop();
            float var5 = (float) (this.getMeasuredHeight() - this.getPaddingTop() - this.getPaddingBottom());
            float var6 = (float) (this.getMeasuredWidth() - this.getPaddingLeft() - this.getPaddingRight());
            int var9 = this.getRowCount();
            int var10 = this.getColCount();
            int var8 = (int) ((y - var4) / var5 * (float) var9);
            if (var8 < 0) {
                result = 0;
            } else {
                result = var8;
                if (var8 >= var9) {
                    result = var9 - 1;
                }
            }

            var9 = (int) ((x - var3) / var6 * (float) var10);
            if (var9 < 0) {
                var8 = 0;
            } else {
                var8 = var9;
                if (var9 >= var10) {
                    var8 = var10 - 1;
                }
            }

            var8 += this.mColCount * result;
            result = var8;
            if (var8 > ASCII_PERIOD) {
                result = var8;
                if (var8 < 53) {
                    result = ASCII_PERIOD;
                }
            }

            var8 = result;
            if (result >= 53) {
                var8 = result - 6;
            }

            if (var8 < 0) {
                return 0;
            }

            result = var8;
            if (var8 >= this.mKeys.length) {
                return this.mKeys.length - 1;
            }
        } else {
            result = 0;
        }

        return result;
    }

    public int getRowCount() {
        return this.mRowCount;
    }

    public int getShiftState() {
        return this.mShiftState;
    }

    public void invalidateAllKeys() {
        this.createKeyImageViews(this.mKeys);
    }

    public void invalidateKey(int keyIndex) {
        if (this.mKeys != null && keyIndex >= 0 && keyIndex < this.mKeys.length) {
            if (this.mKeyImageViews[keyIndex] != null) {
                this.removeView(this.mKeyImageViews[keyIndex]);
            }

            this.mKeyImageViews[keyIndex] = this.createKeyImageView(keyIndex);
        }
    }

    public boolean isMiniKeyboardOnScreen() {
        return this.mMiniKeyboardOnScreen;
    }

    public boolean isShifted() {
        return this.mShiftState == 1 || this.mShiftState == 2;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void onKeyLongPress() {
        int var1 = this.mKeys[this.mFocusIndex].key.popupResId;
        if (var1 != 0) {
            this.dismissMiniKeyboard();
            this.mMiniKeyboardOnScreen = true;
            List var6 = (new Keyboard(this.getContext(), var1)).getKeys();
            int var3 = var6.size();
            var1 = this.mFocusIndex;
            int var2 = this.mFocusIndex / this.mColCount;
            int var4 = (this.mFocusIndex + var3) / this.mColCount;
            if (var2 != var4) {
                var1 = this.mColCount * var4 - var3;
            }

            this.mBaseMiniKbIndex = var1;

            for (var2 = 0; var2 < var3; ++var2) {
                Key var7 = (Key) var6.get(var2);
                var7.x = this.mKeys[var1 + var2].key.x;
                var7.y = this.mKeys[var1 + var2].key.y;
                var7.edgeFlags = this.mKeys[var1 + var2].key.edgeFlags;
                this.mKeys[var1 + var2].key = var7;
                this.mKeys[var1 + var2].isInMiniKb = true;
                LeanbackKeyboardView.KeyHolder var8 = this.mKeys[var1 + var2];
                boolean var5;
                if (var2 == 0) {
                    var5 = true;
                } else {
                    var5 = false;
                }

                var8.isInvertible = var5;
            }

            this.invalidateAllKeys();
        }

    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mKeyboard == null) {
            this.setMeasuredDimension(this.getPaddingLeft() + this.getPaddingRight(), this.getPaddingTop() + this.getPaddingBottom());
        } else {
            int var3 = this.mKeyboard.getMinWidth() + this.getPaddingLeft() + this.getPaddingRight();
            heightMeasureSpec = var3;
            if (MeasureSpec.getSize(widthMeasureSpec) < var3 + 10) {
                heightMeasureSpec = MeasureSpec.getSize(widthMeasureSpec);
            }

            this.setMeasuredDimension(heightMeasureSpec, this.mKeyboard.getHeight() + this.getPaddingTop() + this.getPaddingBottom());
        }
    }

    public void setFocus(int row, int col, boolean clicked) {
        this.setFocus(this.mColCount * row + col, clicked);
    }

    public void setFocus(int index, boolean clicked) {
        this.setFocus(index, clicked, true);
    }

    public void setFocus(int index, boolean clicked, boolean showFocusScale) {
        float var4 = 1.0F;
        if (this.mKeyImageViews != null && this.mKeyImageViews.length != 0) {
            int var5;
            label49:
            {
                if (index >= 0) {
                    var5 = index;
                    if (index < this.mKeyImageViews.length) {
                        break label49;
                    }
                }

                var5 = -1;
            }

            if (var5 != this.mFocusIndex || clicked != this.mFocusClicked) {
                if (var5 != this.mFocusIndex) {
                    if (this.mFocusIndex != -1) {
                        LeanbackUtils.sendAccessibilityEvent(this.mKeyImageViews[this.mFocusIndex], false);
                    }

                    if (var5 != -1) {
                        LeanbackUtils.sendAccessibilityEvent(this.mKeyImageViews[var5], true);
                    }
                }

                if (this.mCurrentFocusView != null) {
                    this.mCurrentFocusView.animate().scaleX(1.0F).scaleY(1.0F).setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                            .setStartDelay((long) this.mUnfocusStartDelay);
                    this.mCurrentFocusView.animate().setDuration((long) this.mClickAnimDur).setInterpolator(LeanbackKeyboardContainer
                            .sMovementInterpolator).setStartDelay((long) this.mUnfocusStartDelay);
                }

                if (var5 != -1) {
                    if (clicked) {
                        var4 = this.mClickedScale;
                    } else if (showFocusScale) {
                        var4 = this.mFocusedScale;
                    }

                    this.mCurrentFocusView = this.mKeyImageViews[var5];
                    this.mCurrentFocusView.animate().scaleX(var4).scaleY(var4).setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                            .setDuration((long) this.mClickAnimDur).start();
                }

                this.mFocusIndex = var5;
                this.mFocusClicked = clicked;
                if (-1 != var5 && !this.mKeys[var5].isInMiniKb) {
                    this.dismissMiniKeyboard();
                    return;
                }
            }
        }

    }

    public void setKeyboard(Keyboard keyboard) {
        this.removeMessages();
        this.mKeyboard = keyboard;
        this.setKeys(this.mKeyboard.getKeys());
        int var2 = this.mShiftState;
        this.mShiftState = -1;
        this.setShiftState(var2);
        this.requestLayout();
        this.invalidateAllKeys();
    }

    public void setShiftState(int state) {
        if (this.mShiftState != state) {
            switch (state) {
                case 0:
                    this.mKeyboard.setShifted(false);
                    break;
                case 1:
                case 2:
                    this.mKeyboard.setShifted(true);
            }

            this.mShiftState = state;
            this.invalidateAllKeys();
        }
    }

    private class KeyHolder {
        public boolean isInMiniKb = false;
        public boolean isInvertible = false;
        public Key key;

        public KeyHolder(Key var2) {
            this.key = var2;
        }
    }
}
