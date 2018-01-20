package com.google.android.leanback.ime;

import android.graphics.PointF;
import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard.Key;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnHoverListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.RelativeLayout;
import com.google.android.pano.util.TouchNavSpaceTracker;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;

public class LeanbackKeyboardController implements LeanbackKeyboardContainer.VoiceListener, LeanbackKeyboardContainer.DismissListener, OnTouchListener, OnHoverListener, Runnable {
   public static final int CLICK_MOVEMENT_BLOCK_DURATION_MS = 500;
   private static final boolean DEBUG = false;
   private static final int KEY_CHANGE_HISTORY_SIZE = 10;
   private static final long KEY_CHANGE_REVERT_TIME_MS = 100L;
   private static final String TAG = "LbKbController";
   private boolean clickConsumed;
   private long lastClickTime;
   private LeanbackKeyboardContainer mContainer;
   private InputMethodService mContext;
   private LeanbackKeyboardController.DoubleClickDetector mDoubleClickDetector;
   private LeanbackKeyboardContainer.KeyFocus mDownFocus;
   private Handler mHandler;
   private LeanbackKeyboardController.InputListener mInputListener;
   ArrayList<LeanbackKeyboardController.KeyChange> mKeyChangeHistory;
   private LeanbackKeyboardContainer.KeyFocus mKeyDownKeyFocus;
   private boolean mKeyDownReceived;
   private boolean mLongPressHandled;
   private int mMoveCount;
   private OnLayoutChangeListener mOnLayoutChangeListener;
   public float mResizeSquareDistance;
   private TouchNavSpaceTracker mSpaceTracker;
   private LeanbackKeyboardContainer.KeyFocus mTempFocus;
   private PointF mTempPoint;
   private LeanbackKeyboardController.TouchEventListener mTouchEventListener;
   private long prevTime;

   public LeanbackKeyboardController(InputMethodService var1, LeanbackKeyboardController.InputListener var2) {
      this(var1, var2, new TouchNavSpaceTracker(), new LeanbackKeyboardContainer(var1));
   }

   LeanbackKeyboardController(InputMethodService var1, LeanbackKeyboardController.InputListener var2, TouchNavSpaceTracker var3, LeanbackKeyboardContainer var4) {
      this.mDoubleClickDetector = new LeanbackKeyboardController.DoubleClickDetector();
      this.mOnLayoutChangeListener = new OnLayoutChangeListener() {
         public void onLayoutChange(View var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
            var2 = var4 - var2;
            var3 = var5 - var3;
            if (var2 > 0 && var3 > 0) {
               if (LeanbackKeyboardController.this.mSpaceTracker != null) {
                  LeanbackKeyboardController.this.mSpaceTracker.setPixelSize((float)var2, (float)var3);
               }

               if (var2 != var8 - var6 || var3 != var9 - var7) {
                  LeanbackKeyboardController.this.initInputView();
               }
            }

         }
      };
      this.mTouchEventListener = new LeanbackKeyboardController.TouchEventListener();
      this.mDownFocus = new LeanbackKeyboardContainer.KeyFocus();
      this.mTempFocus = new LeanbackKeyboardContainer.KeyFocus();
      this.mKeyChangeHistory = new ArrayList(11);
      this.mTempPoint = new PointF();
      this.mKeyDownReceived = false;
      this.mLongPressHandled = false;
      this.mContext = var1;
      this.mResizeSquareDistance = var1.getResources().getDimension(R.dimen.resize_move_distance);
      this.mResizeSquareDistance *= this.mResizeSquareDistance;
      this.mInputListener = var2;
      this.setSpaceTracker(var3);
      this.setKeyboardContainer(var4);
      this.mContainer.setVoiceListener(this);
      this.mContainer.setDismissListener(this);
   }

   private boolean applyLETVFixesDown(int var1) {
      switch(var1) {
      case 82:
      case 85:
      case 89:
      case 90:
         return true;
      default:
         return false;
      }
   }

   private boolean applyLETVFixesUp(int var1) {
      switch(var1) {
      case 82:
         this.mContainer.switchToNextKeyboard();
         break;
      case 85:
         this.fakeKeyIndex(0, 2);
         break;
      case 89:
         this.fakeKeyCode(-5);
         break;
      case 90:
         this.fakeKeyCode(32);
         break;
      default:
         return false;
      }

      return true;
   }

