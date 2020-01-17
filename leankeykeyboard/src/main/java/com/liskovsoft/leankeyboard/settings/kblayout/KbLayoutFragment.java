package com.liskovsoft.leankeyboard.settings.kblayout;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import androidx.leanback.widget.GuidedAction;
import com.liskovsoft.leankeyboard.addons.reskbdfactory.KeyboardInfoAdapter;
import com.liskovsoft.leankeyboard.keyboard.data.CheckedSource;
import com.liskovsoft.leankeyboard.keyboard.data.CheckedSource.CheckedItem;
import com.liskovsoft.leankeykeyboard.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KbLayoutFragment extends GuidedStepSupportFragment {
    private Map<Long, CheckedItem> mActions = new HashMap<>();

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getActivity().getResources().getString(R.string.kb_layout);
        String desc = getActivity().getResources().getString(R.string.kb_layout_desc);
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
        KeyboardInfoAdapter adapter = new KeyboardInfoAdapter(getActivity());
        initCheckedItems(adapter, actions);
    }

    private void initCheckedItems(CheckedSource source, List<GuidedAction> actions) {
        List<CheckedItem> items = source.getItems();
        for (CheckedItem item : items) {
            mActions.put(item.getId(), item);
            GuidedAction action = new GuidedAction.Builder(getActivity())
                    .checked(item.getChecked())
                    .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                    .id(item.getId())
                    .title(item.getTitle())
                    .build();
            actions.add(action);
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        CheckedItem checkedItem = mActions.get(action.getId());

        if (checkedItem != null) {
            checkedItem.onClick(action.isChecked());
        }
    }
}
