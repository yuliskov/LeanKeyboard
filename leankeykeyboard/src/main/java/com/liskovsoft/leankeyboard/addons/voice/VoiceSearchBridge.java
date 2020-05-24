package com.liskovsoft.leankeyboard.addons.voice;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

class VoiceSearchBridge implements SearchCallback {
    private final ArrayList<VoiceDialog> mDialogs;
    private final AppCompatActivity mActivity;
    private final RecognizerCallback mCallback;

    public VoiceSearchBridge(AppCompatActivity activity, RecognizerCallback callback) {
        mActivity = activity;
        mCallback = callback;
        mDialogs = new ArrayList<>();
        mDialogs.add(new SystemVoiceDialog(activity, this));
        mDialogs.add(new VoiceOverlayDialog(activity, this));
    }

    public void displaySpeechRecognizers() {
        for (VoiceDialog dialog : mDialogs) {
            if (dialog.displaySpeechRecognizer()) { // fist successful attempt is used
                break;
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (VoiceDialog dialog : mDialogs) {
            if (dialog instanceof ActivityListener) {
                ((ActivityListener) dialog).onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void openSearchPage(String searchText) {
        if (mCallback != null) {
            mCallback.openSearchPage(searchText);
        }
    }
}
