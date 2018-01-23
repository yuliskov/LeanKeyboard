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

package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.*;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.AddOnsFactory;
import com.anysoftkeyboard.utils.Logger;

import java.util.ArrayList;
import java.util.List;


public class ApkResourceKeyboardFactory extends AddOnsFactory<KeyboardAddOnAndBuilder> {
    private static final String TAG = "ASK_KF";

    private static final String XML_LAYOUT_RES_ID_ATTRIBUTE = "layoutResId";
    private static final String XML_LANDSCAPE_LAYOUT_RES_ID_ATTRIBUTE = "landscapeResId";
    private static final String XML_ICON_RES_ID_ATTRIBUTE = "iconResId";
    private static final String XML_DICTIONARY_NAME_ATTRIBUTE = "defaultDictionaryLocale";
    private static final String XML_ADDITIONAL_IS_LETTER_EXCEPTIONS_ATTRIBUTE = "additionalIsLetterExceptions";
    private static final String XML_SENTENCE_SEPARATOR_CHARACTERS_ATTRIBUTE = "sentenceSeparators";
    private static final String DEFAULT_SENTENCE_SEPARATORS = ".,!?)]:;";
    private static final String XML_PHYSICAL_TRANSLATION_RES_ID_ATTRIBUTE = "physicalKeyboardMappingResId";
    private static final String XML_DEFAULT_ATTRIBUTE = "defaultEnabled";

    public ApkResourceKeyboardFactory() {
        super(TAG, "com.liskovsoft.leankey.langpack.KEYBOARD", "com.liskovsoft.leankey.langpack.keyboards",
                "Keyboards", "Keyboard",
                0, true);
    }

    public List<KeyboardAddOnAndBuilder> getAllAvailableKeyboards(Context askContext) {
        return getAllAddOns(askContext);
    }

    public List<KeyboardAddOnAndBuilder> getEnabledKeyboards(Context askContext) {
        final List<KeyboardAddOnAndBuilder> allAddOns = getAllAddOns(askContext);
        Logger.i(TAG, "Creating enabled addons list. I have a total of " + allAddOns.size() + " addons");

        //getting shared prefs to determine which to create.
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(askContext);

        final ArrayList<KeyboardAddOnAndBuilder> enabledAddOns = new ArrayList<>();
        for (int addOnIndex = 0; addOnIndex < allAddOns.size(); addOnIndex++) {
            final KeyboardAddOnAndBuilder addOn = allAddOns.get(addOnIndex);

            final boolean addOnEnabled = sharedPreferences.getBoolean(addOn.getId(), addOn.getKeyboardDefaultEnabled());

            if (addOnEnabled) {
                enabledAddOns.add(addOn);
            }
        }

        // Fix: issue 219
        // Check if there is any keyboards created if not, lets create a default english keyboard
        if (enabledAddOns.size() == 0) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            final KeyboardAddOnAndBuilder addOn = allAddOns.get(0);
            editor.putBoolean(addOn.getId(), true);
            editor.commit();
            enabledAddOns.add(addOn);
        }

        for (final KeyboardAddOnAndBuilder addOn : enabledAddOns) {
            Logger.d(TAG, "Factory provided addon: %s", addOn.getId());
        }

        return enabledAddOns;
    }

    @Override
    protected KeyboardAddOnAndBuilder createConcreteAddOn(Context askContext, Context context, String prefId, int nameId, String description, int sortIndex, AttributeSet attrs) {
        final int layoutResId = attrs.getAttributeResourceValue(null, XML_LAYOUT_RES_ID_ATTRIBUTE, AddOn.INVALID_RES_ID);
        final int landscapeLayoutResId = attrs.getAttributeResourceValue(null, XML_LANDSCAPE_LAYOUT_RES_ID_ATTRIBUTE, AddOn.INVALID_RES_ID);
        //final int iconResId = attrs.getAttributeResourceValue(null, XML_ICON_RES_ID_ATTRIBUTE, R.drawable.sym_keyboard_notification_icon);
        final int iconResId = 0;
        final String defaultDictionary = attrs.getAttributeValue(null, XML_DICTIONARY_NAME_ATTRIBUTE);
        final String additionalIsLetterExceptions = attrs.getAttributeValue(null, XML_ADDITIONAL_IS_LETTER_EXCEPTIONS_ATTRIBUTE);
        String sentenceSeparators = attrs.getAttributeValue(null, XML_SENTENCE_SEPARATOR_CHARACTERS_ATTRIBUTE);
        if (TextUtils.isEmpty(sentenceSeparators)) sentenceSeparators = DEFAULT_SENTENCE_SEPARATORS;
        final int physicalTranslationResId = attrs.getAttributeResourceValue(null, XML_PHYSICAL_TRANSLATION_RES_ID_ATTRIBUTE, AddOn.INVALID_RES_ID);
        // A keyboard is enabled by default if it is the first one (index==1)
        final boolean keyboardDefault = attrs.getAttributeBooleanValue(null, XML_DEFAULT_ATTRIBUTE, sortIndex == 1);

        // asserting
        if ((prefId == null) || (nameId == AddOn.INVALID_RES_ID) || (layoutResId == AddOn.INVALID_RES_ID)) {
            Logger.e(TAG, "External Keyboard does not include all mandatory details! Will not create keyboard.");
            return null;
        } else {
            Logger.d(TAG,
                    "External keyboard details: prefId:" + prefId + " nameId:"
                            + nameId + " resId:" + layoutResId
                            + " landscapeResId:" + landscapeLayoutResId
                            + " iconResId:" + iconResId + " defaultDictionary:"
                            + defaultDictionary);
            return new KeyboardAddOnAndBuilder(askContext, context,
                    prefId, nameId, layoutResId, landscapeLayoutResId,
                    defaultDictionary, iconResId, physicalTranslationResId,
                    additionalIsLetterExceptions, sentenceSeparators,
                    description, sortIndex, keyboardDefault);
        }
    }

    public boolean hasMultipleAlphabets(Context askContext) {
        return getEnabledKeyboards(askContext).size() > 1;
    }

    public Keyboard createKeyboard(Context context) {
        List<KeyboardAddOnAndBuilder> keyboardBuilders = getAllAvailableKeyboards(context);
        if (keyboardBuilders.size() == 0)
            return new Keyboard(context, 0x7f04000c); // ru keyboard resource id
        // remember, only one external keyboard supported
        return keyboardBuilders.get(0).createKeyboard();
    }
}
