package com.liskovsoft.keyboardaddons;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import com.liskovsoft.keyboardaddons.apklangfactory.keyboards.KeyboardAddOnAndBuilder;
import com.liskovsoft.keyboardaddons.apklangfactory.keyboards.ApkLangKeyboardFactory;

import java.util.ArrayList;
import java.util.List;

public class KeyboardManager {
    private final Keyboard mEnglishKeyboard;
    private final Context mContext;
    private final List<KeyboardAddOnAndBuilder> mKeyboardBuilders;
    private final List<Keyboard> mAllKeyboards;
    private final ApkLangKeyboardFactory mKeyboardFactory;
    private int mKeyboardIndex = 0;

    public KeyboardManager(Context ctx, int keyboardResId) {
        this(ctx, new Keyboard(ctx, keyboardResId));
    }

    public KeyboardManager(Context ctx, Keyboard englishKeyboard) {
        mContext = ctx;
        mEnglishKeyboard = englishKeyboard;
        mKeyboardFactory = new ApkLangKeyboardFactory();

        mKeyboardBuilders = mKeyboardFactory.getAllAvailableKeyboards(mContext);
        mAllKeyboards = buildAllKeyboards();
    }

    private List<Keyboard> buildAllKeyboards() {
        List<Keyboard> keyboards = new ArrayList<>();
        keyboards.add(mEnglishKeyboard);
        if (!mKeyboardBuilders.isEmpty()) {
            for (KeyboardAddOnAndBuilder builder : mKeyboardBuilders) {
                keyboards.add(builder.createKeyboard());
            }
        }
        return keyboards;
    }

    /**
     * Get next keyboard from internal source (looped)
     * @return keyboard
     */
    public Keyboard getNextKeyboard() {
        ++mKeyboardIndex;
        mKeyboardIndex = mKeyboardIndex < mAllKeyboards.size() ? mKeyboardIndex : 0;

        Keyboard kbd = mAllKeyboards.get(mKeyboardIndex);
        if (kbd == null) {
            throw new IllegalStateException(String.format("Keyboard %s not initialized", mKeyboardIndex));
        }

        return kbd;
    }
}
