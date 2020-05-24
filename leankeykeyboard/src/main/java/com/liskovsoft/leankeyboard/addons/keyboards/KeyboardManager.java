package com.liskovsoft.leankeyboard.addons.keyboards;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import com.liskovsoft.leankeyboard.addons.keyboards.intkeyboards.ResKeyboardFactory;

import java.util.ArrayList;
import java.util.List;

public class KeyboardManager {
    private final Context mContext;
    private final KeyboardStateManager mStateManager;
    private List<? extends KeyboardBuilder> mKeyboardBuilders;
    private List<Keyboard> mAllKeyboards;
    private KeyboardFactory mKeyboardFactory;

    private int mKeyboardIndex = 0;

    public KeyboardManager(Context ctx) {
        mContext = ctx;
        mStateManager = new KeyboardStateManager(mContext, this);
        mStateManager.restore();
        init();
    }

    private void init() {
        mKeyboardFactory = new ResKeyboardFactory(mContext);
        mKeyboardBuilders = mKeyboardFactory.getAllAvailableKeyboards(mContext);
        mAllKeyboards = buildAllKeyboards();
    }

    private List<Keyboard> buildAllKeyboards() {
        List<Keyboard> keyboards = new ArrayList<>();
        if (!mKeyboardBuilders.isEmpty()) {
            for (KeyboardBuilder builder : mKeyboardBuilders) {
                keyboards.add(builder.createKeyboard());
            }
        }
        return keyboards;
    }

    /**
     * Performs callback to event handlers
     */
    private void onNextKeyboard() {
        mStateManager.onNextKeyboard();
    }

    /**
     * NOTE: Get next keyboard from internal source (looped)
     * @return keyboard
     */
    public Keyboard getNextKeyboard() {
        if (mKeyboardFactory.needUpdate()) {
            init();
        }

        mKeyboardIndex = mKeyboardIndex < mAllKeyboards.size() ? mKeyboardIndex : 0;

        Keyboard kbd = mAllKeyboards.get(mKeyboardIndex);
        if (kbd == null) {
            throw new IllegalStateException(String.format("Keyboard %s not initialized", mKeyboardIndex));
        }

        onNextKeyboard();

        ++mKeyboardIndex;

        return kbd;
    }

    public int getKeyboardIndex() {
        return mKeyboardIndex;
    }

    public void setKeyboardIndex(int idx) {
        mKeyboardIndex = idx;
    }
}
