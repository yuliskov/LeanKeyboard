package com.liskovsoft.leankeyboard.fragments.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

public class KbThemeFragment extends BaseSettingsFragment {
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;

        initRadioItems();
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getActivity().getResources().getString(R.string.kb_theme);
        String desc = getActivity().getResources().getString(R.string.kb_theme_desc);
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher);

        return new Guidance(
                title,
                desc,
                "",
                icon
        );
    }

    private void initRadioItems() {
        String[] themes = mContext.getResources().getStringArray(R.array.keyboard_themes);

        LeanKeyPreferences prefs = LeanKeyPreferences.instance(mContext);
        String currentTheme = prefs.getCurrentTheme();

        for (String theme : themes) {
            String[] split = theme.split("\\|");
            String themeName = split[0];
            String themeId = split[1];
            addRadioAction(themeName, () -> currentTheme.equals(themeId), (checked) -> prefs.setCurrentTheme(themeId));
        }
    }
}
