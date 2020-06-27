/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liskovsoft.leankeyboard.addons.keyboards.extkeyboards.keyboards;

import android.content.Context;

import android.inputmethodservice.Keyboard;
import androidx.annotation.Nullable;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardBuilder;
import com.liskovsoft.leankeyboard.addons.keyboards.extkeyboards.addons.AddOn;
import com.liskovsoft.leankeyboard.addons.keyboards.extkeyboards.addons.AddOnImpl;
import com.liskovsoft.leankeykeyboard.R;

public class ApkKeyboardAddOnAndBuilder extends AddOnImpl implements KeyboardBuilder {

    public static final String KEYBOARD_PREF_PREFIX = "keyboard_";

    private final int mResId;
    private final int mLandscapeResId;
    private final String mDefaultDictionary;
    private final int mQwertyTranslationId;
    private final String mAdditionalIsLetterExceptions;
    private final String mSentenceSeparators;
    private final boolean mKeyboardDefaultEnabled;

    public ApkKeyboardAddOnAndBuilder(Context askContext, Context packageContext, String id, int nameResId,
                                      int layoutResId, int landscapeLayoutResId,
                                      String defaultDictionary, int iconResId,
                                      int physicalTranslationResId,
                                      String additionalIsLetterExceptions,
                                      String sentenceSeparators,
                                      String description,
                                      int keyboardIndex,
                                      boolean keyboardDefaultEnabled) {
        super(askContext, packageContext, KEYBOARD_PREF_PREFIX + id, nameResId, description, keyboardIndex);

        mResId = layoutResId;
        if (landscapeLayoutResId == AddOn.INVALID_RES_ID) {
            mLandscapeResId = mResId;
        } else {
            mLandscapeResId = landscapeLayoutResId;
        }

        mDefaultDictionary = defaultDictionary;
        //mIconResId = iconResId;
        mAdditionalIsLetterExceptions = additionalIsLetterExceptions;
        mSentenceSeparators = sentenceSeparators;
        mQwertyTranslationId = physicalTranslationResId;
        mKeyboardDefaultEnabled = keyboardDefaultEnabled;
    }

    public boolean getKeyboardDefaultEnabled() {
        return mKeyboardDefaultEnabled;
    }

    public String getKeyboardLocale() {
        return mDefaultDictionary;
    }

    public String getSentenceSeparators() {
        return mSentenceSeparators;
    }

    @Nullable
    @Override
    public android.inputmethodservice.Keyboard createAbcKeyboard() {
        Context remoteContext = getPackageContext();
        if (remoteContext == null) return null;
        return new android.inputmethodservice.Keyboard(remoteContext, mLandscapeResId);
    }

    @Override
    public Keyboard createSymKeyboard() {
        return new Keyboard(getPackageContext(), R.xml.sym_en_us);
    }

    @Override
    public Keyboard createNumKeyboard() {
        return new Keyboard(getPackageContext(), R.xml.number);
    }

    @Override
    public boolean isRtl() {
        return false;
    }
}
