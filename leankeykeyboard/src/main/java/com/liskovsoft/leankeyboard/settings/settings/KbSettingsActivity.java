package com.liskovsoft.leankeyboard.settings.settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.app.PermissionHelper;
import com.liskovsoft.leankeyboard.helpers.PermissionHelpers;
import com.liskovsoft.leankeyboard.receiver.RestartServiceReceiver;

public class KbSettingsActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GuidedStepSupportFragment.addAsRoot(this, new KbSettingsFragment(), android.R.id.content);

        PermissionHelpers.verifyStoragePermissions(this);
        PermissionHelpers.verifyMicPermissions(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // restart kbd service
        Intent intent = new Intent(this, RestartServiceReceiver.class);
        sendBroadcast(intent);
    }
}
