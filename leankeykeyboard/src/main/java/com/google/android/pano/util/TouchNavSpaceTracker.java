package com.google.android.pano.util;

import android.animation.TimeInterpolator;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;

public class TouchNavSpaceTracker {
   private static final boolean DEBUG = false;
   public static final float DEFAULT_DAMPED_SENSITIVITY = 0.5F;
   public static final long DEFAULT_DAMPENING_DURATION_MS = 200L;
   public static final long DEFAULT_DAMPING_DURATION_MS = 200L;
   public static final float DEFAULT_HORIZONTAL_SIZE_MM = 120.0F;
   public static final float DEFAULT_LPF_COEFF = 0.25F;
   public static final float DEFAULT_MAX_FLICK_DISTANCE_MM = 40.0F;
   public static final long DEFAULT_MAX_FLICK_DURATION_MS = 250L;
   public static final float DEFAULT_MIN_FLICK_DISTANCE_MM = 4.0F;
   public static final float DEFAULT_SENSITIVITY = 1.0F;
   public static final float DEFAULT_VERTICAL_SIZE_MM = 50.0F;
   private static final int[] DIRECTIONS = new int[]{1, 3, 2, 6, 4, 12, 8, 9, 1};
   private static final float[] DIRECTION_BOUNDARIES = new float[]{-2.7488935F, -1.9634954F, -1.1780972F, -0.3926991F, 0.3926991F, 1.1780972F, 1.9634954F, 2.7488935F};
   public static final int DIRECTION_DOWN = 2;
   public static final int DIRECTION_DOWN_LEFT = 3;
   public static final int DIRECTION_DOWN_RIGHT = 6;
   public static final int DIRECTION_LEFT = 1;
   public static final int DIRECTION_RIGHT = 4;
   public static final int DIRECTION_UP = 8;
   public static final int DIRECTION_UP_LEFT = 9;
   public static final int DIRECTION_UP_RIGHT = 12;
   private static final int MSG_LONG_CLICK = 0;
   private static final String TAG = "TouchNavSpaceTracker";
   private float mDampedSensitivity;
   private float mDampingDuration;
   private float mFlickMaxDistance;
   private long mFlickMaxDuration;
   private float mFlickMaxSquared;
   private float mFlickMinDistance;
   private float mFlickMinSquared;
   private Handler mHandler;
   protected TouchNavSpaceTracker.KeyEventListener mKeyEventListener;
   private float mLPFCurrX;
   private float mLPFCurrY;
   private boolean mLPFEnabled;
   private long mMovementBlockTime;
   private float mPhysicalHeight;
   private PointF mPhysicalPosition;
   private float mPhysicalWidth;
   private float mPixelHeight;
   protected TouchNavSpaceTracker.TouchEventListener mPixelListener;
   private float mPixelWidth;
   private float mPixelsPerMm;
   private PointF mPrevPhysPosition;
   private float mSensitivity;
   private TimeInterpolator mSensitivityInterpolator;
   protected final SparseArray<TouchNavMotionTracker> mTouchParams;
   private float mUnscaledFlickMaxDistance;
   private float mUnscaledFlickMinDistance;
   private boolean mWasBlocked;

   public TouchNavSpaceTracker() {
      this((TouchNavSpaceTracker.KeyEventListener)null, (TouchNavSpaceTracker.TouchEventListener)null);
   }

   public TouchNavSpaceTracker(TouchNavSpaceTracker.KeyEventListener var1, TouchNavSpaceTracker.TouchEventListener var2) {
      this.mPrevPhysPosition = new PointF(Float.MIN_VALUE, Float.MIN_VALUE);
      this.mPhysicalPosition = new PointF(Float.MIN_VALUE, Float.MIN_VALUE);
      this.mWasBlocked = false;
      this.mSensitivityInterpolator = new AccelerateInterpolator();
      this.mDampingDuration = 200.0F;
      this.mDampedSensitivity = 0.5F;
      this.mSensitivity = 1.0F;
      this.mUnscaledFlickMinDistance = 4.0F;
      this.mUnscaledFlickMaxDistance = 40.0F;
      this.mFlickMinDistance = this.mSensitivity * 4.0F;
      this.mFlickMaxDistance = this.mSensitivity * 40.0F;
      this.mFlickMinSquared = this.mFlickMinDistance * this.mFlickMinDistance;
      this.mFlickMaxSquared = this.mFlickMaxDistance * this.mFlickMaxDistance;
      this.mFlickMaxDuration = 250L;
      this.mLPFEnabled = false;
      this.mHandler = new Handler() {
         public void handleMessage(Message var1) {
            switch(var1.what) {
            case 0:
               if (TouchNavSpaceTracker.this.mKeyEventListener != null) {
                  TouchNavSpaceTracker.this.mKeyEventListener.onKeyLongPress(var1.arg1, (KeyEvent)var1.obj);
                  return;
               }
            default:
            }
         }
      };
      this.mKeyEventListener = var1;
      this.mPixelListener = var2;
      this.mTouchParams = new SparseArray(1);
      this.mPhysicalWidth = 120.0F;
      this.mPhysicalHeight = 50.0F;
      this.mPixelWidth = 0.0F;
      this.mPixelHeight = 0.0F;
      this.mPixelsPerMm = 0.0F;
   }

