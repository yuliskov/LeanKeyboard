package com.liskovsoft.leankeyboard.fragments.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import com.liskovsoft.leankeyboard.addons.reskbdfactory.KeyboardInfoAdapter;
import com.liskovsoft.leankeyboard.addons.reskbdfactory.CheckedSource;
import com.liskovsoft.leankeyboard.addons.reskbdfactory.CheckedSource.CheckedItem;
import com.liskovsoft.leankeykeyboard.R;

public class KbLayoutFragment extends BaseSettingsFragment {
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        initCheckedItems();
    }

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

    private void initCheckedItems() {
        CheckedSource source = new KeyboardInfoAdapter(getActivity());
        for (CheckedItem item : source.getItems()) {
            addCheckedAction(item.getTitle(), item::getChecked, item::onClick);
        }
    }
}
