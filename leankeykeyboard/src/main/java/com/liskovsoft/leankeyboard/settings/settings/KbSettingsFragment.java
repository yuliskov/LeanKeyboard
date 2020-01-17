package com.liskovsoft.leankeyboard.settings.settings;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import androidx.leanback.widget.GuidedAction;
import com.liskovsoft.leankeyboard.settings.kbchooser.GenericLaunchActivity;
import com.liskovsoft.leankeyboard.settings.kblayout.KbLayoutFragment;
import com.liskovsoft.leankeykeyboard.R;

import java.util.List;

public class KbSettingsFragment extends GuidedStepSupportFragment {
    private static final long ACTION_ID_ACTIVATE_KB = 0;
    private static final long ACTION_ID_CHANGE_LAYOUT = 1;

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

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_ACTIVATE_KB)
                .hasNext(true)
                .title(getString(R.string.activate_keyboard)).build();
        actions.add(action);

        action = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_CHANGE_LAYOUT)
                .hasNext(true)
                .title(getString(R.string.change_layout)).build();
        actions.add(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == ACTION_ID_ACTIVATE_KB) {
            Intent intent = new Intent(getActivity(), GenericLaunchActivity.class);
            Toast.makeText(getActivity(), getString(R.string.enable_kb_in_system_prefs, getString(R.string.ime_name)), Toast.LENGTH_LONG).show();
            startActivity(intent);
        } else if (action.getId() == ACTION_ID_CHANGE_LAYOUT && getFragmentManager() != null) {
            GuidedStepSupportFragment.add(getFragmentManager(), new KbLayoutFragment());
        }
    }
}
