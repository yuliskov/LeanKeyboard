package com.liskovsoft.leankeyboard.settings.kblayout;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import androidx.leanback.widget.GuidedAction;
import com.liskovsoft.leankeykeyboard.R;

import java.util.List;

public class KbLayoutFragment extends GuidedStepSupportFragment {
    private static final long ACTION_ID_NAME = 0;
    private static final long ACTION_ID_EMAIL = 1;

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        Guidance guidance = new Guidance("User Profile", "Use Name",
                "", ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher));
        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction action = new GuidedAction.Builder(getActivity())   .id(ACTION_ID_NAME).description("String name").descriptionEditable(true)
                .title(getString(R.string.user_name)).build();
        actions.add(action);

        action = new GuidedAction.Builder(getActivity()).id(ACTION_ID_EMAIL).description("String email").descriptionEditable(true).title(getString(R.string.email_id)).build();
        actions.add(action);
    }


    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (ACTION_ID_NAME == action.getId()) {
            Log.d("editedText", action.getDescription().toString());
        } else if (ACTION_ID_EMAIL == action.getId()) {
            Log.d("editedText", action.getDescription().toString());
        }
    }
}
