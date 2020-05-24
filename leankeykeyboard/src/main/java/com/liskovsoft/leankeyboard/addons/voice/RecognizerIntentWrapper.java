package com.liskovsoft.leankeyboard.addons.voice;

import android.content.Context;
import android.content.Intent;

public class RecognizerIntentWrapper {
    private final Context mContext;
    private RecognizerCallback mCallback;

    public RecognizerIntentWrapper(Context context) {
        mContext = context;
    }

    public void setListener(RecognizerCallback callback) {
        mCallback = callback;
    }

    public void startListening() {
        if (mCallback != null) {
            RecognizerIntentActivity.sCallback = mCallback;
            Intent intent = new Intent(mContext, RecognizerIntentActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    }
}
