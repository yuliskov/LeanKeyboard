package com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.voice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.LeanbackUtils;
import com.liskovsoft.leankeykeyboard.R;

public class RecognizerView extends RelativeLayout {
    private static final boolean DEBUG = false;
    private static final String TAG = "RecognizerView";
    private RecognizerView.Callback mCallback;
    private boolean mEnabled;
    protected ImageView mMicButton;
    private BitmapSoundLevelView mSoundLevels;
    private RecognizerView.State mState;

    private enum State {
        LISTENING, MIC_INITIALIZING, NOT_LISTENING, RECOGNIZING, RECORDING;
    }

    public RecognizerView(Context context) {
        super(context);
    }

    public RecognizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecognizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void updateState(State state) {
        mState = state;
        refreshUi();
    }

    public View getMicButton() {
        return mMicButton;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshUi();
    }

    public void onClick() {
        switch (mState) {
            case MIC_INITIALIZING:
            default:
                return;
            case LISTENING:
                mCallback.onCancelRecordingClicked();
                return;
            case RECORDING:
                mCallback.onStopRecordingClicked();
                return;
            case RECOGNIZING:
                mCallback.onCancelRecordingClicked();
                return;
            case NOT_LISTENING:
                mCallback.onStartRecordingClicked();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onFinishInflate() {
        LayoutInflater.from(this.getContext()).inflate(R.layout.recognizer_view, this, true);
        mSoundLevels = (BitmapSoundLevelView) findViewById(R.id.microphone);
        mMicButton = (ImageView) findViewById(R.id.recognizer_mic_button);
        mState = RecognizerView.State.NOT_LISTENING;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof RecognizerView.SavedState)) {
            super.onRestoreInstanceState(state);
        } else {
            RecognizerView.SavedState savedState = (RecognizerView.SavedState) state;
            super.onRestoreInstanceState(savedState.getSuperState());
            mState = savedState.mState;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        RecognizerView.SavedState savedState = new RecognizerView.SavedState(super.onSaveInstanceState());
        savedState.mState = mState;
        return savedState;
    }

    protected void refreshUi() {
        if (mEnabled) {
            switch (mState) {
                case MIC_INITIALIZING:
                    mMicButton.setImageResource(R.drawable.vs_micbtn_on_selector);
                    mSoundLevels.setEnabled(false);
                    return;
                case LISTENING:
                    mMicButton.setImageResource(R.drawable.vs_micbtn_on_selector);
                    mSoundLevels.setEnabled(true);
                    return;
                case RECORDING:
                    mMicButton.setImageResource(R.drawable.vs_micbtn_rec_selector);
                    mSoundLevels.setEnabled(true);
                    return;
                case RECOGNIZING:
                    mMicButton.setImageResource(R.drawable.vs_micbtn_off_selector);
                    mSoundLevels.setEnabled(false);
                    return;
                case NOT_LISTENING:
                    mMicButton.setImageResource(R.drawable.vs_micbtn_off_selector);
                    mSoundLevels.setEnabled(false);
                    return;
                default:
            }
        }
    }

    public void setCallback(RecognizerView.Callback callback) {
        mCallback = callback;
    }

    public void setMicEnabled(boolean enabled) {
        mEnabled = enabled;
        if (enabled) {
            mMicButton.setAlpha(1.0F);
            mMicButton.setImageResource(R.drawable.ic_voice_available);
        } else {
            mMicButton.setAlpha(0.1F);
            mMicButton.setImageResource(R.drawable.ic_voice_off);
        }
    }

    public void setMicFocused(boolean focused) {
        if (mEnabled) {
            if (focused) {
                mMicButton.setImageResource(R.drawable.ic_voice_focus);
            } else {
                mMicButton.setImageResource(R.drawable.ic_voice_available);
            }

            LeanbackUtils.sendAccessibilityEvent(mMicButton, focused);
        }

    }

    public void setSpeechLevelSource(SpeechLevelSource var1) {
        mSoundLevels.setLevelSource(var1);
    }

    public void showInitializingMic() {
        updateState(State.MIC_INITIALIZING);
    }

    public void showListening() {
        updateState(State.LISTENING);
    }

    public void showNotListening() {
        updateState(State.NOT_LISTENING);
    }

    public void showRecognizing() {
        updateState(State.RECOGNIZING);
    }

    public void showRecording() {
        updateState(State.RECORDING);
    }

    public interface Callback {
        void onCancelRecordingClicked();

        void onStartRecordingClicked();

        void onStopRecordingClicked();
    }

    public static class SavedState extends BaseSavedState {
        public static final Creator<RecognizerView.SavedState> CREATOR = new Creator<RecognizerView.SavedState>() {
            public RecognizerView.SavedState createFromParcel(Parcel var1) {
                return new RecognizerView.SavedState(var1);
            }

            public RecognizerView.SavedState[] newArray(int var1) {
                return new RecognizerView.SavedState[var1];
            }
        };
        RecognizerView.State mState;

        private SavedState(Parcel var1) {
            super(var1);
            this.mState = RecognizerView.State.valueOf(var1.readString());
        }

        public SavedState(Parcelable var1) {
            super(var1);
        }

        public void writeToParcel(Parcel var1, int var2) {
            super.writeToParcel(var1, var2);
            var1.writeString(this.mState.toString());
        }
    }
}
