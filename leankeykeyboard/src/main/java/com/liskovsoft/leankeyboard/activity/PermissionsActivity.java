package com.liskovsoft.leankeyboard.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.liskovsoft.leankeyboard.helpers.PermissionHelpers;
import com.liskovsoft.leankeyboard.receiver.RestartServiceReceiver;

public class PermissionsActivity extends FragmentActivity {
    private int mRequestCalledTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PermissionHelpers.verifyMicPermissions(this);
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

        if (mRequestCalledTimes == 0) {
            PermissionHelpers.verifyStoragePermissions(this);
        }

        if (mRequestCalledTimes == 1) {
            finish();
        }

        mRequestCalledTimes++;
    }
}
