package com.liskovsoft.leankeyboard.settings.kblayout;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;

public class KbLayoutActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_kb_layout);

        GuidedStepSupportFragment.addAsRoot(this, new KbLayoutFragment(), android.R.id.content);
    }
}