   private float calculateSensitivity(MotionEvent var1, MotionEvent var2) {
      long var4 = var1.getEventTime() - var2.getEventTime();
      float var3;
      if (var1.getEventTime() < this.mMovementBlockTime) {
         var3 = 0.0F;
         this.mWasBlocked = true;
      } else if ((float)var4 < this.mDampingDuration) {
         var3 = this.mSensitivityInterpolator.getInterpolation((float)var4 / this.mDampingDuration);
         var3 = this.mDampedSensitivity + (this.mSensitivity - this.mDampedSensitivity) * var3;
      } else {
         var3 = this.mSensitivity;
      }

      if (var3 != 0.0F && this.mWasBlocked) {
         this.mWasBlocked = false;
         this.setPhysicalPosition(this.mPhysicalPosition.x, this.mPhysicalPosition.y);
      }

      return var3;
   }

   private void checkForLongClick(int var1, KeyEvent var2) {
      if (var1 == 23) {
         Message var3 = this.mHandler.obtainMessage(0);
         var3.arg1 = var1;
         var3.obj = var2;
         if (!this.mHandler.hasMessages(0)) {
            this.mHandler.sendMessageDelayed(var3, (long)ViewConfiguration.getLongPressTimeout());
            return;
         }
      }

   }

   private void clampPosition() {
      if (this.mPhysicalPosition.x < 0.0F) {
         this.setPhysicalPosition(0.0F, this.mPhysicalPosition.y);
      } else if (this.mPhysicalPosition.x > this.mPhysicalWidth) {
         this.setPhysicalPosition(this.mPhysicalWidth, this.mPhysicalPosition.y);
      }

      if (this.mPhysicalPosition.y < 0.0F) {
         this.setPhysicalPosition(this.mPhysicalPosition.x, 0.0F);
      } else if (this.mPhysicalPosition.y > this.mPhysicalHeight) {
         this.setPhysicalPosition(this.mPhysicalPosition.x, this.mPhysicalHeight);
         return;
      }

   }

   private int getDpadDirection(float var1, float var2) {
      var1 = (float)Math.atan2((double)(-var2), (double)var1);

      int var3;
      for(var3 = 0; var3 < DIRECTION_BOUNDARIES.length && var1 >= DIRECTION_BOUNDARIES[var3]; ++var3) {
         ;
      }

      return DIRECTIONS[var3];
   }

   private float getPhysicalX(float var1) {
      return this.mPixelWidth <= 0.0F ? 0.0F : this.mPhysicalWidth * var1 / this.mPixelWidth;
   }

   private float getPhysicalY(float var1) {
      return this.mPixelHeight <= 0.0F ? 0.0F : this.mPhysicalHeight * var1 / this.mPixelHeight;
   }

   private float getPixelX(float var1) {
      return this.mPixelWidth * var1 / this.mPhysicalWidth;
   }

   private float getPixelY(float var1) {
      return this.mPixelHeight * var1 / this.mPhysicalHeight;
   }

   private int getPrimaryDpadDirection(float var1, float var2) {
      if (Math.abs(var1) > Math.abs(var2)) {
         return var1 > 0.0F ? 4 : 1;
      } else {
         return var2 > 0.0F ? 2 : 8;
      }
   }

   private float getScaledValue(float var1, float var2) {
      return var1 * var2;
   }

   private TouchNavMotionTracker getTrackerForDevice(InputDevice var1) {
      TouchNavMotionTracker var3 = (TouchNavMotionTracker)this.mTouchParams.get(var1.getId());
      TouchNavMotionTracker var2 = var3;
      if (var3 == null) {
         var2 = TouchNavMotionTracker.buildTrackerForDevice(var1, 0.1F);
         this.mTouchParams.put(var1.getId(), var2);
      }

      return var2;
   }

