package com.liskovsoft.leankeyboard.keyboard.android.leanback.ime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import com.liskovsoft.leankeykeyboard.R;

import java.util.Iterator;
import java.util.List;

public class LeanbackKeyboardView extends FrameLayout {
    /**
     * space key index (important: wrong value will broke navigation)
     */
    public static final int ASCII_PERIOD = 47;
    /**
     * keys count among which space key spans (important: wrong value will broke navigation)
     */
    public static final int ASCII_PERIOD_LEN = 5;
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
    public static final int KEYCODE_LANG_TOGGLE = -9;
    public static final int KEYCODE_CLIPBOARD = -10;
    public static final int NOT_A_KEY = -1;
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
    private KeyHolder[] mKeys;
    private boolean mMiniKeyboardOnScreen;
    private int mModeChangeTextSize;
    private Rect mPadding;
    private Paint mPaint;
    private int mRowCount;
    private int mShiftState;
    private final int mUnfocusStartDelay;
    private final KeyConverter mConverter;

    private class KeyConverter {
        private static final int LOWER_CASE = 0;
        private static final int UPPER_CASE = 1;

        private void init(KeyHolder keyHolder) {
            // store original label
            // in case when two characters are stored in one label (e.g. "A|B")
            if (keyHolder.key.text == null) {
                keyHolder.key.text = keyHolder.key.label;
            }
        }

        public void toLowerCase(KeyHolder keyHolder) {
            extractChar(LOWER_CASE, keyHolder);
        }

        public void toUpperCase(KeyHolder keyHolder) {
            extractChar(UPPER_CASE, keyHolder);
        }

        private void extractChar(int charCase, KeyHolder keyHolder) {
            init(keyHolder);

            CharSequence result = null;
            CharSequence label = keyHolder.key.text;

            String[] labels = splitLabels(label);

            switch (charCase) {
                case LOWER_CASE:
                    result = labels != null ? labels[0] : label.toString().toLowerCase();
                    break;
                case UPPER_CASE:
                    result = labels != null ? labels[1] : label.toString().toUpperCase();
                    break;
            }

            keyHolder.key.label = result;
        }

