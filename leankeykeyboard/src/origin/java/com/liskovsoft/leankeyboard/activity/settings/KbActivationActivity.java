package com.liskovsoft.leankeyboard.activity.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import com.liskovsoft.leankeyboard.helpers.Helpers;
import com.liskovsoft.leankeyboard.helpers.MessageHelpers;
import com.liskovsoft.leankeyboard.ime.LeanbackImeService;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.List;

public class KbActivationActivity extends Activity {
    private static final String META_PACKAGE_NAME = "package";
    private static final String META_CLASS_NAME = "class";
    private static final String META_PACKAGE_NAME_ALT = "package_alt";
    private static final String META_CLASS_NAME_ALT = "class_alt";
    private static final String META_INTENT_NAME = "intent";
    private static final String MANUAL_URL = "https://github.com/yuliskov/LeanKeyKeyboard/wiki/How-to-Install-LeanKeyKeyboard-on-FireTV";
    private List<Intent> mIntents = new ArrayList<>();
    private String mErrorMsg;

    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);

        String kbdName = getString(R.string.ime_service_name);
        mErrorMsg = getString(R.string.kbd_activation_error);

        String welcomeMsg = getString(R.string.enable_kb_in_system_prefs, kbdName);
        MessageHelpers.showMessage(this, welcomeMsg);

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
            MessageHelpers.showLongMessage(this, ex.getLocalizedMessage());
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
            if (Helpers.startIntent(this, intent)) { // run until first successful attempt
                return;
            }
        }

        MessageHelpers.showLongMessage(this, mErrorMsg);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MANUAL_URL));
        Helpers.startIntent(this, intent);
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
}
