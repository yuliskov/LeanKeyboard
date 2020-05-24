package com.liskovsoft.leankeyboard.addons.keyboards.intkeyboards;

import android.content.Context;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardInfo;

import java.util.ArrayList;
import java.util.List;

public class KeyboardInfoAdapter implements CheckedSource {
    private final Context mContext;
    private final List<KeyboardInfo> mInfos;

    public KeyboardInfoAdapter(Context context) {
        mContext = context;
        mInfos = ResKeyboardInfo.getAllKeyboardInfos(context);
    }

    @Override
    public List<CheckedItem> getItems() {
        List<CheckedItem> result = new ArrayList<>();

        int counter = 99;

        for (KeyboardInfo info : mInfos) {
            int finalCounter = counter++;

            CheckedItem item = new CheckedItem() {
                private final KeyboardInfo mInfo = info;
                private final int mCounter = finalCounter;

                @Override
                public long getId() {
                    return mCounter;
                }

                @Override
                public String getTitle() {
                    return mInfo.getLangName();
                }

                @Override
                public void onClick(boolean checked) {
                    if (mInfo.isEnabled() == checked) {
                        return;
                    }

                    mInfo.setEnabled(checked);
                    ResKeyboardInfo.updateAllKeyboardInfos(mContext, mInfos);
                }

                @Override
                public boolean getChecked() {
                    return mInfo.isEnabled();
                }
            };

            result.add(item);
        }

        return result;
    }
}
