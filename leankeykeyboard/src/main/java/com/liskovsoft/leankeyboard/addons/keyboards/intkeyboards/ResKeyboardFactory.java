package com.liskovsoft.leankeyboard.addons.keyboards.intkeyboards;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardBuilder;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardFactory;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardInfo;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardView;
import com.liskovsoft.leankeyboard.utils.TextDrawable;

import java.util.ArrayList;
import java.util.List;

public class ResKeyboardFactory implements KeyboardFactory {
    private static final String TAG = ResKeyboardFactory.class.getSimpleName();
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
            Log.d(TAG, "Creating keyboard... " + info.getLangName());
            return localizeKeys(keyboard, info);
        };
    }

    @Override
    public boolean needUpdate() {
        return ResKeyboardInfo.needUpdate();
    }

    private Keyboard localizeKeys(Keyboard keyboard, KeyboardInfo info) {
        List<Key> keys = keyboard.getKeys();

        for (Key key : keys) {
            if (key.codes[0] == LeanbackKeyboardView.ASCII_SPACE) {
                localizeSpace(key, info);
                break;
            }
        }

        return keyboard;
    }

    private void localizeSpace(Key key, KeyboardInfo info) {
        TextDrawable drawable = new TextDrawable(mContext, key.icon);
        drawable.setText(info.getLangName());
        drawable.setTextAlign(Layout.Alignment.ALIGN_CENTER);
        //Customize text size and color
        drawable.setTextColor(Color.WHITE);
        drawable.setTextSizeFactor(0.3f);
        drawable.setTypeface(Typeface.SANS_SERIF);
        key.icon = drawable;
    }
}
