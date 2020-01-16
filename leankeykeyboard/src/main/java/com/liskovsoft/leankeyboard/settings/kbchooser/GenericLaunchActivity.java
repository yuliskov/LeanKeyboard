package com.liskovsoft.leankeyboard.settings.kbchooser;

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

import java.util.ArrayList;
import java.util.List;

public class GenericLaunchActivity extends Activity
{
    private static final String META_PACKAGE_NAME = "package";
    private static final String META_CLASS_NAME = "class";
    private static final String META_PACKAGE_NAME_ALT = "package_alt";
    private static final String META_CLASS_NAME_ALT = "class_alt";
    private static final String META_INTENT_NAME = "intent";
    private List<Intent> mIntents = new ArrayList<>();

    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        launchApp();
    }

    @SuppressLint("WrongConstant")
    private void addCommonIntentFlags(final Intent intent) {
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
            makeLongToast(ex.getLocalizedMessage());
            return null;
        }
    }
    
    private void launchApp() {
        makeIntentList();
        startIntents();
        finish();
    }

    private void startIntents() {
        for (Intent intent : mIntents) {
            boolean result = startIntent(intent);
            if (result) { // run until first successful attempt
                break;
            }
        }
    }

    private void makeIntentList() {
        final ActivityInfo activityInfo = getCurrentActivityInfo();
        if (activityInfo == null) {
            return;
        }
        final Bundle metaData = activityInfo.metaData;

        String metaPackageName = metaData.getString(META_PACKAGE_NAME);
        String metaClassName = metaData.getString(META_CLASS_NAME);
        mIntents.add(createIntent(metaPackageName, metaClassName));

        String metaPackageNameAlt = metaData.getString(META_PACKAGE_NAME_ALT);
        String metaClassNameAlt = metaData.getString(META_CLASS_NAME_ALT);
        mIntents.add(createIntent(metaPackageNameAlt, metaClassNameAlt));

        String metaIntentName = metaData.getString(META_INTENT_NAME);
        mIntents.add(createIntent(metaIntentName));
    }

    private Intent createIntent(String intentName) {
        if (intentName == null) {
            return null;
        }

        final Intent intent = new Intent();
        intent.setAction(intentName);
        addCommonIntentFlags(intent);
        return intent;
    }

    private Intent createIntent(String packageName, String className) {
        if (packageName == null || className == null) {
            return null;
        }

        final Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, className));
        addCommonIntentFlags(intent);
        return intent;
    }

    private void makeLongToast(final String s) {
        makeLongToast(s, 10);
    }

    private void makeLongToast(final String s, int nums) {
        int n;
        for (n = nums / 2, nums = 0; nums < n; ++nums) {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }
    }

    private boolean startIntent(final Intent intent) {
        if (intent == null) {
            return false;
        }

        try {
            startActivity(intent);
        }
        catch (ActivityNotFoundException ex) {
            return false;
        }

        return true;
    }
}
