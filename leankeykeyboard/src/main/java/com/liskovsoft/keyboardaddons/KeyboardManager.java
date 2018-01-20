package com.liskovsoft.keyboardaddons;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.anysoftkeyboard.keyboards.KeyboardFactory;

import java.util.ArrayList;
import java.util.List;

public class KeyboardManager {
    private final Keyboard mEnglishKeyboard;
    private final Context mContext;
    private final List<KeyboardAddOnAndBuilder> mKeyboardBuilders;
    private final List<Keyboard> mAllKeyboards;
    private final KeyboardFactory mKeyboardFactory;
    private int mKeyboardIndex = 0;

    public KeyboardManager(Context ctx, int defaultKeyboard1) {
        this(ctx, new Keyboard(ctx, defaultKeyboard1));
    }

    public KeyboardManager(Context ctx, Keyboard englishKeyboard) {
        mContext = ctx;
        mEnglishKeyboard = englishKeyboard;
        mKeyboardFactory = new KeyboardFactory();

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

    public Keyboard getNextKeyboard() {
        ++mKeyboardIndex;
        mKeyboardIndex = mKeyboardIndex < mAllKeyboards.size() ? mKeyboardIndex : 0;

        Keyboard kbd = mAllKeyboards.get(mKeyboardIndex);
        if (kbd == null) {
            throw new UnsupportedOperationException(String.format("Keyboard %s not initialized", mKeyboardIndex));
        }


        return kbd;
    }
}
