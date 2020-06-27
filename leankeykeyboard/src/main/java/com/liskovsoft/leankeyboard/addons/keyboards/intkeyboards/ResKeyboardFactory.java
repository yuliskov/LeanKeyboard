package com.liskovsoft.leankeyboard.addons.keyboards.intkeyboards;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.text.Layout;
import android.util.Log;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardBuilder;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardFactory;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardInfo;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardView;
import com.liskovsoft.leankeyboard.utils.TextDrawable;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResKeyboardFactory implements KeyboardFactory {
    private static final String TAG = ResKeyboardFactory.class.getSimpleName();
    private final Context mContext;
    private Map<String, Drawable> mCachedSpace;

    public ResKeyboardFactory(Context ctx) {
        mContext = ctx;
        mCachedSpace = new HashMap<>();
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
            KeyboardInfo firstKbd = findByLocale(infos);
            result.add(createKeyboard(firstKbd));
            firstKbd.setEnabled(true);
            //ResKeyboardInfo.updateAllKeyboardInfos(mContext, infos);
        }

        return result;
    }

    private KeyboardInfo findByLocale(List<KeyboardInfo> infos) {
        KeyboardInfo defaultKeyboard = infos.get(0);
        Locale defaultLocale = Locale.getDefault();
        String lang = defaultLocale.getLanguage();

        for (final KeyboardInfo info : infos) {
            if (info.getLangCode().startsWith(lang)) {
                defaultKeyboard = info;
                break;
            }
        }

        return defaultKeyboard;
    }

    /**
     * NOTE: create keyboard from xml data
     */
    private KeyboardBuilder createKeyboard(final KeyboardInfo info) {
        return new KeyboardBuilder() {
            private final String langCode = info.getLangCode();

            @Override
            public Keyboard createAbcKeyboard() {
                String prefix = info.isAzerty() ? "azerty_" : "qwerty_";
                int kbResId = mContext.getResources().getIdentifier(prefix + langCode, "xml", mContext.getPackageName());
                Keyboard keyboard = new Keyboard(mContext, kbResId);
                Log.d(TAG, "Creating keyboard... " + info.getLangName());
                return localizeKeys(keyboard, info);
            }

            @Override
            public Keyboard createSymKeyboard() {
                Keyboard keyboard = new Keyboard(mContext, R.xml.sym_en_us);
                return localizeKeys(keyboard, info);
            }

            @Override
            public Keyboard createNumKeyboard() {
                return new Keyboard(mContext, R.xml.number);
            }

            @Override
            public boolean isRtl() {
                return langCode.contains("he") || langCode.contains("ar");
            }
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
        if (mCachedSpace.containsKey(info.getLangCode())) {
            key.icon = mCachedSpace.get(info.getLangCode());
            return;
        }

        TextDrawable drawable = new TextDrawable(mContext, key.icon);
        drawable.setText(info.getLangName());
        drawable.setTextAlign(Layout.Alignment.ALIGN_CENTER);
        //Customize text size and color
        drawable.setTextColor(Color.WHITE);
        drawable.setTextSizeFactor(0.3f);
        drawable.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        key.icon = drawable;

        mCachedSpace.put(info.getLangCode(), drawable);
    }
}
