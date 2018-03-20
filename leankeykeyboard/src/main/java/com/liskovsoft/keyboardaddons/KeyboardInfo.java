package com.liskovsoft.keyboardaddons;

public interface KeyboardInfo {
    boolean isEnabled();
    String getLangCode();
    String getLangName();
    void setLangName(String langName);
    void setLangCode(String langCode);
    void setEnabled(boolean enabled);
}
