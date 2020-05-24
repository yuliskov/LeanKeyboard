package com.liskovsoft.leankeyboard.addons.keyboards.intkeyboards;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.text.Layout;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardBuilder;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardFactory;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardInfo;
import com.liskovsoft.leankeyboard.helpers.Helpers;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardView;
import com.liskovsoft.leankeyboard.utils.TextDrawable;

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
            Keyboard keyboard = new Keyboard(mContext, kbResId);
            return keyboard;
        };
    }

    @Override
    public boolean needUpdate() {
        return ResKeyboardInfo.needUpdate();
    }

    private Keyboard localizeSpaceKey(Keyboard keyboard) {
        List<Key> keys = keyboard.getKeys();

        for (Key key : keys) {
            if (key.codes[0] == LeanbackKeyboardView.ASCII_SPACE) {
                //key.icon = Helpers.writeTextCentered(mContext, key.icon, "Hello World!", 18);
                TextDrawable drawable = new TextDrawable(mContext, key.icon);
                drawable.setText("TEXT DRAWN IN A CIRCLE");
                drawable.setTextAlign(Layout.Alignment.ALIGN_CENTER);
                //Customize text size and color
                drawable.setTextColor(Color.WHITE);
                drawable.setTextSize(12);
                key.icon = drawable;
            }
        }

        return keyboard;
    }
}
