package com.liskovsoft.leankeyboard.fragments.settings;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import androidx.leanback.widget.GuidedAction;
import com.liskovsoft.leankeyboard.helpers.AppInfoHelpers;
import com.liskovsoft.leankeykeyboard.R;

import java.util.List;

public class AboutFragment extends GuidedStepSupportFragment {
    private static final String MARKET_URL = "https://play.google.com/store/apps/details?id=org.liskovsoft.androidtv.rukeyboard";
    private static final String DONATE_URL = "https://www.donationalerts.com/r/firsthash";
    private static final String RELEASES_URL = "https://github.com/yuliskov/LeankeyKeyboard/releases";
    private static final String ISSUE_URL = "https://github.com/yuliskov/LeankeyKeyboard/issues";
    private static final String[] URL_MAPPING = {DONATE_URL, ISSUE_URL, RELEASES_URL, MARKET_URL};
    private int mId;

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
        appendInfoAction(getString(R.string.about_donate), actions);
        appendInfoAction(getString(R.string.about_issue), actions);
        appendInfoAction(getString(R.string.about_releases), actions);

        String appName = AppInfoHelpers.getApplicationName(getActivity());
        String appVersion = AppInfoHelpers.getAppVersionName(getActivity());
        String flavorName = getString(R.string.flavor_name);
        appendInfoAction(String.format("%s (%s %s)", appName, appVersion, flavorName), actions);
    }

    private void appendInfoAction(String textLine, List<GuidedAction> actions) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .title(textLine)
                .id(mId++)
                .build();
        actions.add(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        int idx = (int) action.getId();
        String link = URL_MAPPING.length > idx ? URL_MAPPING[idx] : MARKET_URL;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        
        startActivity(intent);
    }
}
