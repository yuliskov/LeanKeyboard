package com.liskovsoft.leankeyboard.addons.keyboards;

public interface KeyboardInfo {
    String getLangCode();
    void setLangCode(String langCode);
    String getLangName();
    void setLangName(String langName);
    boolean isEnabled();
    void setEnabled(boolean enabled);
    boolean isAzerty();
    void setIsAzerty(boolean enabled);
}
