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
    private List<KeyboardData> mAllKeyboards;
    private final KeyboardFactory mKeyboardFactory;
    private int mKeyboardIndex = 0;

    public static class KeyboardData {
        public Keyboard abcKeyboard;
        public Keyboard symKeyboard;
        public Keyboard numKeyboard;
    }

    public KeyboardManager(Context ctx) {
        mContext = ctx;
        mStateManager = new KeyboardStateManager(mContext, this);
        mKeyboardFactory = new ResKeyboardFactory(mContext);
        mStateManager.restore();
    }

    public void load() {
        mKeyboardBuilders = mKeyboardFactory.getAllAvailableKeyboards(mContext);
        mAllKeyboards = buildAllKeyboards();
    }

    private List<KeyboardData> buildAllKeyboards() {
        List<KeyboardData> keyboards = new ArrayList<>();
        if (!mKeyboardBuilders.isEmpty()) {
            for (KeyboardBuilder builder : mKeyboardBuilders) {
                KeyboardData data = new KeyboardData();
                data.abcKeyboard = builder.createAbcKeyboard();
                data.symKeyboard = builder.createSymKeyboard();
                data.numKeyboard = builder.createNumKeyboard();

                keyboards.add(data);
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
     * Get next keyboard from internal source (looped)
     */
    public KeyboardData next() {
        if (mKeyboardFactory.needUpdate() || mAllKeyboards == null) {
            load();
        }

        ++mKeyboardIndex;

        mKeyboardIndex = mKeyboardIndex < mAllKeyboards.size() ? mKeyboardIndex : 0;

        KeyboardData kbd = mAllKeyboards.get(mKeyboardIndex);

        if (kbd == null) {
            throw new IllegalStateException(String.format("Keyboard %s not initialized", mKeyboardIndex));
        }

        onNextKeyboard();

        return kbd;
    }

    public int getIndex() {
        return mKeyboardIndex;
    }

    public void setIndex(int idx) {
        mKeyboardIndex = idx;
    }

    /**
     * Get current keyboard
     */
    public KeyboardData get() {
        if (mAllKeyboards == null) {
            load();
        }

        return mAllKeyboards.get(mKeyboardIndex);
    }
}