   private void setPhysicalSizeInternal(float var1, float var2) {
      this.mPhysicalWidth = var1;
      this.mPhysicalHeight = var2;
      if (this.mPhysicalPosition.x > this.mPhysicalWidth) {
         this.mPhysicalPosition.x = this.mPhysicalWidth;
      }

      if (this.mPhysicalPosition.y > this.mPhysicalHeight) {
         this.mPhysicalPosition.y = this.mPhysicalHeight;
      }

   }

   private void updatePhysicalSize() {
      if (this.mPixelWidth > 0.0F && this.mPixelHeight > 0.0F && this.mPixelsPerMm > 0.0F) {
         this.setPhysicalSizeInternal(this.mPixelWidth / this.mPixelsPerMm, this.mPixelHeight / this.mPixelsPerMm);
      }

   }

   public void blockMovementUntil(long var1) {
      this.mMovementBlockTime = var1;
   }

   public void configureDamping(float var1, long var2) {
      this.mDampedSensitivity = var1;
      this.mDampingDuration = (float)var2;
   }

   public void configureFlicks(float var1, float var2, long var3) {
      this.mUnscaledFlickMinDistance = var1;
      this.mUnscaledFlickMaxDistance = var2;
      this.mFlickMinDistance = this.mSensitivity * var1;
      this.mFlickMaxDistance = this.mSensitivity * var2;
      this.mFlickMinSquared = this.mFlickMinDistance * this.mFlickMinDistance;
      this.mFlickMaxSquared = this.mFlickMaxDistance * this.mFlickMaxDistance;
      this.mFlickMaxDuration = var3;
   }

   public PointF getCurrentPhysicalPosition() {
      return new PointF(this.mPhysicalPosition.x, this.mPhysicalPosition.y);
   }

   public PointF getCurrentPixelPosition() {
      return new PointF(this.getPixelX(this.mPhysicalPosition.x), this.getPixelY(this.mPhysicalPosition.y));
   }