   private void beginLongClickCountdown() {
      this.clickConsumed = false;
      Handler var2 = this.mHandler;
      Handler var1 = var2;
      if (var2 == null) {
         var1 = new Handler();
         this.mHandler = var1;
      }

      var1.removeCallbacks(this);
      var1.postDelayed(this, (long)1000);
   }

   private void clearKeyIfNecessary() {
      ++this.mMoveCount;
      if (this.mMoveCount >= 3) {
         this.mMoveCount = 0;
         this.mKeyDownKeyFocus = null;
      }

   }

   private void commitKey() {
      this.commitKey(this.mContainer.getCurrFocus());
   }

   private void commitKey(LeanbackKeyboardContainer.KeyFocus var1) {
      if (this.mContainer != null && var1 != null) {
         switch(var1.type) {
         case 1:
            this.mContainer.onVoiceClick();
            return;
         case 2:
            this.mInputListener.onEntry(5, 0, (CharSequence)null);
            return;
         case 3:
            this.mInputListener.onEntry(2, 0, this.mContainer.getSuggestionText(var1.index));
            return;
         default:
            Key var2 = this.mContainer.getKey(var1.type, var1.index);
            if (var2 != null) {
               this.handleCommitKeyboardKey(var2.codes[0], var2.label);
               return;
            }
         }
      }

   }

   private void fakeClickDown() {
      this.mContainer.setTouchState(3);
   }

   private void fakeClickUp() {
      LeanbackKeyboardContainer var1 = this.mContainer;
      this.commitKey(var1.getCurrFocus());
      var1.setTouchState(0);
   }

   private void fakeKeyCode(int var1) {
      this.mContainer.getCurrFocus().code = var1;
      this.handleCommitKeyboardKey(var1, (CharSequence)null);
   }

   private void fakeKeyIndex(int var1, int var2) {
      LeanbackKeyboardContainer.KeyFocus var3 = this.mContainer.getCurrFocus();
      var3.index = var1;
      var3.type = var2;
      this.commitKey(var3);
   }

   private void fakeLongClickDown() {
      LeanbackKeyboardContainer var1 = this.mContainer;
      var1.onKeyLongPress();
      var1.setTouchState(3);
   }

   private void fakeLongClickUp() {
      this.mContainer.setTouchState(0);
   }

   private PointF getBestSnapPosition(PointF var1, long var2) {
      if (this.mKeyChangeHistory.size() <= 1) {
         return var1;
      } else {
         int var4 = 0;

         PointF var5;
         while(true) {
            var5 = var1;
            if (var4 >= this.mKeyChangeHistory.size() - 1) {
               break;
            }

            LeanbackKeyboardController.KeyChange var6 = (LeanbackKeyboardController.KeyChange)this.mKeyChangeHistory.get(var4);
            if (var2 - ((LeanbackKeyboardController.KeyChange)this.mKeyChangeHistory.get(var4 + 1)).time < 100L) {
               var5 = var6.position;
               this.mKeyChangeHistory.clear();
               this.mKeyChangeHistory.add(new LeanbackKeyboardController.KeyChange(var2, var5));
               break;
            }

            ++var4;
         }

         return var5;
      }
   }

   private PointF getCurrentKeyPosition() {
      if (this.mContainer != null) {
         LeanbackKeyboardContainer.KeyFocus var1 = this.mContainer.getCurrFocus();
         return new PointF((float)var1.rect.centerX(), (float)var1.rect.centerY());
      } else {
         return null;
      }
   }

   private PointF getRelativePosition(View var1, MotionEvent var2) {
      int[] var5 = new int[2];
      var1.getLocationOnScreen(var5);
      float var3 = var2.getRawX();
      float var4 = var2.getRawY();
      return new PointF(var3 - (float)var5[0], var4 - (float)var5[1]);
   }

   private int getSimplifiedKey(int var1) {
      int var2 = 23;
      if (var1 != 23) {
         byte var3 = 66;
         var2 = var3;
         if (var1 != 66) {
            var2 = var3;
            if (var1 != 160) {
               var2 = var1;
               if (var1 == 96) {
                  var2 = var3;
               }
            }
         }
      }

      var1 = var2;
      if (var2 == 97) {
         var1 = 4;
      }

      return var1;
   }

