package com.liskovsoft.leankeyboard.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TextUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = TextUpdateReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent.toUri(0));
    }
}
