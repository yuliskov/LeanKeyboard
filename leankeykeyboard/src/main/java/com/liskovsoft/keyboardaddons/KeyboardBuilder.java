package com.liskovsoft.keyboardaddons;

import android.inputmethodservice.Keyboard;
import android.support.annotation.Nullable;

public interface KeyboardBuilder {
    @Nullable
    Keyboard createKeyboard();
}
