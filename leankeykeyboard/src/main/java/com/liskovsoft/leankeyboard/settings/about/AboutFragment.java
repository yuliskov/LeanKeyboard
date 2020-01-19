package com.liskovsoft.leankeyboard.settings.about;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import androidx.leanback.widget.GuidedAction;
import com.liskovsoft.leankeyboard.utils.AppInfoHelpers;
import com.liskovsoft.leankeykeyboard.R;

import java.util.List;

public class AboutFragment extends GuidedStepSupportFragment {
    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getActivity().getResources().getString(R.string.about);
        String desc = getActivity().getResources().getString(R.string.about_desc);
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
                .title(AppInfoHelpers.getAppVersionName(getActivity())).build();
        actions.add(action);
    }
}
