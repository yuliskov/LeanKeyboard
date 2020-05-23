package com.liskovsoft.leankeyboard.addons.theme;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import androidx.core.content.ContextCompat;
import com.liskovsoft.leankeyboard.utils.LeanKeySettings;
import com.liskovsoft.leankeykeyboard.R;

public class ThemeManager {
    private final Context mContext;

    public ThemeManager(Context context) {
        mContext = context;
    }

    public void applyTo(View rootView) {
        String currentTheme = LeanKeySettings.instance(mContext).getCurrentTheme();

        if (LeanKeySettings.DEFAULT_THEME_ID.equals(currentTheme)) {
            applyColors(rootView, R.color.keyboard_background, R.color.candidate_background, R.color.enter_key_font_color);
        } else if (LeanKeySettings.DARK_THEME_ID.equals(currentTheme)) {
            applyColors(rootView, R.color.keyboard_background_dark, R.color.candidate_background_dark, R.color.enter_key_font_color_dark);
        }
    }

    private void applyColors(View rootView, int keyboardBackground, int candidateBackground, int enterFontColor) {
        RelativeLayout rootLayout = rootView.findViewById(R.id.root_ime);

        if (rootLayout != null) {
            rootLayout.setBackgroundColor(ContextCompat.getColor(mContext, keyboardBackground));
        }

        View candidateLayout = rootView.findViewById(R.id.candidate_background);

        if (candidateLayout != null) {
            candidateLayout.setBackgroundColor(ContextCompat.getColor(mContext, candidateBackground));
        }

        Button enterButton = rootView.findViewById(R.id.enter);

        if (enterButton != null) {
            enterButton.setTextColor(ContextCompat.getColor(mContext, enterFontColor));
        }
    }
}
