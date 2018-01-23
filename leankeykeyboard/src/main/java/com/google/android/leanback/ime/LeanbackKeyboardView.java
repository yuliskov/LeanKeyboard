package com.google.android.leanback.ime;

import android.annotation.SuppressLint;
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
        mRowCount = styledAttrs.getInteger(R.styleable.LeanbackKeyboardView_rowCount, -1);
        mColCount = styledAttrs.getInteger(R.styleable.LeanbackKeyboardView_columnCount, -1);
        mKeyTextSize = (int) res.getDimension(R.dimen.key_font_size);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize((float) mKeyTextSize);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);
        mPadding = new Rect(0, 0, 0, 0);
        mModeChangeTextSize = (int) res.getDimension(R.dimen.function_key_mode_change_font_size);
        mKeyTextColor = res.getColor(R.color.key_text_default);
        mFocusIndex = -1;
        mShiftState = 0;
        mFocusedScale = res.getFraction(R.fraction.focused_scale, 1, 1);
        mClickedScale = res.getFraction(R.fraction.clicked_scale, 1, 1);
        mClickAnimDur = res.getInteger(R.integer.clicked_anim_duration);
        mUnfocusStartDelay = res.getInteger(R.integer.unfocused_anim_delay);
        mInactiveMiniKbAlpha = res.getInteger(R.integer.inactive_mini_kb_alpha);
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

    @SuppressLint("NewApi")
    private ImageView createKeyImageView(final int keyIndex) {
        Rect padding = mPadding;
        int kbdPaddingLeft = getPaddingLeft();
        int kbdPaddingTop = getPaddingTop();
        LeanbackKeyboardView.KeyHolder keyHolder = mKeys[keyIndex];
        Key key = keyHolder.key;
        adjustCase(keyHolder);
        String label;
        if (key.label == null) {
            label = null;
        } else {
            label = key.label.toString();
        }

        if (Log.isLoggable("LbKbView", Log.DEBUG)) {
            Log.d("LbKbView", "LABEL: " + key.label + "->" + label);
        }

        Bitmap bitmap = Bitmap.createBitmap(key.width, key.height, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = mPaint;
        paint.setColor(mKeyTextColor);
        canvas.drawARGB(0, 0, 0, 0);
        if (key.icon != null) {
            if (key.codes[0] == -1) {
                switch (mShiftState) {
                    case 0:
                        key.icon = getContext().getResources().getDrawable(R.drawable.ic_ime_shift_off);
                        break;
                    case 1:
                        key.icon = getContext().getResources().getDrawable(R.drawable.ic_ime_shift_on);
                        break;
                    case 2:
                        key.icon = getContext().getResources().getDrawable(R.drawable.ic_ime_shift_lock_on);
                }
            }

            int dx = (key.width - padding.left - padding.right - key.icon.getIntrinsicWidth()) / 2 + padding.left;
            int dy = (key.height - padding.top - padding.bottom - key.icon.getIntrinsicHeight()) / 2 + padding.top;
            canvas.translate((float) dx, (float) dy);
            key.icon.setBounds(0, 0, key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
            key.icon.draw(canvas);
            canvas.translate((float) (-dx), (float) (-dy));
        } else if (label != null) {
            if (label.length() > 1) {
                paint.setTextSize((float) mModeChangeTextSize);
                paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            } else {
                paint.setTextSize((float) mKeyTextSize);
                paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            }

            canvas.drawText(label, (float) ((key.width - padding.left - padding.right) / 2 + padding.left), (float) ((key.height - padding.top - padding.bottom) /
                    2) + (paint.getTextSize() - paint.descent()) / 2.0F + (float) padding.top, paint);
            paint.setShadowLayer(0.0F, 0.0F, 0.0F, 0);
        }

        ImageView image = new ImageView(getContext());
        image.setImageBitmap(bitmap);
        image.setContentDescription(label);
        addView(image, new LayoutParams(-2, -2));
        image.setX((float) (key.x + kbdPaddingLeft));
        image.setY((float) (key.y + kbdPaddingTop));
        int opacity;
        if (mMiniKeyboardOnScreen && !keyHolder.isInMiniKb) {
            opacity = mInactiveMiniKbAlpha;
        } else {
            opacity = 255;
        }

        image.setImageAlpha(opacity);
        image.setVisibility(View.VISIBLE);
        return image;
    }

    private void createKeyImageViews(LeanbackKeyboardView.KeyHolder[] keys) {
        if (mKeyImageViews != null) {
            ImageView[] images = mKeyImageViews;
            int totalImages = images.length;

            for (int i = 0; i < totalImages; ++i) {
                removeView(images[i]);
            }

            mKeyImageViews = null;
        }

        int totalKeys = keys.length;
        for (int i = 0; i < totalKeys; ++i) {
            if (mKeyImageViews == null) {
                mKeyImageViews = new ImageView[totalKeys];
            } else if (mKeyImageViews[i] != null) {
                removeView(mKeyImageViews[i]);
            }

            mKeyImageViews[i] = createKeyImageView(i);
        }

    }

    private void removeMessages() {
        throw new IllegalStateException("method not implemented");
    }

    private void setKeys(List<Key> keys) {
        mKeys = new LeanbackKeyboardView.KeyHolder[keys.size()];
        Iterator iterator = keys.iterator();

        for (int i = 0; i < mKeys.length && iterator.hasNext(); ++i) {
            Key key = (Key) iterator.next();
            mKeys[i] = new LeanbackKeyboardView.KeyHolder(key);
        }

    }

    public boolean dismissMiniKeyboard() {
        boolean dismiss = false;
        if (mMiniKeyboardOnScreen) {
            mMiniKeyboardOnScreen = false;
            setKeys(mKeyboard.getKeys());
            invalidateAllKeys();
            dismiss = true;
        }

        return dismiss;
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
        if (mKeys != null && mKeys.length != 0) {
            float paddingLeft = (float) getPaddingLeft();
            float paddingTop = (float) getPaddingTop();
            float height = (float) (getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
            float width = (float) (getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
            int rows = getRowCount();
            int cols = getColCount();
            int index = (int) ((y - paddingTop) / height * (float) rows);
            if (index < 0) {
                result = 0;
            } else {
                result = index;
                if (index >= rows) {
                    result = rows - 1;
                }
            }

            rows = (int) ((x - paddingLeft) / width * (float) cols);
            if (rows < 0) {
                index = 0;
            } else {
                index = rows;
                if (rows >= cols) {
                    index = cols - 1;
                }
            }

            index += mColCount * result;
            result = index;
            if (index > ASCII_PERIOD) {
                result = index;
                if (index < 53) {
                    result = ASCII_PERIOD;
                }
            }

            index = result;
            if (result >= 53) {
                index = result - 6;
            }

            if (index < 0) {
                return 0;
            }

            result = index;
            if (index >= mKeys.length) {
                return mKeys.length - 1;
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
        if (mKeys != null && keyIndex >= 0 && keyIndex < mKeys.length) {
            if (mKeyImageViews[keyIndex] != null) {
                removeView(mKeyImageViews[keyIndex]);
            }

            mKeyImageViews[keyIndex] = createKeyImageView(keyIndex);
        }
    }

    public boolean isMiniKeyboardOnScreen() {
        return mMiniKeyboardOnScreen;
    }

    public boolean isShifted() {
        return mShiftState == 1 || mShiftState == 2;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void onKeyLongPress() {
        int popupResId = mKeys[mFocusIndex].key.popupResId;
        if (popupResId != 0) {
            dismissMiniKeyboard();
            mMiniKeyboardOnScreen = true;
            List<Key> accentKeys = (new Keyboard(getContext(), popupResId)).getKeys();
            int totalAccentKeys = accentKeys.size();
            int baseIndex = mFocusIndex;
            int currentRow = mFocusIndex / mColCount;
            int nextRow = (mFocusIndex + totalAccentKeys) / mColCount;
            if (currentRow != nextRow) {
                baseIndex = mColCount * nextRow - totalAccentKeys;
            }

            mBaseMiniKbIndex = baseIndex;

            for (int i = 0; i < totalAccentKeys; ++i) {
                Key accentKey = accentKeys.get(i);
                accentKey.x = mKeys[baseIndex + i].key.x;
                accentKey.y = mKeys[baseIndex + i].key.y;
                accentKey.edgeFlags = mKeys[baseIndex + i].key.edgeFlags;
                mKeys[baseIndex + i].key = accentKey;
                mKeys[baseIndex + i].isInMiniKb = true;
                LeanbackKeyboardView.KeyHolder holder = mKeys[baseIndex + i];
                boolean invertible;
                if (i == 0) {
                    invertible = true;
                } else {
                    invertible = false;
                }

                holder.isInvertible = invertible;
            }

            invalidateAllKeys();
        }

    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mKeyboard == null) {
            setMeasuredDimension(getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom());
        } else {
            int calcHeight = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
            heightMeasureSpec = calcHeight;
            if (MeasureSpec.getSize(widthMeasureSpec) < calcHeight + 10) {
                heightMeasureSpec = MeasureSpec.getSize(widthMeasureSpec);
            }

            setMeasuredDimension(heightMeasureSpec, mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom());
        }
    }

    public void setFocus(int row, int col, boolean clicked) {
        setFocus(mColCount * row + col, clicked);
    }

    public void setFocus(int index, boolean clicked) {
        setFocus(index, clicked, true);
    }

    /**
     * Move focus to the key specified by index
     * @param index index of the key
     * @param clicked key state
     * @param showFocusScale increase size
     */
    public void setFocus(final int index, final boolean clicked, final boolean showFocusScale) {
        float scale = 1.0F;
        if (mKeyImageViews != null && mKeyImageViews.length != 0) {
            int calcIndex;

            if (index >= 0 && index < mKeyImageViews.length) {
                calcIndex = index;
            } else {
                calcIndex = -1;
            }

            if (calcIndex != mFocusIndex || clicked != mFocusClicked) {
                if (calcIndex != mFocusIndex) {
                    if (mFocusIndex != -1) {
                        LeanbackUtils.sendAccessibilityEvent(mKeyImageViews[mFocusIndex], false);
                    }

                    if (calcIndex != -1) {
                        LeanbackUtils.sendAccessibilityEvent(mKeyImageViews[calcIndex], true);
                    }
                }

                if (mCurrentFocusView != null) {
                    mCurrentFocusView.animate()
                                    .scaleX(1.0F)
                                    .scaleY(1.0F)
                                    .setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                                    .setStartDelay((long) mUnfocusStartDelay);
                    mCurrentFocusView.animate()
                                .setDuration((long) mClickAnimDur)
                                .setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                                .setStartDelay((long) mUnfocusStartDelay);
                }

                if (calcIndex != -1) {
                    if (clicked) {
                        scale = mClickedScale;
                    } else if (showFocusScale) {
                        scale = mFocusedScale;
                    }

                    mCurrentFocusView = mKeyImageViews[calcIndex];
                    mCurrentFocusView.animate().scaleX(scale).scaleY(scale).setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                            .setDuration((long) mClickAnimDur).start();
                }

                mFocusIndex = calcIndex;
                mFocusClicked = clicked;
                if (-1 != calcIndex && !mKeys[calcIndex].isInMiniKb) {
                    dismissMiniKeyboard();
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

    /**
     * Set keyboard shift sate
     * @param state one of the
     * {@link LeanbackKeyboardView#SHIFT_ON SHIFT_ON},
     * {@link LeanbackKeyboardView#SHIFT_OFF SHIFT_OFF},
     * {@link LeanbackKeyboardView#SHIFT_LOCKED SHIFT_LOCKED}
     * constants
     */
    public void setShiftState(int state) {
        if (mShiftState != state) {
            switch (state) {
                case SHIFT_OFF:
                    mKeyboard.setShifted(false);
                    break;
                case SHIFT_ON:
                case SHIFT_LOCKED:
                    mKeyboard.setShifted(true);
            }

            mShiftState = state;
            invalidateAllKeys();
        }
    }

    private class KeyHolder {
        public boolean isInMiniKb = false;
        public boolean isInvertible = false;
        public Key key;

        public KeyHolder(Key key) {
            this.key = key;
        }
    }
}