   public boolean onGenericMotionEvent(MotionEvent var1) {
      if (var1 != null && (var1.getSource() & 2097152) == 2097152) {
         InputDevice var13 = var1.getDevice();
         if (var13 == null) {
            return false;
         }

         TouchNavMotionTracker var19 = this.getTrackerForDevice(var13);
         int var10 = var1.getActionMasked();
         var19.addMovement(var1);
         boolean var6;
         if ((var10 & 255) == 6) {
            var6 = true;
         } else {
            var6 = false;
         }

         int var7;
         if (var6) {
            var7 = var1.getActionIndex();
         } else {
            var7 = -1;
         }

         float var3 = 0.0F;
         float var2 = 0.0F;
         int var9 = var1.getPointerCount();

         for(int var8 = 0; var8 < var9; ++var8) {
            if (var7 != var8) {
               var3 += var1.getX(var8);
               var2 += var1.getY(var8);
            }
         }

         int var17;
         if (var6) {
            var17 = var9 - 1;
         } else {
            var17 = var9;
         }

         float var4 = var3 / (float)var17;
         float var5 = var2 / (float)var17;
         TouchNavSpaceTracker.PhysicalMotionEvent var14 = new TouchNavSpaceTracker.PhysicalMotionEvent(var1.getDeviceId(), var19.getPhysicalX(var4), var19.getPhysicalX(var5), var1.getEventTime());
         boolean var18 = false;
         boolean var12 = false;
         boolean var11;
         MotionEvent var15;
         TouchNavSpaceTracker.PhysicalMotionEvent var16;
         switch(var10 & 255) {
         case 0:
            if (this.mLPFEnabled) {
               this.mLPFCurrX = var4;
               this.mLPFCurrY = var5;
            }

            var19.setNewValues(var4, var5);
            var19.updatePrevValues();
            var19.setDownEvent(MotionEvent.obtain(var1));
            if (this.mPixelListener != null) {
               return false | this.mPixelListener.onDown(var14);
            }
            break;
         case 1:
            var15 = var19.getDownEvent();
            if (var15 == null) {
               Log.w("TouchNavSpaceTracker", "Up event without down event");
               return false | this.mPixelListener.onUp(var14, this.getPixelX(this.mPhysicalPosition.x), this.getPixelY(this.mPhysicalPosition.y));
            }

            var16 = new TouchNavSpaceTracker.PhysicalMotionEvent(var1.getDeviceId(), var19.getPhysicalX(var15.getX()), var19.getPhysicalY(var15.getY()), var15.getEventTime());
            var6 = var18;
            if (var19.computeVelocity()) {
               var6 = var18;
               if (this.mPixelListener != null) {
                  var2 = this.getPixelX(var19.getPhysicalX(var19.getXVel()));
                  var3 = this.getPixelY(var19.getPhysicalY(var19.getYVel()));
                  var18 = false | this.mPixelListener.onFling(var16, var14, var2, var3);
                  var6 = var18;
                  if (var14.getTime() - var16.getTime() < this.mFlickMaxDuration) {
                     var2 = var14.getX() - var16.getX();
                     var3 = var14.getY() - var16.getY();
                     var4 = var2 * var2 + var3 * var3;
                     var6 = var18;
                     if (var4 > this.mFlickMinSquared) {
                        var6 = var18;
                        if (var4 < this.mFlickMaxSquared) {
                           this.mPixelListener.onFlick(var16, var14, this.getDpadDirection(var2, var3), this.getPrimaryDpadDirection(var2, var3));
                           var6 = var18;
                        }
                     }
                  }
               }
            }

            var2 = this.getPixelX(this.mPhysicalPosition.x);
            var3 = this.getPixelY(this.mPhysicalPosition.y);
            var11 = this.mPixelListener.onUp(var14, var2, var3);
            var19.clear();
            return var6 | var11;
         case 2:
            if (var19.getDownEvent() == null) {
               var19.setDownEvent(MotionEvent.obtain(var1));
               if (this.mLPFEnabled) {
                  this.mLPFCurrX = var4;
                  this.mLPFCurrY = var5;
               }
            }

            var3 = var4;
            var2 = var5;
            if (this.mLPFEnabled) {
               this.mLPFCurrX = this.mLPFCurrX * 0.75F + 0.25F * var4;
               this.mLPFCurrY = this.mLPFCurrY * 0.75F + 0.25F * var5;
               var3 = this.mLPFCurrX;
               var2 = this.mLPFCurrY;
            }

            if (var19.setNewValues(var3, var2)) {
               var2 = var19.getPhysicalX(var19.getScrollX());
               var3 = var19.getPhysicalY(var19.getScrollY());
               var4 = this.calculateSensitivity(var1, var19.getDownEvent());
               this.mPhysicalPosition.x = this.mPrevPhysPosition.x + this.getScaledValue(var2, var4);
               this.mPhysicalPosition.y = this.mPrevPhysPosition.y + this.getScaledValue(var3, var4);
               this.clampPosition();
               if (!this.mPhysicalPosition.equals(this.mPrevPhysPosition)) {
                  var11 = var12;
                  if (this.mPixelListener != null) {
                     var11 = var12;
                     if (this.mPixelHeight > 0.0F) {
                        var11 = var12;
                        if (this.mPixelWidth > 0.0F) {
                           var15 = var19.getDownEvent();
                           var16 = new TouchNavSpaceTracker.PhysicalMotionEvent(var1.getDeviceId(), var19.getPhysicalX(var15.getX()), var19.getPhysicalY(var15.getY()), var15.getEventTime());
                           var2 = this.getPixelX(this.mPhysicalPosition.x);
                           var3 = this.getPixelY(this.mPhysicalPosition.y);
                           var11 = false | this.mPixelListener.onMove(var16, var14, var2, var3);
                        }
                     }
                  }

                  this.mPrevPhysPosition.set(this.mPhysicalPosition);
               } else {
                  var11 = false | true;
               }

               var19.updatePrevValues();
               return var11;
            }

            return false | true;
         case 3:
            var19.clear();
            return false;
         default:
            return false;
         }
      }

      return false;
   }

   public boolean onKeyDown(int var1, KeyEvent var2) {
      if (var2 != null && var2.getDevice() != null && (var2.getDevice().getSources() & 2097152) == 2097152) {
         if (var2.getRepeatCount() == 0) {
            this.checkForLongClick(var1, var2);
         }

         if (this.mKeyEventListener != null) {
            return this.mKeyEventListener.onKeyDown(var1, var2);
         }
      }

      return false;
   }

   public boolean onKeyUp(int var1, KeyEvent var2) {
      if (var2 != null && var2.getDevice() != null && (var2.getDevice().getSources() & 2097152) == 2097152) {
         if (var1 == 23) {
            this.mHandler.removeMessages(0);
         }

         if (this.mKeyEventListener != null) {
            return this.mKeyEventListener.onKeyUp(var1, var2);
         }
      }

      return false;
   }

   public void onPause() {
      this.mHandler.removeMessages(0);
   }

   public void setKeyEventListener(TouchNavSpaceTracker.KeyEventListener var1) {
      this.mKeyEventListener = var1;
   }

