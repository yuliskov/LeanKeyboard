package com.liskovsoft.keyboardaddons.reskbdfactory;

import android.content.Context;
import android.content.res.Resources;
import android.inputmethodservice.Keyboard;
import android.support.annotation.Nullable;
import com.liskovsoft.keyboardaddons.KeyboardBuilder;
import com.liskovsoft.keyboardaddons.KeyboardFactory;
import com.liskovsoft.keyboardaddons.KeyboardInfo;

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

        List<KeyboardInfo> infos = ResKeyboardInfo.getAllKeyboardInfos(context);
        final Resources resources = mContext.getResources();

        for (final KeyboardInfo info : infos) {
            if (!info.isEnabled()) {
                continue;
            }

            result.add(new KeyboardBuilder() {
                @Nullable
                @Override
                public Keyboard createKeyboard() {
                    return new Keyboard(mContext, resources.getIdentifier("qwerty_" + info.getLangCode(), "xml", mContext.getPackageName()));
                }
            });
        }
        return result;
    }

    @Override
    public boolean needUpdate() {
        return ResKeyboardInfo.needUpdate();
    }
}
