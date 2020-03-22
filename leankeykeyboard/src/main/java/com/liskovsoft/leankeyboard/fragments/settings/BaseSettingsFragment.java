package com.liskovsoft.leankeyboard.fragments.settings;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidedAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BaseSettingsFragment extends GuidedStepSupportFragment {
    private Map<Long, CheckedAction> mCheckedActions = new LinkedHashMap<>();
    private Map<Long, NextAction> mNextActions = new LinkedHashMap<>();
    private long mId;

    protected interface OnChecked {
        void onChecked(boolean checked);
    }

    protected interface GetChecked {
        boolean getChecked();
    }

    protected interface OnClick {
        void onClick();
    }

    protected void addCheckedAction(int titleResId, int descResId, GetChecked getChecked, OnChecked onChecked) {
        addCheckedAction(getString(titleResId), getString(descResId), getChecked, onChecked);
    }

    protected void addCheckedAction(int titleRedId, GetChecked getChecked, OnChecked onChecked) {
        addCheckedAction(getString(titleRedId), getChecked, onChecked);
    }

    protected void addCheckedAction(String title, GetChecked getChecked, OnChecked onChecked) {
        mCheckedActions.put(mId++, new CheckedAction(title, getChecked, onChecked));
    }

    protected void addCheckedAction(String title, String desc, GetChecked getChecked, OnChecked onChecked) {
        mCheckedActions.put(mId++, new CheckedAction(title, desc, getChecked, onChecked));
    }

    protected void addNextAction(int resId, OnClick onClick) {
        mNextActions.put(mId++, new NextAction(resId, onClick));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        for (long id : mCheckedActions.keySet()) {
            addCheckedItem(id, mCheckedActions.get(id), actions);
        }

        for (long id : mNextActions.keySet()) {
            addNextItem(id, mNextActions.get(id), actions);
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        CheckedAction checkedAction = mCheckedActions.get(action.getId());

        if (checkedAction != null) {
            checkedAction.onChecked(action.isChecked());
        }

        NextAction nextAction = mNextActions.get(action.getId());

        if (nextAction != null) {
            nextAction.onClick();
        }
    }

    private void addNextItem(long id, NextAction nextAction, List<GuidedAction> actions) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .id(id)
                .hasNext(true)
                .title(nextAction.getResId()).build();
        actions.add(action);
    }

    private void addCheckedItem(long id, CheckedAction checkedAction, List<GuidedAction> actions) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .checked(checkedAction.isChecked())
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .id(id)
                .title(checkedAction.getTitle())
                .build();

        if (checkedAction.getDesc() != null) {
            action.setDescription(checkedAction.getDesc());
        }

        actions.add(action);
    }

    private static class CheckedAction {
        private final String mDesc;
        private final GetChecked mGetChecked;
        private final OnChecked mOnChecked;
        private final String mTitle;

        public CheckedAction(String title, GetChecked getChecked, OnChecked onChecked) {
            this(title, null, getChecked, onChecked);
        }

        public CheckedAction(String title, String desc, GetChecked getChecked, OnChecked onChecked) {
            mTitle = title;
            mDesc = desc;
            mGetChecked = getChecked;
            mOnChecked = onChecked;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getDesc() {
            return mDesc;
        }

        public boolean isChecked() {
            return mGetChecked.getChecked();
        }

        public void onChecked(boolean checked) {
            mOnChecked.onChecked(checked);
        }
    }

    private static class NextAction {
        private final int mResId;
        private final OnClick mOnClick;

        public NextAction(int resId, OnClick onClick) {
            mResId = resId;
            mOnClick = onClick;
        }

        public int getResId() {
            return mResId;
        }

        public void onClick() {
            mOnClick.onClick();
        }
    }
}
