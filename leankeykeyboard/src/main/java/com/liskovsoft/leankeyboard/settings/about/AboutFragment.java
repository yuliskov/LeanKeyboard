package com.liskovsoft.leankeyboard.settings.about;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
    private static final String MARKET_LINK = "https://play.google.com/store/apps/details?id=org.liskovsoft.androidtv.rukeyboard";

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
        appendInfoAction(AppInfoHelpers.getApplicationName(getActivity()), actions);
        appendInfoAction("Version " + AppInfoHelpers.getAppVersionName(getActivity()), actions);
    }

    private void appendInfoAction(String textLine, List<GuidedAction> actions) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .title(textLine)
                .build();
        actions.add(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_LINK));
        startActivity(intent);
    }
}
