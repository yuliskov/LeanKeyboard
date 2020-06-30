package com.liskovsoft.leankeyboard.fragments.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import com.liskovsoft.leankeyboard.activity.settings.KbSettingsActivity2;
import com.liskovsoft.leankeyboard.helpers.Helpers;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

public class MiscFragment extends BaseSettingsFragment {
    private LeanKeyPreferences mPrefs;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
        mPrefs = LeanKeyPreferences.instance(getActivity());
        addCheckedAction(R.string.keep_on_screen, R.string.keep_on_screen_desc, mPrefs::getForceShowKeyboard, mPrefs::setForceShowKeyboard);
        addCheckedAction(R.string.increase_kbd_size, R.string.increase_kbd_size_desc, mPrefs::getEnlargeKeyboard, mPrefs::setEnlargeKeyboard);
        addCheckedAction(R.string.enable_suggestions, R.string.enable_suggestions_desc, mPrefs::getSuggestionsEnabled, mPrefs::setSuggestionsEnabled);
        addCheckedAction(R.string.show_launcher_icon, R.string.show_launcher_icon_desc, this::getLauncherIconShown, this::setLauncherIconShown);
        addCheckedAction(R.string.enable_cyclic_navigation, R.string.enable_cyclic_navigation_desc, mPrefs::getCyclicNavigationEnabled, mPrefs::setCyclicNavigationEnabled);
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getActivity().getResources().getString(R.string.misc);
        String desc = getActivity().getResources().getString(R.string.misc_desc);
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher);

        return new Guidance(
                title,
                desc,
                "",
                icon
        );
    }

    private void setLauncherIconShown(boolean shown) {
        Helpers.setLauncherIconShown(mContext, KbSettingsActivity2.class, shown);
    }

    private boolean getLauncherIconShown() {
        return Helpers.getLauncherIconShown(mContext, KbSettingsActivity2.class);
    }
}