   private void handleCommitKeyboardKey(int var1, CharSequence var2) {
      switch(var1) {
      case -8:
         this.mContainer.dismissMiniKeyboard();
         return;
      case -7:
         this.mContainer.startVoiceRecording();
         return;
      case -6:
         this.mContainer.onShiftDoubleClick(this.mContainer.isCapsLockOn());
         return;
      case -5:
         this.mInputListener.onEntry(1, 0, (CharSequence)null);
         return;
      case -4:
         this.mInputListener.onEntry(4, 0, (CharSequence)null);
         return;
      case -3:
         this.mInputListener.onEntry(3, 0, (CharSequence)null);
         return;
      case -2:
         if (Log.isLoggable("LbKbController", Log.DEBUG)) {
            Log.d("LbKbController", "mode change");
         }

         this.mContainer.onModeChangeClick();
         return;
      case -1:
         if (Log.isLoggable("LbKbController", Log.DEBUG)) {
            Log.d("LbKbController", "shift");
         }

         this.mContainer.onShiftClick();
         return;
      case 32:
         this.mInputListener.onEntry(0, var1, " ");
         this.mContainer.onSpaceEntry();
         return;
      case 46:
         this.mInputListener.onEntry(0, var1, var2);
         this.mContainer.onPeriodEntry();
         return;
      default:
         this.mInputListener.onEntry(0, var1, var2);
         this.mContainer.onTextEntry();
         if (this.mContainer.isMiniKeyboardOnScreen()) {
            this.mContainer.dismissMiniKeyboard();
         }

      }
   }

   private boolean handleKeyDownEvent(int var1, int var2) {
      var1 = this.getSimplifiedKey(var1);
      boolean var3;
      boolean var4;
      if (var1 == 4) {
         this.mContainer.cancelVoiceRecording();
         var3 = false;
      } else if (this.mContainer.isVoiceVisible()) {
         if (var1 == 22 || var1 == 23 || var1 == 66) {
            this.mContainer.cancelVoiceRecording();
         }

         var3 = true;
      } else {
         var4 = true;
         var3 = var4;
         switch(var1) {
         case 19:
            var3 = this.onDirectionalMove(8);
            break;
         case 20:
            var3 = this.onDirectionalMove(2);
            break;
         case 21:
            var3 = this.onDirectionalMove(1);
            break;
         case 22:
            var3 = this.onDirectionalMove(4);
            break;
         case 23:
         case 66:
            if (var2 == 0) {
               this.mMoveCount = 0;
               this.mKeyDownKeyFocus = new LeanbackKeyboardContainer.KeyFocus();
               this.mKeyDownKeyFocus.set(this.mContainer.getCurrFocus());
            } else if (var2 == 1 && this.handleKeyLongPress(var1)) {
               this.mKeyDownKeyFocus = null;
            }

            var3 = var4;
            if (this.isKeyHandledOnKeyDown(this.mContainer.getCurrKeyCode())) {
               this.commitKey();
               var3 = var4;
            }
            break;
         case 99:
            this.handleCommitKeyboardKey(-5, (CharSequence)null);
            var3 = var4;
            break;
         case 100:
            this.handleCommitKeyboardKey(32, (CharSequence)null);
            var3 = var4;
            break;
         case 102:
            this.handleCommitKeyboardKey(-3, (CharSequence)null);
            var3 = var4;
            break;
         case 103:
            this.handleCommitKeyboardKey(-4, (CharSequence)null);
            var3 = var4;
         case 106:
         case 107:
            break;
         default:
            var3 = false;
         }
      }

      var4 = var3;
      if (!var3) {
         var4 = this.applyLETVFixesDown(var1);
      }

      return var4;
   }

   private boolean handleKeyLongPress(int var1) {
      boolean var2;
      if (this.isEnterKey(var1) && this.mContainer.onKeyLongPress()) {
         var2 = true;
      } else {
         var2 = false;
      }

      this.mLongPressHandled = var2;
      if (this.mContainer.isMiniKeyboardOnScreen()) {
         Log.d("LbKbController", "mini keyboard shown after long press");
      }

      return this.mLongPressHandled;
   }

