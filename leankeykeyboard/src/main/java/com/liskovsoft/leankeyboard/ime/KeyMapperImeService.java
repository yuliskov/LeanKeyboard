package com.liskovsoft.leankeyboard.ime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import com.liskovsoft.leankeykeyboard.BuildConfig;

public class KeyMapperImeService extends InputMethodService {
    private static final String KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP = BuildConfig.APPLICATION_ID + ".inputmethod.ACTION_INPUT_DOWN_UP";
    private static final String KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN = BuildConfig.APPLICATION_ID + ".inputmethod.ACTION_INPUT_DOWN";
    private static final String KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP = BuildConfig.APPLICATION_ID + ".inputmethod.ACTION_INPUT_UP";
    private static final String KEY_MAPPER_INPUT_METHOD_ACTION_TEXT = BuildConfig.APPLICATION_ID + ".inputmethod.ACTION_INPUT_TEXT";
    private static final String KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT = BuildConfig.APPLICATION_ID + ".inputmethod.EXTRA_TEXT";
    private static final String KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT = BuildConfig.APPLICATION_ID +  ".inputmethod.EXTRA_KEY_EVENT";

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            String action = intent.getAction();
            InputConnection currentInputConnection = getCurrentInputConnection();

            if (currentInputConnection == null || action == null) {
                return;
            }

            KeyEvent downEvent;
            KeyEvent upEvent;

            switch (action) {
                case KEY_MAPPER_INPUT_METHOD_ACTION_TEXT:
                    String text = intent.getStringExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_TEXT);
                    if (text == null) {
                        return;
                    }

                    currentInputConnection.commitText(text, 1);
                    break;
                case KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP:
                    downEvent = intent.getParcelableExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT);
                    if (downEvent == null) {
                        return;
                    }

                    currentInputConnection.sendKeyEvent(downEvent);

                    upEvent = KeyEvent.changeAction(downEvent, KeyEvent.ACTION_UP);
                    currentInputConnection.sendKeyEvent(upEvent);
                    break;
                case KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN:
                    downEvent = intent.getParcelableExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT);
                    if (downEvent == null) {
                        return;
                    }

                    downEvent = KeyEvent.changeAction(downEvent, KeyEvent.ACTION_DOWN);

                    currentInputConnection.sendKeyEvent(downEvent);
                    break;
                case KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP:
                    upEvent = intent.getParcelableExtra(KEY_MAPPER_INPUT_METHOD_EXTRA_KEY_EVENT);
                    if (upEvent == null) {
                        return;
                    }

                    upEvent = KeyEvent.changeAction(upEvent, KeyEvent.ACTION_UP);

                    currentInputConnection.sendKeyEvent(upEvent);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN);
        intentFilter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_DOWN_UP);
        intentFilter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_INPUT_UP);
        intentFilter.addAction(KEY_MAPPER_INPUT_METHOD_ACTION_TEXT);

        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver);
    }
}
