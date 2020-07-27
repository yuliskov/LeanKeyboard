package com.liskovsoft.leankeyboard.ime;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;

public class LeanbackSuggestionsFactory {
    private static final String TAG = "LbSuggestionsFactory";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG); // Use short text tag to fix "Log tag exceeds limit of 23 characters"
    private static final int MODE_AUTO_COMPLETE = 2;
    private static final int MODE_DEFAULT = 0;
    private static final int MODE_DOMAIN = 1;
    private InputMethodService mContext;
    private int mMode;
    private int mNumSuggestions;
    private final ArrayList<String> mSuggestions = new ArrayList<>();

    public LeanbackSuggestionsFactory(InputMethodService context, int numSuggestions) {
        mContext = context;
        mNumSuggestions = numSuggestions;
    }

    public void clearSuggestions() {
        mSuggestions.clear();
        mSuggestions.add(null); // make room for user input, see LeanbackKeyboardContainer.addUserInputToSuggestions
    }

    public void createSuggestions() {
        clearSuggestions();
        if (mMode == MODE_DOMAIN) {
            String[] domains = mContext.getResources().getStringArray(R.array.common_domains);
            int totalDomains = domains.length;

            for (int i = 0; i < totalDomains; ++i) {
                String domain = domains[i];
                mSuggestions.add(domain);
            }
        }

    }

    public ArrayList<String> getSuggestions() {
        return mSuggestions;
    }

    public void onDisplayCompletions(CompletionInfo[] infos) {
        createSuggestions();
        int len;
        if (infos == null) {
            len = 0;
        } else {
            len = infos.length;
        }

        for (int i = 0; i < len && mSuggestions.size() < mNumSuggestions && !TextUtils.isEmpty(infos[i].getText()); ++i) {
            mSuggestions.add(i, infos[i].getText().toString());
        }

        if (DEBUG) {
            for (len = 0; len < mSuggestions.size(); ++len) {
                Log.d(TAG, "completion " + len + ": " + mSuggestions.get(len));
            }
        }

    }

    public void onStartInput(EditorInfo info) {
        mMode = MODE_DEFAULT;
        if ((info.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
            mMode = MODE_AUTO_COMPLETE;
        }

        switch (LeanbackUtils.getInputTypeClass(info)) {
            case InputType.TYPE_CLASS_TEXT:
                switch (LeanbackUtils.getInputTypeVariation(info)) {
                    case InputType.TYPE_DATETIME_VARIATION_TIME:
                    case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
                        mMode = MODE_DOMAIN;
                        return;
                    default:
                        return;
                }
            default:
        }
    }

    public boolean shouldSuggestionsAmend() {
        return mMode == MODE_DOMAIN;
    }
}
