package com.liskovsoft.leankeyboard.addons.reskbdfactory;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import com.liskovsoft.leankeyboard.addons.KeyboardBuilder;
import com.liskovsoft.leankeyboard.addons.KeyboardFactory;
import com.liskovsoft.leankeyboard.addons.KeyboardInfo;

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

        for (final KeyboardInfo info : infos) {
            if (info.isEnabled()) {
                result.add(createKeyboard(info));
            }
        }

        // at least one kbd should be enabled
        if (result.isEmpty()) {
            KeyboardInfo firstKbd = infos.get(0);
            result.add(createKeyboard(firstKbd));
            firstKbd.setEnabled(true);
            ResKeyboardInfo.updateAllKeyboardInfos(mContext, infos);
        }

        return result;
    }

    /**
     * NOTE: create keyboard from xml data
     */
    private KeyboardBuilder createKeyboard(final KeyboardInfo info) {
        return () -> {
            String prefix = info.isAzerty() ? "azerty_" : "qwerty_";
            int kbResId = mContext.getResources().getIdentifier(prefix + info.getLangCode(), "xml", mContext.getPackageName());
            return new Keyboard(mContext, kbResId);
        };
    }

    @Override
    public boolean needUpdate() {
        return ResKeyboardInfo.needUpdate();
    }
}