   private boolean handleKeyUpEvent(int var1, long var2) {
      var1 = this.getSimplifiedKey(var1);
      boolean var4;
      boolean var5;
      if (var1 == 4) {
         var4 = false;
      } else if (this.mContainer.isVoiceVisible()) {
         var4 = true;
      } else {
         var5 = true;
         var4 = var5;
         switch(var1) {
         case 19:
         case 20:
         case 21:
         case 22:
            this.clearKeyIfNecessary();
            var4 = var5;
            break;
         case 23:
         case 66:
            if (this.mContainer.getCurrKeyCode() == -1) {
               this.mDoubleClickDetector.addEvent(var2);
               var4 = var5;
            } else {
               var4 = var5;
               if (!this.isKeyHandledOnKeyDown(this.mContainer.getCurrKeyCode())) {
                  this.commitKey(this.mKeyDownKeyFocus);
                  var4 = var5;
               }
            }
         case 99:
         case 100:
         case 102:
         case 103:
            break;
         case 106:
            this.handleCommitKeyboardKey(-2, (CharSequence)null);
            var4 = var5;
            break;
         case 107:
            this.handleCommitKeyboardKey(-6, (CharSequence)null);
            var4 = var5;
            break;
         default:
            var4 = false;
         }
      }

      var5 = var4;
      if (!var4) {
         var5 = this.applyLETVFixesUp(var1);
      }

      return var5;
   }

   private void initInputView() {
      this.mContainer.onInitInputView();
      this.updatePositionToCurrentFocus();
   }

   private boolean isCallAllowed(int var1) {
      long var2 = System.currentTimeMillis();
      if (this.prevTime != 0L && var2 - this.prevTime <= (long)(var1 * 3)) {
         if (var2 - this.prevTime > (long)var1) {
            this.prevTime = 0L;
            return true;
         }
      } else {
         this.prevTime = var2;
      }

      return false;
   }

   private boolean isDoubleClick() {
      long var1 = System.currentTimeMillis();
      long var3 = this.lastClickTime;
      if (this.lastClickTime != 0L && var1 - var3 <= (long)300) {
         return true;
      } else {
         this.lastClickTime = var1;
         return false;
      }
   }

   private boolean isEnterKey(int var1) {
      var1 = this.getSimplifiedKey(var1);
      return var1 == 23 || var1 == 66;
   }

   private boolean isKeyHandledOnKeyDown(int var1) {
      return var1 == -5 || var1 == -3 || var1 == -4;
   }

   private void moveSelectorToPoint(float var1, float var2) {
      LeanbackKeyboardContainer var3 = this.mContainer;
      LeanbackKeyboardContainer.KeyFocus var4 = this.mTempFocus;
      var3.getBestFocus(new Float(var1), new Float(var2), var4);
      this.mContainer.setFocus(this.mTempFocus);
      var3 = this.mContainer;
      Rect var5 = this.mTempFocus.rect;
      var3.alignSelector((float)var5.centerX(), (float)var5.centerY(), false);
   }

   private boolean onDirectionalMove(int var1) {
      if (this.mContainer.getNextFocusInDirection(var1, this.mDownFocus, this.mTempFocus)) {
         this.mContainer.setFocus(this.mTempFocus);
         this.mDownFocus.set(this.mTempFocus);
         this.clearKeyIfNecessary();
      }

      return true;
   }

   private void performBestSnap(long var1) {
      LeanbackKeyboardContainer.KeyFocus var3 = this.mContainer.getCurrFocus();
      this.mTempPoint.x = (float)var3.rect.centerX();
      this.mTempPoint.y = (float)var3.rect.centerY();
      PointF var4 = this.getBestSnapPosition(this.mTempPoint, var1);
      this.mContainer.getBestFocus(var4.x, var4.y, this.mTempFocus);
      this.mContainer.setFocus(this.mTempFocus);
      this.updatePositionToCurrentFocus();
   }

   private void setKeyState(int var1, boolean var2) {
      LeanbackKeyboardContainer var3 = this.mContainer;
      LeanbackKeyboardContainer.KeyFocus var4 = var3.getCurrFocus();
      var4.index = var1;
      var4.type = 0;
      byte var5;
      if (var2) {
         var5 = 3;
      } else {
         var5 = 0;
      }

      var3.setTouchState(var5);
   }

