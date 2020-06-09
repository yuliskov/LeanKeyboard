package com.liskovsoft.leankeyboard.addons.theme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.core.content.ContextCompat;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardView;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

public class ThemeManager {
    private static final String TAG = ThemeManager.class.getSimpleName();
    private final Context mContext;
    private final RelativeLayout mRootView;
    private final LeanKeyPreferences mPrefs;

    public ThemeManager(Context context, RelativeLayout rootView) {
        mContext = context;
        mRootView = rootView;
        mPrefs = LeanKeyPreferences.instance(mContext);
    }

    public void updateKeyboardTheme() {
        String currentThemeId = mPrefs.getCurrentTheme();

        if (LeanKeyPreferences.THEME_DEFAULT.equals(currentThemeId)) {
            applyKeyboardColors(
                    R.color.keyboard_background,
                    R.color.candidate_background,
                    R.color.enter_key_font_color,
                    R.color.key_text_default
            );
            applyShiftDrawable(-1);
        } else {
            applyForTheme((String themeId) -> {
                Resources resources = mContext.getResources();
                int keyboardBackgroundResId = resources.getIdentifier("keyboard_background_" + themeId.toLowerCase(), "color", mContext.getPackageName());
                int candidateBackgroundResId = resources.getIdentifier("candidate_background_" + themeId.toLowerCase(), "color", mContext.getPackageName());
                int enterFontColorResId = resources.getIdentifier("enter_key_font_color_" + themeId.toLowerCase(), "color", mContext.getPackageName());
                int keyTextColorResId = resources.getIdentifier("key_text_default_" + themeId.toLowerCase(), "color", mContext.getPackageName());

                applyKeyboardColors(
                        keyboardBackgroundResId,
                        candidateBackgroundResId,
                        enterFontColorResId,
                        keyTextColorResId
                );

                int shiftLockOnResId = resources.getIdentifier("ic_ime_shift_lock_on_" + themeId.toLowerCase(), "drawable", mContext.getPackageName());

                applyShiftDrawable(shiftLockOnResId);
            });
        }
    }

    public void updateSuggestionsTheme() {
        String currentTheme = mPrefs.getCurrentTheme();

        if (LeanKeyPreferences.THEME_DEFAULT.equals(currentTheme)) {
            applySuggestionsColors(
                    R.color.candidate_font_color
            );
        } else {
            applyForTheme((String themeId) -> {
                Resources resources = mContext.getResources();
                int candidateFontColorResId = resources.getIdentifier("candidate_font_color_" + themeId.toLowerCase(), "color", mContext.getPackageName());
                applySuggestionsColors(candidateFontColorResId);
            });
        }
    }

    private void applyKeyboardColors(
            int keyboardBackground,
            int candidateBackground,
            int enterFontColor,
            int keyTextColor) {

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

        LeanbackKeyboardView keyboardView = mRootView.findViewById(R.id.main_keyboard);

        if (keyboardView != null) {
            keyboardView.setKeyTextColor(ContextCompat.getColor(mContext, keyTextColor));
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

    private void applyShiftDrawable(int resId) {
        LeanbackKeyboardView keyboardView = mRootView.findViewById(R.id.main_keyboard);

        if (keyboardView != null && resId > 0) {
            Drawable drawable = ContextCompat.getDrawable(mContext, resId);

            keyboardView.setCapsLockDrawable(drawable);
        }
    }

    private void applyForTheme(ThemeCallback callback) {
        String currentThemeId = mPrefs.getCurrentTheme();
        Resources resources = mContext.getResources();
        String[] themes = resources.getStringArray(R.array.keyboard_themes);

        for (String theme : themes) {
            String[] split = theme.split("\\|");
            String themeName = split[0];
            String themeId = split[1];

            if (currentThemeId.equals(themeId)) {
                callback.onThemeFound(themeId);

                break;
            }
        }
    }

    private interface ThemeCallback {
        void onThemeFound(String themeId);
    }
}
