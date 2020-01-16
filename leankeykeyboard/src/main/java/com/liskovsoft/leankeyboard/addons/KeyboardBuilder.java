package com.liskovsoft.leankeyboard.addons;

import android.inputmethodservice.Keyboard;
import androidx.annotation.Nullable;

public interface KeyboardBuilder {
    @Nullable
    Keyboard createKeyboard();
}