   private void updatePositionToCurrentFocus() {
      PointF var1 = this.getCurrentKeyPosition();
      if (var1 != null && this.mSpaceTracker != null) {
         this.mSpaceTracker.setPixelPosition(var1.x, var1.y);
      }

   }

   public boolean areSuggestionsEnabled() {
      return this.mContainer != null ? this.mContainer.areSuggestionsEnabled() : false;
   }

   public boolean enableAutoEnterSpace() {
      return this.mContainer != null ? this.mContainer.enableAutoEnterSpace() : false;
   }

   public View getView() {
      if (this.mContainer != null) {
         RelativeLayout var1 = this.mContainer.getView();
         var1.setClickable(true);
         var1.setOnTouchListener(this);
         var1.setOnHoverListener(this);
         Button var2 = this.mContainer.getGoButton();
         var2.setOnTouchListener(this);
         var2.setOnHoverListener(this);
         var2.setTag("Go");
         return var1;
      } else {
         return null;
      }
   }

   public void onDismiss(boolean var1) {
      if (var1) {
         this.mInputListener.onEntry(8, 0, (CharSequence)null);
      } else {
         this.mInputListener.onEntry(7, 0, (CharSequence)null);
      }
   }

   public boolean onGenericMotionEvent(MotionEvent var1) {
      return this.mSpaceTracker != null && this.mContext != null && this.mContext.isInputViewShown() && this.mSpaceTracker.onGenericMotionEvent(var1);
   }

   public boolean onHover(View var1, MotionEvent var2) {
      boolean var4 = this.isCallAllowed(300);
      boolean var3 = var4;
      if (var4) {
         if (var2.getAction() == 7) {
            PointF var5 = this.getRelativePosition(this.mContainer.getView(), var2);
            this.moveSelectorToPoint(var5.x, var5.y);
         }

         var3 = true;
      }

      return var3;
   }

   public boolean onKeyDown(int var1, KeyEvent var2) {
      this.mDownFocus.set(this.mContainer.getCurrFocus());
      if (this.mSpaceTracker != null && this.mSpaceTracker.onKeyDown(var1, var2)) {
         return true;
      } else {
         if (this.isEnterKey(var1)) {
            this.mKeyDownReceived = true;
            if (var2.getRepeatCount() == 0) {
               this.mContainer.setTouchState(3);
            }
         }

         return this.handleKeyDownEvent(var1, var2.getRepeatCount());
      }
   }

   public boolean onKeyUp(int var1, KeyEvent var2) {
      if (this.mSpaceTracker != null && this.mSpaceTracker.onKeyUp(var1, var2)) {
         return true;
      } else {
         if (this.isEnterKey(var1)) {
            if (!this.mKeyDownReceived || this.mLongPressHandled) {
               this.mLongPressHandled = false;
               return true;
            }

            this.mKeyDownReceived = false;
            if (this.mContainer.getTouchState() == 3) {
               this.mContainer.setTouchState(1);
            }
         }

         return this.handleKeyUpEvent(var1, var2.getEventTime());
      }
   }

   public void onStartInput(EditorInfo var1) {
      if (this.mContainer != null) {
         this.mContainer.onStartInput(var1);
         this.initInputView();
      }

   }

   public void onStartInputView() {
      this.mKeyDownReceived = false;
      if (this.mContainer != null) {
         this.mContainer.onStartInputView();
      }

      this.mDoubleClickDetector.reset();
   }

   public boolean onTouch(View var1, MotionEvent var2) {
      Object var3 = var1.getTag();
      if (var3 != null && "Go".equals(var3)) {
         this.fakeKeyIndex(0, 2);
      } else {
         switch(var2.getAction()) {
         case 0:
            this.moveSelectorToPoint(var2.getX(), var2.getY());
            this.fakeClickDown();
            this.beginLongClickCountdown();
            break;
         case 1:
            if (!this.clickConsumed) {
               this.clickConsumed = true;
               if (this.isDoubleClick()) {
                  this.mContainer.onKeyLongPress();
                  break;
               }

               this.fakeClickUp();
            }

            this.fakeLongClickUp();
            break;
         default:
            return false;
         }
      }

      return true;
   }

   public void onVoiceResult(String var1) {
      this.mInputListener.onEntry(6, 0, var1);
   }

