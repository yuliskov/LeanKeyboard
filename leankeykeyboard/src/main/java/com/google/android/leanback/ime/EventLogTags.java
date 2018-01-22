package com.google.android.leanback.ime;

import android.util.EventLog;

public class EventLogTags {
   public static final int TIME_LEANBACK_IME_INPUT = 270900;
   public static final int TOTAL_LEANBACK_IME_BACKSPACE = 270902;

   public static void writeTimeLeanbackImeInput(long time, long duration) {
      EventLog.writeEvent(TIME_LEANBACK_IME_INPUT, new Object[]{time, duration});
   }

   public static void writeTotalLeanbackImeBackspace(int count) {
      EventLog.writeEvent(TOTAL_LEANBACK_IME_BACKSPACE, count);
   }
}