        private String[] splitLabels(CharSequence label) {
            String realLabel = label.toString();

            String[] labels = realLabel.split("\\|");

            return labels.length == 2 ? labels : null; // remember, we encoding two chars
        }
    }

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
        mKeyTextColor = ContextCompat.getColor(getContext(), R.color.key_text_default);
        mFocusIndex = -1;
        mShiftState = 0;
        mFocusedScale = res.getFraction(R.fraction.focused_scale, 1, 1);
        mClickedScale = res.getFraction(R.fraction.clicked_scale, 1, 1);
        mClickAnimDur = res.getInteger(R.integer.clicked_anim_duration);
        mUnfocusStartDelay = res.getInteger(R.integer.unfocused_anim_delay);
        mInactiveMiniKbAlpha = res.getInteger(R.integer.inactive_mini_kb_alpha);
        mConverter = new KeyConverter();
    }

    private void adjustCase(KeyHolder keyHolder) {
        boolean flag;

        if (keyHolder.isInMiniKb && keyHolder.isInvertible) {
            flag = true;
        } else {
            flag = false;
        }

        // ^ equals to !=
        if (mKeyboard.isShifted() ^ flag) {
            mConverter.toUpperCase(keyHolder);
        } else {
            mConverter.toLowerCase(keyHolder);
        }
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
            if (key.codes[0] == NOT_A_KEY) {
                switch (mShiftState) {
                    case SHIFT_OFF:
                        key.icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_ime_shift_off);
                        break;
                    case SHIFT_ON:
                        key.icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_ime_shift_on);
                        break;
                    case SHIFT_LOCKED:
                        key.icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_ime_shift_lock_on);
                }
            }

            int iconWidth = key.icon.getIntrinsicWidth();
            int iconHeight = key.icon.getIntrinsicHeight();

            if (key.width > key.height) { // wide key fix (space key)
                iconWidth = key.width;
            }

            int dx = (key.width - padding.left - padding.right - iconWidth) / 2 + padding.left;
            int dy = (key.height - padding.top - padding.bottom - iconHeight) / 2 + padding.top;
            canvas.translate((float) dx, (float) dy);
            key.icon.setBounds(0, 0, iconWidth, iconHeight);
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

    private void createKeyImageViews(KeyHolder[] keys) {
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
        // TODO: not implemented
        Log.w(TAG, "method 'removeMessages()' not implemented");
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
        return mBaseMiniKbIndex;
    }

    public int getColCount() {
        return mColCount;
    }

    public Key getFocusedKey() {
        return mFocusIndex == -1 ? null : mKeys[mFocusIndex].key;
    }

    public Key getKey(int index) {
        return mKeys != null && mKeys.length != 0 && index >= 0 && index <= mKeys.length ? mKeys[index].key : null;
    }

    public Keyboard getKeyboard() {
        return mKeyboard;
    }

    /**
     * Get index of the key under cursor
     * <br/>
     * Resulted index depends on the space key position
     * @param x x position
     * @param y y position
     * @return index of the key
     */
    public int getNearestIndex(final float x, final float y) {
        int result;
        if (mKeys != null && mKeys.length != 0) {
            float paddingLeft = (float) getPaddingLeft();
            float paddingTop = (float) getPaddingTop();
            float kbHeight = (float) (getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
            float kbWidth = (float) (getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
            final int rows = getRowCount();
            final int cols = getColCount();
            final int indexVert = (int) ((y - paddingTop) / kbHeight * (float) rows);
            if (indexVert < 0) {
                result = 0;
            } else {
                result = indexVert;
                if (indexVert >= rows) {
                    result = rows - 1;
                }
            }

            final int indexHoriz = (int) ((x - paddingLeft) / kbWidth * (float) cols);
            int indexFull;
            if (indexHoriz < 0) {
                indexFull = 0;
            } else {
                indexFull = indexHoriz;
                if (indexHoriz >= cols) {
                    indexFull = cols - 1;
                }
            }

            indexFull += mColCount * result;
            result = indexFull;
            if (indexFull > ASCII_PERIOD) { // key goes beyond space
                result = indexFull;
                if (indexFull < (ASCII_PERIOD + ASCII_PERIOD_LEN)) {  // key stays within space boundary
                    result = ASCII_PERIOD;
                }
            }

            indexFull = result;
            if (result >= (ASCII_PERIOD + ASCII_PERIOD_LEN)) { // is key position after space?
                indexFull = result - ASCII_PERIOD_LEN + 1;
            }

            if (indexFull < 0) {
                return 0;
            }

            result = indexFull;
            if (indexFull >= mKeys.length) {
                return mKeys.length - 1;
            }
        } else {
            result = 0;
        }

        return result;
    }

    public int getRowCount() {
        return mRowCount;
    }

    public int getShiftState() {
        return mShiftState;
    }

    public void invalidateAllKeys() {
        createKeyImageViews(mKeys);
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
        return mShiftState == SHIFT_ON || mShiftState == SHIFT_LOCKED;
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
                KeyHolder holder = mKeys[baseIndex + i];
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
            int heightFull = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
            heightMeasureSpec = heightFull;
            if (MeasureSpec.getSize(widthMeasureSpec) < heightFull + 10) {
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
            int indexFull;

            if (index >= 0 && index < mKeyImageViews.length) {
                indexFull = index;
            } else {
                indexFull = -1;
            }

            if (indexFull != mFocusIndex || clicked != mFocusClicked) {
                if (indexFull != mFocusIndex) {
                    if (mFocusIndex != -1) {
                        LeanbackUtils.sendAccessibilityEvent(mKeyImageViews[mFocusIndex], false);
                    }

                    if (indexFull != -1) {
                        LeanbackUtils.sendAccessibilityEvent(mKeyImageViews[indexFull], true);
                    }
                }

                if (mCurrentFocusView != null) {
                    mCurrentFocusView.animate()
                                     .scaleX(1.0F)
                                     .scaleY(1.0F)
                                     .setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                                     .setStartDelay(mUnfocusStartDelay);

                    mCurrentFocusView.animate()
                                     .setDuration(mClickAnimDur)
                                     .setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                                     .setStartDelay(mUnfocusStartDelay);
                }

                if (indexFull != -1) {
                    if (clicked) {
                        scale = mClickedScale;
                    } else if (showFocusScale) {
                        scale = mFocusedScale;
                    }

                    mCurrentFocusView = mKeyImageViews[indexFull];
                    mCurrentFocusView.animate()
                                     .scaleX(scale)
                                     .scaleY(scale)
                                     .setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                                     .setDuration((long) mClickAnimDur)
                                     .start();
                }

                mFocusIndex = indexFull;
                mFocusClicked = clicked;
                if (-1 != indexFull && !mKeys[indexFull].isInMiniKb) {
                    dismissMiniKeyboard();
                }
            }
        }

    }

    public void setKeyboard(Keyboard keyboard) {
        removeMessages();
        mKeyboard = keyboard;
        setKeys(mKeyboard.getKeys());
        int state = mShiftState;
        mShiftState = -1;
        setShiftState(state);
        requestLayout();
        invalidateAllKeys();
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
