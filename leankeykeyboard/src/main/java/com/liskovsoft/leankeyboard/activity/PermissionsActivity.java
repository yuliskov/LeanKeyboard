package com.liskovsoft.leankeyboard.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.liskovsoft.leankeyboard.helpers.PermissionHelpers;
import com.liskovsoft.leankeyboard.receiver.RestartServiceReceiver;

public class PermissionsActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // restart kbd service
        Intent intent = new Intent(this, RestartServiceReceiver.class);
        sendBroadcast(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        checkPermissions();
    }

    private void checkPermissions() {
        if (!PermissionHelpers.hasMicPermissions(this)) {
            PermissionHelpers.verifyMicPermissions(this);
        } else if (!PermissionHelpers.hasStoragePermissions(this)) {
            PermissionHelpers.verifyStoragePermissions(this);
        } else {
            finish();
        }
    }
}
