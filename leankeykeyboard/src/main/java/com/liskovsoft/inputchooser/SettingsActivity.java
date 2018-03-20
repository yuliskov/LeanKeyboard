// 
// Decompiled by Procyon v0.5.30
// 

package com.liskovsoft.inputchooser;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.widget.Toast;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.app.Activity;

public class SettingsActivity extends Activity
{
    @SuppressLint("WrongConstant")
    private void addIntentFlags(final Intent intent) {
        intent.addFlags(67108864);
        intent.addFlags(536870912);
    }

    @SuppressLint("WrongConstant")
    private ActivityInfo getCurrentActivityInfo() {
        try {
            return this.getPackageManager().getActivityInfo(this.getComponentName(), 129);
        }
        catch (NameNotFoundException ex) {
            ex.printStackTrace();
            this.makeLongToast(ex.getLocalizedMessage(), 10);
            return null;
        }
    }
    
    private void launchApp() {
        final Intent intent = this.makeIntent(this.getCurrentActivityInfo());
        this.addIntentFlags(intent);
        this.startIntent(intent);
        this.finish();
    }
    
    private Intent makeIntent(final ActivityInfo activityInfo) {
        final Bundle metaData = activityInfo.metaData;
        final Intent intent = new Intent();
        if (metaData.getString("intent") != null) {
            intent.setAction(metaData.getString("intent"));
            return intent;
        }
        intent.setComponent(new ComponentName(metaData.getString("package"), metaData.getString("class")));
        return intent;
    }

    private void makeLongToast(final String s, int i) {
        int n;
        for (n = i / 2, i = 0; i < n; ++i) {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }
    }
    
    private void startIntent(final Intent intent) {
        try {
            this.startActivity(intent);
        }
        catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
            this.makeLongToast(ex.getLocalizedMessage(), 10);
        }
    }
    
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        // new ChooseKeyboardDialog(this).run();
        this.launchApp();
    }
}
