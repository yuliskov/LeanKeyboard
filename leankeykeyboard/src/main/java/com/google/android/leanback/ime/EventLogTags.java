package com.google.android.leanback.ime;

import android.util.EventLog;

public class EventLogTags {
   public static final int TIME_LEANBACK_IME_INPUT = 270900;
   public static final int TOTAL_LEANBACK_IME_BACKSPACE = 270902;

   public static void writeTimeLeanbackImeInput(long var0, long var2) {
      EventLog.writeEvent(270900, new Object[]{var0, var2});
   }

   public static void writeTotalLeanbackImeBackspace(int var0) {
      EventLog.writeEvent(270902, var0);
   }
}