   public void setLPFEnabled(boolean var1) {
      this.mLPFEnabled = var1;
   }

   public void setPhysicalDensity(float var1) {
      this.mPixelsPerMm = var1;
      if (var1 > 0.0F) {
         this.updatePhysicalSize();
      }

   }

   public void setPhysicalPosition(float var1, float var2) {
      this.mPhysicalPosition.x = var1;
      this.mPhysicalPosition.y = var2;
      this.mPrevPhysPosition.x = var1;
      this.mPrevPhysPosition.y = var2;
      this.clampPosition();
   }

   public void setPhysicalSize(float var1, float var2) {
      if (this.mPixelsPerMm <= 0.0F) {
         this.setPhysicalSizeInternal(var1, var2);
      }
   }

   public void setPixelPosition(float var1, float var2) {
      this.setPhysicalPosition(this.getPhysicalX(var1), this.getPhysicalY(var2));
   }

   public void setPixelSize(float var1, float var2) {
      this.mPixelHeight = var2;
      this.mPixelWidth = var1;
      this.updatePhysicalSize();
   }

   public void setSensitivity(float var1) {
      this.mSensitivity = var1;
      this.configureFlicks(this.mUnscaledFlickMinDistance, this.mUnscaledFlickMaxDistance, this.mFlickMaxDuration);
   }

   public void setTouchEventListener(TouchNavSpaceTracker.TouchEventListener var1) {
      this.mPixelListener = var1;
   }

   public void unblockMovement() {
      this.mMovementBlockTime = 0L;
   }

   public interface KeyEventListener {
      boolean onKeyDown(int var1, KeyEvent var2);

      boolean onKeyLongPress(int var1, KeyEvent var2);

      boolean onKeyUp(int var1, KeyEvent var2);
   }

   public static class PhysicalMotionEvent {
      private final int mDeviceId;
      private final long mTime;
      // $FF: renamed from: mX float
      private final float field_6;
      // $FF: renamed from: mY float
      private final float field_7;

      public PhysicalMotionEvent(int var1, float var2, float var3, long var4) {
         this.mDeviceId = var1;
         this.field_6 = var2;
         this.field_7 = var3;
         this.mTime = var4;
      }

      public final InputDevice getDevice() {
         return InputDevice.getDevice(this.getDeviceId());
      }

      public final int getDeviceId() {
         return this.mDeviceId;
      }

      public final long getTime() {
         return this.mTime;
      }

      public final float getX() {
         return this.field_6;
      }

      public final float getY() {
         return this.field_7;
      }
   }

   public static class SimpleTouchEventListener implements TouchNavSpaceTracker.KeyEventListener, TouchNavSpaceTracker.TouchEventListener {
      public boolean onDown(TouchNavSpaceTracker.PhysicalMotionEvent var1) {
         return false;
      }

      public boolean onFlick(TouchNavSpaceTracker.PhysicalMotionEvent var1, TouchNavSpaceTracker.PhysicalMotionEvent var2, int var3, int var4) {
         return false;
      }

      public boolean onFling(TouchNavSpaceTracker.PhysicalMotionEvent var1, TouchNavSpaceTracker.PhysicalMotionEvent var2, float var3, float var4) {
         return false;
      }

      public boolean onKeyDown(int var1, KeyEvent var2) {
         return false;
      }

      public boolean onKeyLongPress(int var1, KeyEvent var2) {
         return false;
      }

      public boolean onKeyUp(int var1, KeyEvent var2) {
         return false;
      }

      public boolean onMove(TouchNavSpaceTracker.PhysicalMotionEvent var1, TouchNavSpaceTracker.PhysicalMotionEvent var2, float var3, float var4) {
         return false;
      }

      public boolean onUp(TouchNavSpaceTracker.PhysicalMotionEvent var1, float var2, float var3) {
         return false;
      }
   }

   public interface TouchEventListener {
      boolean onDown(TouchNavSpaceTracker.PhysicalMotionEvent var1);

      boolean onFlick(TouchNavSpaceTracker.PhysicalMotionEvent var1, TouchNavSpaceTracker.PhysicalMotionEvent var2, int var3, int var4);

      boolean onFling(TouchNavSpaceTracker.PhysicalMotionEvent var1, TouchNavSpaceTracker.PhysicalMotionEvent var2, float var3, float var4);

      boolean onMove(TouchNavSpaceTracker.PhysicalMotionEvent var1, TouchNavSpaceTracker.PhysicalMotionEvent var2, float var3, float var4);

      boolean onUp(TouchNavSpaceTracker.PhysicalMotionEvent var1, float var2, float var3);
   }
}
