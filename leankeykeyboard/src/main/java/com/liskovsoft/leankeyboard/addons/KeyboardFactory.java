package com.liskovsoft.leankeyboard.addons;

import android.content.Context;

import java.util.List;

public interface KeyboardFactory {
    List<? extends KeyboardBuilder> getAllAvailableKeyboards(Context context);
    boolean needUpdate();
}
