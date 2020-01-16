package com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.voice;

public class SpeechLevelSource {
   private volatile int mSpeechLevel;

   public int getSpeechLevel() {
      return this.mSpeechLevel;
   }

   public boolean isValid() {
      return this.mSpeechLevel > 0;
   }

   public void reset() {
      this.mSpeechLevel = -1;
   }

   public void setSpeechLevel(int var1) {
      if (var1 >= 0 && var1 <= 100) {
         this.mSpeechLevel = var1;
      } else {
         throw new IllegalArgumentException();
      }
   }
}
