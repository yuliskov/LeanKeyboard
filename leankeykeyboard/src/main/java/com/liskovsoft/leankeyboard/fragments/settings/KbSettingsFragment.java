package com.liskovsoft.leankeyboard.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import com.liskovsoft.leankeyboard.activity.settings.KbActivationActivity;
import com.liskovsoft.leankeykeyboard.R;

public class KbSettingsFragment extends BaseSettingsFragment {
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        addNextAction(R.string.activate_keyboard, () -> {
            Intent intent = new Intent(getActivity(), KbActivationActivity.class);
            startActivity(intent);
        });

        addNextAction(R.string.change_layout, () -> startGuidedFragment(new KbLayoutFragment()));

        addNextAction(R.string.misc, () -> startGuidedFragment(new MiscFragment()));

        addNextAction(R.string.about_desc, () -> startGuidedFragment(new AboutFragment()));
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getActivity().getResources().getString(R.string.kb_settings);
        String desc = getActivity().getResources().getString(R.string.kb_settings_desc);
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher);

        return new Guidance(
                title,
                desc,
                "",
                icon
        );
    }

    private void startGuidedFragment(GuidedStepSupportFragment fragment) {
        if (getFragmentManager() != null) {
            GuidedStepSupportFragment.add(getFragmentManager(), fragment);
        }
    }
}
