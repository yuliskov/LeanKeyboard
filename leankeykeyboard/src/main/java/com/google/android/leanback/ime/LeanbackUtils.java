package com.google.android.leanback.ime;

import android.os.Handler;
import android.view.View;
import android.view.inputmethod.EditorInfo;

public class LeanbackUtils {
   private static final int ACCESSIBILITY_DELAY_MS = 250;
   private static final Handler sAccessibilityHandler = new Handler();

   public static int getImeAction(EditorInfo var0) {
      return var0.imeOptions & 1073742079;
   }

   public static int getInputTypeClass(EditorInfo var0) {
      return var0.inputType & 15;
   }

   public static int getInputTypeVariation(EditorInfo var0) {
      return var0.inputType & 4080;
   }

   public static boolean isAlphabet(int var0) {
      return Character.isLetter(var0);
   }

   public static void sendAccessibilityEvent(final View var0, boolean var1) {
      if (var0 != null && var1) {
         sAccessibilityHandler.removeCallbacksAndMessages((Object)null);
         sAccessibilityHandler.postDelayed(new Runnable() {
            public void run() {
               var0.sendAccessibilityEvent(16384);
            }
         }, 250L);
      }

   }
}
