package com.liskovsoft.leankeyboard.addons.theme;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.core.content.ContextCompat;
import com.liskovsoft.leankeyboard.utils.LeanKeySettings;
import com.liskovsoft.leankeykeyboard.R;

public class ThemeManager {
    private static final String TAG = ThemeManager.class.getSimpleName();
    private final Context mContext;
    private final RelativeLayout mRootView;
    private final LeanKeySettings mPrefs;

    public ThemeManager(Context context, RelativeLayout rootView) {
        mContext = context;
        mRootView = rootView;
        mPrefs = LeanKeySettings.instance(mContext);
    }

    public void updateKeyboardTheme() {
        String currentTheme = mPrefs.getCurrentTheme();

        if (LeanKeySettings.DEFAULT_THEME_ID.equals(currentTheme)) {
            applyKeyboardColors(
                    R.color.keyboard_background,
                    R.color.candidate_background,
                    R.color.enter_key_font_color
            );
        } else if (LeanKeySettings.DARK_THEME_ID.equals(currentTheme)) {
            applyKeyboardColors(
                    R.color.keyboard_background_dark,
                    R.color.candidate_background_dark,
                    R.color.enter_key_font_color_dark
            );
        }
    }

    public void updateSuggestionsTheme() {
        String currentTheme = mPrefs.getCurrentTheme();

        if (LeanKeySettings.DEFAULT_THEME_ID.equals(currentTheme)) {
            applySuggestionsColors(
                    R.color.candidate_font_color
            );
        } else if (LeanKeySettings.DARK_THEME_ID.equals(currentTheme)) {
            applySuggestionsColors(
                    R.color.candidate_font_color_dark
            );
        }
    }

    private void applyKeyboardColors(
            int keyboardBackground,
            int candidateBackground,
            int enterFontColor) {
        RelativeLayout rootLayout = mRootView.findViewById(R.id.root_ime);

        if (rootLayout != null) {
            rootLayout.setBackgroundColor(ContextCompat.getColor(mContext, keyboardBackground));
        }

        View candidateLayout = mRootView.findViewById(R.id.candidate_background);

        if (candidateLayout != null) {
            candidateLayout.setBackgroundColor(ContextCompat.getColor(mContext, candidateBackground));
        }

        Button enterButton = mRootView.findViewById(R.id.enter);

        if (enterButton != null) {
            enterButton.setTextColor(ContextCompat.getColor(mContext, enterFontColor));
        }
    }

    private void applySuggestionsColors(int candidateFontColor) {
        LinearLayout suggestions = mRootView.findViewById(R.id.suggestions);

        if (suggestions != null) {
            int childCount = suggestions.getChildCount();

            Log.d(TAG, "Number of suggestions: " + childCount);

            for (int i = 0; i < childCount; i++) {
                View child = suggestions.getChildAt(i);

                Button candidateButton = child.findViewById(R.id.text);

                if (candidateButton != null) {
                    candidateButton.setTextColor(ContextCompat.getColor(mContext, candidateFontColor));
                }
            }
        }
    }
}
