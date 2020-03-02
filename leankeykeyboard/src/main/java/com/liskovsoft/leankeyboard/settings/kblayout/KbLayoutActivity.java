package com.liskovsoft.leankeyboard.settings.kblayout;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import com.liskovsoft.leankeyboard.other.RestartServiceReceiver;

public class KbLayoutActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GuidedStepSupportFragment.addAsRoot(this, new KbLayoutFragment(), android.R.id.content);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // restart kbd service
        Intent intent = new Intent(this, RestartServiceReceiver.class);
        sendBroadcast(intent);
    }
}
