// 
// Decompiled by Procyon v0.5.30
// 

package com.liskovsoft.other;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.app.Activity;

public class GenericLaunchActivity extends Activity
{
    private boolean isSecondLaunch = false;

    @SuppressLint("WrongConstant")
    private void addIntentFlags(final Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @SuppressLint("WrongConstant")
    private ActivityInfo getCurrentActivityInfo() {
        try {
            return getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA | PackageManager.GET_ACTIVITIES);
        }
        catch (NameNotFoundException ex) {
            ex.printStackTrace();
            makeLongToast(ex.getLocalizedMessage(), 10);
            return null;
        }
    }
    
    private void launchApp() {
        final Intent intent = makeIntent(getCurrentActivityInfo());
        addIntentFlags(intent);
        startIntent(intent);
        finish();
    }
    
    private Intent makeIntent(final ActivityInfo activityInfo) {
        String metaPackage = isSecondLaunch ? "package_alt" : "package";
        String metaClass = isSecondLaunch ? "class_alt" : "class";

        final Bundle metaData = activityInfo.metaData;
        final Intent intent = new Intent();
        if (metaData.getString("intent") != null) {
            intent.setAction(metaData.getString("intent"));
        } else {
            intent.setComponent(new ComponentName(metaData.getString(metaPackage), metaData.getString(metaClass)));
        }
        return intent;
    }

    private void makeLongToast(final String s, int nums) {
        int n;
        for (n = nums / 2, nums = 0; nums < n; ++nums) {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }
    }
    
    private void startIntent(final Intent intent) {
        try {
            startActivity(intent);
        }
        catch (ActivityNotFoundException ex) {
            if (!isSecondLaunch) {
                isSecondLaunch = true;
                launchApp();
                return;
            }

            ex.printStackTrace();
            makeLongToast(ex.getLocalizedMessage(), 10);
        }
    }
    
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        launchApp();
    }
}
