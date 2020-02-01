package com.liskovsoft.leankeyboard.addons.voicesearch;

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
            mContext.startActivity(new Intent(mContext, RecognizerIntentActivity.class));
        }
    }
}
