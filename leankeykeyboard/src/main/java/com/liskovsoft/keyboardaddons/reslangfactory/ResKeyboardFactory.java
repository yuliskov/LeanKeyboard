package com.liskovsoft.keyboardaddons.reslangfactory;

import android.content.Context;
import android.content.res.Resources;
import android.inputmethodservice.Keyboard;
import android.support.annotation.Nullable;
import com.liskovsoft.keyboardaddons.KeyboardBuilder;
import com.liskovsoft.keyboardaddons.KeyboardFactory;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.List;

public class ResKeyboardFactory implements KeyboardFactory {
    private final Context mContext;

    public ResKeyboardFactory(Context ctx) {
        mContext = ctx;
    }

    @Override
    public List<? extends KeyboardBuilder> getAllAvailableKeyboards(Context context) {
        List<KeyboardBuilder> result = new ArrayList<>();
        String[] langs = mContext.getResources().getStringArray(R.array.additional_languages);
        final Resources resources = mContext.getResources();
        for (final String langPair : langs) {
            final String langCode = langPair.split("\\|")[1];
            result.add(new KeyboardBuilder() {
                @Nullable
                @Override
                public Keyboard createKeyboard() {
                    return new Keyboard(mContext, resources.getIdentifier("qwerty_" + langCode, "xml", mContext.getPackageName()));
                }
            });
        }
        return result;
    }
}