   public void run() {
      if (!this.clickConsumed) {
         this.clickConsumed = true;
         this.fakeLongClickDown();
      }
   }

   public void setKeyboardContainer(LeanbackKeyboardContainer var1) {
      this.mContainer = var1;
      var1.getView().addOnLayoutChangeListener(this.mOnLayoutChangeListener);
   }

   public void setSpaceTracker(TouchNavSpaceTracker var1) {
      this.mSpaceTracker = var1;
      var1.setLPFEnabled(true);
      var1.setKeyEventListener(this.mTouchEventListener);
   }

   public void updateAddonKeyboard() {
      this.mContainer.updateAddonKeyboard();
   }

   public void updateSuggestions(ArrayList<String> var1) {
      if (this.mContainer != null) {
         this.mContainer.updateSuggestions(var1);
      }

   }

   private class DoubleClickDetector {
      final long DOUBLE_CLICK_TIMEOUT_MS;
      boolean mFirstClickShiftLocked;
      long mFirstClickTime;

      private DoubleClickDetector() {
         this.DOUBLE_CLICK_TIMEOUT_MS = 200L;
         this.mFirstClickTime = 0L;
      }

      public void addEvent(long var1) {
         if (var1 - this.mFirstClickTime > 200L) {
            this.mFirstClickTime = var1;
            this.mFirstClickShiftLocked = LeanbackKeyboardController.this.mContainer.isCapsLockOn();
            LeanbackKeyboardController.this.commitKey();
         } else {
            LeanbackKeyboardController.this.mContainer.onShiftDoubleClick(this.mFirstClickShiftLocked);
            this.reset();
         }
      }

      public void reset() {
         this.mFirstClickTime = 0L;
      }
   }

   public interface InputListener {
      int ENTRY_TYPE_ACTION = 5;
      int ENTRY_TYPE_BACKSPACE = 1;
      int ENTRY_TYPE_DISMISS = 7;
      int ENTRY_TYPE_LEFT = 3;
      int ENTRY_TYPE_RIGHT = 4;
      int ENTRY_TYPE_STRING = 0;
      int ENTRY_TYPE_SUGGESTION = 2;
      int ENTRY_TYPE_VOICE = 6;
      int ENTRY_TYPE_VOICE_DISMISS = 8;

      void onEntry(int var1, int var2, CharSequence var3);
   }

   private static final class KeyChange {
      public PointF position;
      public long time;

      public KeyChange(long var1, PointF var3) {
         this.time = var1;
         this.position = var3;
      }
   }

   private class TouchEventListener implements TouchNavSpaceTracker.KeyEventListener {
      private TouchEventListener() {
      }

      public boolean onKeyDown(int var1, KeyEvent var2) {
         if (LeanbackKeyboardController.this.isEnterKey(var1)) {
            LeanbackKeyboardController.this.mKeyDownReceived = true;
            if (var2.getRepeatCount() == 0) {
               LeanbackKeyboardController.this.mContainer.setTouchState(3);
               LeanbackKeyboardController.this.mSpaceTracker.blockMovementUntil(var2.getEventTime() + 500L);
               LeanbackKeyboardController.this.performBestSnap(var2.getEventTime());
            }
         }

         return LeanbackKeyboardController.this.handleKeyDownEvent(var1, var2.getRepeatCount());
      }

      public boolean onKeyLongPress(int var1, KeyEvent var2) {
         return LeanbackKeyboardController.this.handleKeyLongPress(var1);
      }

      public boolean onKeyUp(int var1, KeyEvent var2) {
         if (LeanbackKeyboardController.this.isEnterKey(var1)) {
            if (!LeanbackKeyboardController.this.mKeyDownReceived || LeanbackKeyboardController.this.mLongPressHandled) {
               LeanbackKeyboardController.this.mLongPressHandled = false;
               return true;
            }

            LeanbackKeyboardController.this.mKeyDownReceived = false;
            if (LeanbackKeyboardController.this.mContainer.getTouchState() == 3) {
               LeanbackKeyboardController.this.mContainer.setTouchState(1);
               LeanbackKeyboardController.this.mSpaceTracker.unblockMovement();
            }
         }

         return LeanbackKeyboardController.this.handleKeyUpEvent(var1, var2.getEventTime());
      }
   }
}
