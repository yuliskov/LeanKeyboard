package com.liskovsoft.leankeyboard.addons.voicesearch;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class RecognizerIntentActivity extends AppCompatActivity {
    public static RecognizerCallback sCallback;
    private VoiceSearchBridge mBridge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBridge = new VoiceSearchBridge(this, searchText -> sCallback.openSearchPage(searchText));

        mBridge.displaySpeechRecognizers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mBridge.onActivityResult(requestCode, resultCode, data);

        finish();
    }
}
