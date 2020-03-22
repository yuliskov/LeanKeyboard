package com.liskovsoft.leankeyboard.helpers;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import androidx.core.app.ActivityCompat;

@TargetApi(16)
public class PermissionHelpers {
    // Storage Permissions
    public static final int REQUEST_EXTERNAL_STORAGE = 112;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    // Mic Permissions
    public static final int REQUEST_MIC = 113;
    private static String[] PERMISSIONS_MIC = {
            Manifest.permission.RECORD_AUDIO
    };

    /**
     * Checks if the app has permission to write to device storage<br/>
     * If the app does not has permission then the user will be prompted to grant permissions<br/>
     * Required for the {@link Context#getExternalCacheDir()}<br/>
     * NOTE: runs async<br/>
     *
     * @param activity to apply permissions to
     */
    public static void verifyStoragePermissions(Context activity) {
        requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
    }

    public static void verifyMicPermissions(Context activity) {
        requestPermissions(activity, PERMISSIONS_MIC, REQUEST_MIC);
    }

    /**
     * Only check. There is no prompt.
     * @param activity to apply permissions to
     * @return whether permission already granted
     */
    public static boolean hasStoragePermissions(Context activity) {
        // Check if we have write permission
        return hasPermissions(activity, PERMISSIONS_STORAGE);
    }

    public static boolean hasMicPermissions(Context activity) {
        // Check if we have mic permission
        return hasPermissions(activity, PERMISSIONS_MIC);
    }

    // Utils

    /**
     * Shows permissions dialog<br/>
     * NOTE: runs async
     */
    private static void requestPermissions(Context activity, String[] permissions, int requestId) {
        if (!hasPermissions(activity, permissions) && !Helpers.isGenymotion()) {
            if (activity instanceof Activity) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        (Activity) activity,
                        permissions,
                        requestId
                );
            }
        }
    }

    /**
     * Only check. There is no prompt.
     * @param activity to apply permissions to
     * @return whether permission already granted
     */
    private static boolean hasPermissions(Context activity, String... permissions) {
        if (VERSION.SDK_INT >= 23) {
            for (String permission : permissions) {
                int result = ActivityCompat.checkSelfPermission(activity, permission);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }

        return true;
    }
}
