package com.liskovsoft.leankeyboard.keyboard.android.leanback.ime.voice;

public class SpeechLevelSource {
   private volatile int mSpeechLevel;

   public int getSpeechLevel() {
      return mSpeechLevel;
   }

   public boolean isValid() {
      return mSpeechLevel > 0;
   }

   public void reset() {
      mSpeechLevel = -1;
   }

   public void setSpeechLevel(int speechLevel) {
      if (speechLevel >= 0 && speechLevel <= 100) {
         mSpeechLevel = speechLevel;
      } else {
         throw new IllegalArgumentException();
      }
   }
}
