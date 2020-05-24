package com.liskovsoft.leankeyboard.addons.resize;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardView;
import com.liskovsoft.leankeyboard.utils.LeanKeySettings;
import com.liskovsoft.leankeykeyboard.R;

import java.util.List;

public class ResizeableLeanbackKeyboardView extends LeanbackKeyboardView {
    private final LeanKeySettings mPrefs;
    private final int mKeyTextSizeOrigin;
    private final int mModeChangeTextSizeOrigin;
    private final float mSizeFactor = 1.3f;
    private int mKeyOriginWidth;

    public ResizeableLeanbackKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPrefs = LeanKeySettings.instance(getContext());
        mKeyTextSizeOrigin = mKeyTextSize;
        mModeChangeTextSizeOrigin = mModeChangeTextSize;
    }

    @Override
    public void setKeyboard(Keyboard keyboard) {
        int keySelectorDrawableId;

        if (mPrefs.getEnlargeKeyboard()) {
            keySelectorDrawableId = R.drawable.key_selector_large;
            mKeyTextSize = (int) (mKeyTextSizeOrigin * mSizeFactor);
            mModeChangeTextSize = (int) (mModeChangeTextSizeOrigin * mSizeFactor);

            keyboard = updateKeyboard(keyboard);
        } else {
            keySelectorDrawableId = R.drawable.key_selector;
            mKeyTextSize = mKeyTextSizeOrigin;
            mModeChangeTextSize = mModeChangeTextSizeOrigin;
        }

        getKeySelector().setBackgroundResource(keySelectorDrawableId);
        mPaint.setTextSize(mKeyTextSize);

        super.setKeyboard(keyboard);
    }

    private Keyboard updateKeyboard(Keyboard keyboard) {
        List<Key> keys = keyboard.getKeys();

        if (isNotSizedYet(keys.get(0))) {
            for (Key key : keys) {
                key.width *= mSizeFactor;
                key.height *= mSizeFactor;
                key.gap *= mSizeFactor;
                key.x *= mSizeFactor;
                key.y *= mSizeFactor;
            }
        }

        KeyboardWrapper wrapper = KeyboardWrapper.from(keyboard, getContext());
        wrapper.setHeightFactor(mSizeFactor);
        wrapper.setWidthFactor(mSizeFactor);

        return wrapper;
    }

    private boolean isNotSizedYet(Key key) {
        boolean result = false;

        if (mKeyOriginWidth == 0) {
            mKeyOriginWidth = key.width;
        }

        if (mKeyOriginWidth == key.width) {
            result = true;
        }

        return result;
    }
}
