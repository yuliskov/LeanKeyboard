package com.google.android.leanback.ime;

import android.inputmethodservice.InputMethodService;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;

public class LeanbackSuggestionsFactory {
   private static final boolean DEBUG = Log.isLoggable("LbSuggestionsFactory", Log.DEBUG);
   private static final int MODE_AUTO_COMPLETE = 2;
   private static final int MODE_DEFAULT = 0;
   private static final int MODE_DOMAIN = 1;
   private static final String TAG = "LbSuggestionsFactory";
   private InputMethodService mContext;
   private int mMode;
   private int mNumSuggestions;
   private final ArrayList<String> mSuggestions = new ArrayList();

   public LeanbackSuggestionsFactory(InputMethodService var1, int var2) {
      this.mContext = var1;
      this.mNumSuggestions = var2;
   }

   public void clearSuggestions() {
      this.mSuggestions.clear();
   }

   public void createSuggestions() {
      this.clearSuggestions();
      if (this.mMode == 1) {
         String[] var3 = this.mContext.getResources().getStringArray(R.array.common_domains);
         int var2 = var3.length;

         for(int var1 = 0; var1 < var2; ++var1) {
            String var4 = var3[var1];
            this.mSuggestions.add(var4);
         }
      }

   }

   public ArrayList<String> getSuggestions() {
      return this.mSuggestions;
   }

   public void onDisplayCompletions(CompletionInfo[] infos) {
      this.createSuggestions();
      int len;
      if (infos == null) {
         len = 0;
      } else {
         len = infos.length;
      }

      for(int i = 0; i < len && this.mSuggestions.size() < this.mNumSuggestions && !TextUtils.isEmpty(infos[i].getText()); ++i) {
         this.mSuggestions.add(i, infos[i].getText().toString());
      }

      if (Log.isLoggable("LbSuggestionsFactory", Log.DEBUG)) {
         for(len = 0; len < this.mSuggestions.size(); ++len) {
            Log.d("LbSuggestionsFactory", "completion " + len + ": " + (String)this.mSuggestions.get(len));
         }
      }

   }

   public void onStartInput(EditorInfo info) {
      this.mMode = 0;
      if ((info.inputType & 65536) != 0) {
         this.mMode = 2;
      }

      switch(LeanbackUtils.getInputTypeClass(info)) {
      case 1:
         switch(LeanbackUtils.getInputTypeVariation(info)) {
         case 32:
         case 208:
            this.mMode = 1;
            return;
         default:
            return;
         }
      default:
      }
   }

   public boolean shouldSuggestionsAmend() {
      return this.mMode == 1;
   }
}
