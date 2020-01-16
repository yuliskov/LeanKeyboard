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

    public RecognizerView(Context var1) {
        super(var1);
    }

    public RecognizerView(Context var1, AttributeSet var2) {
        super(var1, var2);
    }

    public RecognizerView(Context var1, AttributeSet var2, int var3) {
        super(var1, var2, var3);
    }

    private void updateState(RecognizerView.State var1) {
        this.mState = var1;
        this.refreshUi();
    }

    public View getMicButton() {
        return this.mMicButton;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.refreshUi();
    }

    public void onClick() {
        switch (this.mState) {
            case MIC_INITIALIZING:
            default:
                return;
            case LISTENING:
                this.mCallback.onCancelRecordingClicked();
                return;
            case RECORDING:
                this.mCallback.onStopRecordingClicked();
                return;
            case RECOGNIZING:
                this.mCallback.onCancelRecordingClicked();
                return;
            case NOT_LISTENING:
                this.mCallback.onStartRecordingClicked();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onFinishInflate() {
        LayoutInflater.from(this.getContext()).inflate(R.layout.recognizer_view, this, true);
        this.mSoundLevels = (BitmapSoundLevelView) this.findViewById(R.id.microphone);
        this.mMicButton = (ImageView) this.findViewById(R.id.recognizer_mic_button);
        this.mState = RecognizerView.State.NOT_LISTENING;
    }

    @Override
    public void onRestoreInstanceState(Parcelable var1) {
        if (!(var1 instanceof RecognizerView.SavedState)) {
            super.onRestoreInstanceState(var1);
        } else {
            RecognizerView.SavedState var2 = (RecognizerView.SavedState) var1;
            super.onRestoreInstanceState(var2.getSuperState());
            this.mState = var2.mState;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        RecognizerView.SavedState var1 = new RecognizerView.SavedState(super.onSaveInstanceState());
        var1.mState = this.mState;
        return var1;
    }

    protected void refreshUi() {
        if (this.mEnabled) {
            switch (this.mState) {
                case MIC_INITIALIZING:
                    this.mMicButton.setImageResource(R.drawable.vs_micbtn_on_selector);
                    this.mSoundLevels.setEnabled(false);
                    return;
                case LISTENING:
                    this.mMicButton.setImageResource(R.drawable.vs_micbtn_on_selector);
                    this.mSoundLevels.setEnabled(true);
                    return;
                case RECORDING:
                    this.mMicButton.setImageResource(R.drawable.vs_micbtn_rec_selector);
                    this.mSoundLevels.setEnabled(true);
                    return;
                case RECOGNIZING:
                    this.mMicButton.setImageResource(R.drawable.vs_micbtn_off_selector);
                    this.mSoundLevels.setEnabled(false);
                    return;
                case NOT_LISTENING:
                    this.mMicButton.setImageResource(R.drawable.vs_micbtn_off_selector);
                    this.mSoundLevels.setEnabled(false);
                    return;
                default:
            }
        }
    }

    public void setCallback(RecognizerView.Callback callback) {
        this.mCallback = callback;
    }

    public void setMicEnabled(boolean enabled) {
        this.mEnabled = enabled;
        if (enabled) {
            this.mMicButton.setAlpha(1.0F);
            this.mMicButton.setImageResource(R.drawable.ic_voice_available);
        } else {
            this.mMicButton.setAlpha(0.1F);
            this.mMicButton.setImageResource(R.drawable.ic_voice_off);
        }
    }

    public void setMicFocused(boolean focused) {
        if (this.mEnabled) {
            if (focused) {
                this.mMicButton.setImageResource(R.drawable.ic_voice_focus);
            } else {
                this.mMicButton.setImageResource(R.drawable.ic_voice_available);
            }

            LeanbackUtils.sendAccessibilityEvent(this.mMicButton, focused);
        }

    }

    public void setSpeechLevelSource(SpeechLevelSource var1) {
        this.mSoundLevels.setLevelSource(var1);
    }

    public void showInitializingMic() {
        this.updateState(RecognizerView.State.MIC_INITIALIZING);
    }

    public void showListening() {
        this.updateState(RecognizerView.State.LISTENING);
    }

    public void showNotListening() {
        this.updateState(RecognizerView.State.NOT_LISTENING);
    }

    public void showRecognizing() {
        this.updateState(RecognizerView.State.RECOGNIZING);
    }

    public void showRecording() {
        this.updateState(RecognizerView.State.RECORDING);
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

    private static enum State {
        LISTENING, MIC_INITIALIZING, NOT_LISTENING, RECOGNIZING, RECORDING;
    }
}
