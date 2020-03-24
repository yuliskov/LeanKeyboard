package com.liskovsoft.leankeyboard.ime.pano.util;

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
    private static final float[] DIRECTION_BOUNDARIES = new float[]{-2.7488935F, -1.9634954F, -1.1780972F, -0.3926991F, 0.3926991F, 1.1780972F,
            1.9634954F, 2.7488935F};
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
        this(null, null);
    }

    public TouchNavSpaceTracker(TouchNavSpaceTracker.KeyEventListener keyListener, TouchNavSpaceTracker.TouchEventListener pixelSpaceListener) {
        mPrevPhysPosition = new PointF(Float.MIN_VALUE, Float.MIN_VALUE);
        mPhysicalPosition = new PointF(Float.MIN_VALUE, Float.MIN_VALUE);
        mWasBlocked = false;
        mSensitivityInterpolator = new AccelerateInterpolator();
        mDampingDuration = DEFAULT_DAMPING_DURATION_MS;
        mDampedSensitivity = DEFAULT_DAMPED_SENSITIVITY;
        mSensitivity = DEFAULT_SENSITIVITY;
        mUnscaledFlickMinDistance = DEFAULT_MIN_FLICK_DISTANCE_MM;
        mUnscaledFlickMaxDistance = DEFAULT_MAX_FLICK_DISTANCE_MM;
        mFlickMinDistance = mSensitivity * DEFAULT_MIN_FLICK_DISTANCE_MM;
        mFlickMaxDistance = mSensitivity * DEFAULT_MAX_FLICK_DISTANCE_MM;
        mFlickMinSquared = mFlickMinDistance * mFlickMinDistance;
        mFlickMaxSquared = mFlickMaxDistance * mFlickMaxDistance;
        mFlickMaxDuration = DEFAULT_MAX_FLICK_DURATION_MS;
        mLPFEnabled = false;
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        if (TouchNavSpaceTracker.this.mKeyEventListener != null) {
                            TouchNavSpaceTracker.this.mKeyEventListener.onKeyLongPress(msg.arg1, (KeyEvent) msg.obj);
                            return;
                        }
                    default:
                }
            }
        };
        mKeyEventListener = keyListener;
        mPixelListener = pixelSpaceListener;
        mTouchParams = new SparseArray<>(1);
        mPhysicalWidth = DEFAULT_HORIZONTAL_SIZE_MM;
        mPhysicalHeight = DEFAULT_VERTICAL_SIZE_MM;
        mPixelWidth = 0.0F;
        mPixelHeight = 0.0F;
        mPixelsPerMm = 0.0F;
    }

    private float calculateSensitivity(MotionEvent var1, MotionEvent var2) {
        long var4 = var1.getEventTime() - var2.getEventTime();
        float var3;
        if (var1.getEventTime() < this.mMovementBlockTime) {
            var3 = 0.0F;
            this.mWasBlocked = true;
        } else if ((float) var4 < this.mDampingDuration) {
            var3 = this.mSensitivityInterpolator.getInterpolation((float) var4 / this.mDampingDuration);
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

    private void checkForLongClick(int var1, KeyEvent event) {
        if (var1 == 23) {
            Message msg = this.mHandler.obtainMessage(0);
            msg.arg1 = var1;
            msg.obj = event;
            if (!this.mHandler.hasMessages(0)) {
                this.mHandler.sendMessageDelayed(msg, (long) ViewConfiguration.getLongPressTimeout());
                return;
            }
        }

    }

    private void clampPosition() {
        if (mPhysicalPosition.x < 0.0F) {
            setPhysicalPosition(0.0F, mPhysicalPosition.y);
        } else if (mPhysicalPosition.x > mPhysicalWidth) {
            setPhysicalPosition(mPhysicalWidth, mPhysicalPosition.y);
        }

        if (mPhysicalPosition.y < 0.0F) {
            setPhysicalPosition(mPhysicalPosition.x, 0.0F);
        } else if (mPhysicalPosition.y > mPhysicalHeight) {
            setPhysicalPosition(mPhysicalPosition.x, mPhysicalHeight);
        }
    }

    private int getDpadDirection(final float dx, final float dy) {
        final float polar = (float) Math.atan2((double) (-dy), (double) dx);

        int idx;
        for (idx = 0; idx < DIRECTION_BOUNDARIES.length && polar >= DIRECTION_BOUNDARIES[idx]; ++idx) {
            ;
        }

        return DIRECTIONS[idx];
    }

    private float getPhysicalX(float x) {
        return this.mPixelWidth <= 0.0F ? 0.0F : this.mPhysicalWidth * x / this.mPixelWidth;
    }

    private float getPhysicalY(float y) {
        return this.mPixelHeight <= 0.0F ? 0.0F : this.mPhysicalHeight * y / this.mPixelHeight;
    }

    private float getPixelX(float x) {
        return this.mPixelWidth * x / this.mPhysicalWidth;
    }

    private float getPixelY(float y) {
        return this.mPixelHeight * y / this.mPhysicalHeight;
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

    private TouchNavMotionTracker getTrackerForDevice(InputDevice device) {
        TouchNavMotionTracker var3 = (TouchNavMotionTracker) this.mTouchParams.get(device.getId());
        TouchNavMotionTracker var2 = var3;
        if (var3 == null) {
            var2 = TouchNavMotionTracker.buildTrackerForDevice(device, 0.1F);
            this.mTouchParams.put(device.getId(), var2);
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
        this.mDampingDuration = (float) var2;
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

    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event != null && (event.getSource() & InputDevice.SOURCE_TOUCH_NAVIGATION) == InputDevice.SOURCE_TOUCH_NAVIGATION) {
            InputDevice device = event.getDevice();
            if (device == null) {
                return false;
            }

            TouchNavMotionTracker tracker = this.getTrackerForDevice(device);
            int action = event.getActionMasked();
            tracker.addMovement(event);
            boolean pointerUp;
            if ((action & 255) == MotionEvent.ACTION_POINTER_UP) {
                pointerUp = true;
            } else {
                pointerUp = false;
            }

            int skipIndex;
            if (pointerUp) {
                skipIndex = event.getActionIndex();
            } else {
                skipIndex = -1;
            }

            float sumX = 0.0F;
            float sumY = 0.0F;
            int count = event.getPointerCount();

            for (int i = 0; i < count; ++i) {
                if (skipIndex != i) {
                    sumX += event.getX(i);
                    sumY += event.getY(i);
                }
            }

            int div;
            if (pointerUp) {
                div = count - 1;
            } else {
                div = count;
            }

            float currX = sumX / (float) div;
            float currY = sumY / (float) div;
            TouchNavSpaceTracker.PhysicalMotionEvent pe = new TouchNavSpaceTracker.PhysicalMotionEvent(event.getDeviceId(), tracker.getPhysicalX
                    (currX), tracker.getPhysicalX(currY), event.getEventTime());
            boolean var18 = false;
            boolean var12 = false;
            boolean var11;
            MotionEvent var15;
            TouchNavSpaceTracker.PhysicalMotionEvent var16;
            switch (action & 255) {
                case MotionEvent.ACTION_DOWN:
                    if (mLPFEnabled) {
                        mLPFCurrX = currX;
                        mLPFCurrY = currY;
                    }

                    tracker.setNewValues(currX, currY);
                    tracker.updatePrevValues();
                    tracker.setDownEvent(MotionEvent.obtain(event));
                    if (mPixelListener != null) {
                        return mPixelListener.onDown(pe);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    var15 = tracker.getDownEvent();
                    if (var15 == null) {
                        Log.w("TouchNavSpaceTracker", "Up event without down event");
                        return false | this.mPixelListener.onUp(pe, this.getPixelX(this.mPhysicalPosition.x),
                                this.getPixelY(this.mPhysicalPosition.y));
                    }

                    var16 = new TouchNavSpaceTracker.PhysicalMotionEvent(event.getDeviceId(), tracker.getPhysicalX(var15.getX()), tracker.getPhysicalY
                            (var15.getY()), var15.getEventTime());
                    pointerUp = var18;
                    if (tracker.computeVelocity()) {
                        pointerUp = var18;
                        if (this.mPixelListener != null) {
                            sumY = this.getPixelX(tracker.getPhysicalX(tracker.getXVel()));
                            sumX = this.getPixelY(tracker.getPhysicalY(tracker.getYVel()));
                            var18 = false | this.mPixelListener.onFling(var16, pe, sumY, sumX);
                            pointerUp = var18;
                            if (pe.getTime() - var16.getTime() < this.mFlickMaxDuration) {
                                sumY = pe.getX() - var16.getX();
                                sumX = pe.getY() - var16.getY();
                                currX = sumY * sumY + sumX * sumX;
                                pointerUp = var18;
                                if (currX > this.mFlickMinSquared) {
                                    pointerUp = var18;
                                    if (currX < this.mFlickMaxSquared) {
                                        this.mPixelListener.onFlick(var16, pe, this.getDpadDirection(sumY, sumX), this.getPrimaryDpadDirection
                                                (sumY, sumX));
                                        pointerUp = var18;
                                    }
                                }
                            }
                        }
                    }

                    sumY = this.getPixelX(this.mPhysicalPosition.x);
                    sumX = this.getPixelY(this.mPhysicalPosition.y);
                    var11 = this.mPixelListener.onUp(pe, sumY, sumX);
                    tracker.clear();
                    return pointerUp | var11;
                case MotionEvent.ACTION_MOVE:
                    if (tracker.getDownEvent() == null) {
                        tracker.setDownEvent(MotionEvent.obtain(event));
                        if (this.mLPFEnabled) {
                            this.mLPFCurrX = currX;
                            this.mLPFCurrY = currY;
                        }
                    }

                    sumX = currX;
                    sumY = currY;
                    if (this.mLPFEnabled) {
                        this.mLPFCurrX = this.mLPFCurrX * 0.75F + DEFAULT_LPF_COEFF * currX;
                        this.mLPFCurrY = this.mLPFCurrY * 0.75F + DEFAULT_LPF_COEFF * currY;
                        sumX = this.mLPFCurrX;
                        sumY = this.mLPFCurrY;
                    }

                    if (tracker.setNewValues(sumX, sumY)) {
                        sumY = tracker.getPhysicalX(tracker.getScrollX());
                        sumX = tracker.getPhysicalY(tracker.getScrollY());
                        currX = this.calculateSensitivity(event, tracker.getDownEvent());
                        this.mPhysicalPosition.x = this.mPrevPhysPosition.x + this.getScaledValue(sumY, currX);
                        this.mPhysicalPosition.y = this.mPrevPhysPosition.y + this.getScaledValue(sumX, currX);
                        this.clampPosition();
                        if (!this.mPhysicalPosition.equals(this.mPrevPhysPosition)) {
                            var11 = var12;
                            if (this.mPixelListener != null) {
                                var11 = var12;
                                if (this.mPixelHeight > 0.0F) {
                                    var11 = var12;
                                    if (this.mPixelWidth > 0.0F) {
                                        var15 = tracker.getDownEvent();
                                        var16 = new TouchNavSpaceTracker.PhysicalMotionEvent(event.getDeviceId(), tracker.getPhysicalX(var15.getX()),
                                                tracker.getPhysicalY(var15.getY()), var15.getEventTime());
                                        sumY = this.getPixelX(this.mPhysicalPosition.x);
                                        sumX = this.getPixelY(this.mPhysicalPosition.y);
                                        var11 = false | this.mPixelListener.onMove(var16, pe, sumY, sumX);
                                    }
                                }
                            }

                            this.mPrevPhysPosition.set(this.mPhysicalPosition);
                        } else {
                            var11 = false | true;
                        }

                        tracker.updatePrevValues();
                        return var11;
                    }

                    return false | true;
                case MotionEvent.ACTION_CANCEL:
                    tracker.clear();
                    return false;
                default:
                    return false;
            }
        }

        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event != null && event.getDevice() != null && (event.getDevice().getSources() & InputDevice.SOURCE_TOUCH_NAVIGATION) == InputDevice
                .SOURCE_TOUCH_NAVIGATION) {
            if (event.getRepeatCount() == 0) {
                checkForLongClick(keyCode, event);
            }

            if (mKeyEventListener != null) {
                return mKeyEventListener.onKeyDown(keyCode, event);
            }
        }

        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event != null && event.getDevice() != null && (event.getDevice().getSources() & InputDevice.SOURCE_TOUCH_NAVIGATION) == InputDevice
                .SOURCE_TOUCH_NAVIGATION) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                mHandler.removeMessages(0);
            }

            if (mKeyEventListener != null) {
                return mKeyEventListener.onKeyUp(keyCode, event);
            }
        }

        return false;
    }

    public void onPause() {
        mHandler.removeMessages(0);
    }

    public void setKeyEventListener(TouchNavSpaceTracker.KeyEventListener listener) {
        mKeyEventListener = listener;
    }

    public void setLPFEnabled(boolean enabled) {
        mLPFEnabled = enabled;
    }

    public void setPhysicalDensity(float density) {
        mPixelsPerMm = density;
        if (density > 0.0F) {
            updatePhysicalSize();
        }

    }

    public void setPhysicalPosition(float x, float y) {
        mPhysicalPosition.x = x;
        mPhysicalPosition.y = y;
        mPrevPhysPosition.x = x;
        mPrevPhysPosition.y = y;
        clampPosition();
    }

    public void setPhysicalSize(float widthMm, float heightMm) {
        if (mPixelsPerMm <= 0.0F) {
            setPhysicalSizeInternal(widthMm, heightMm);
        }
    }

    public void setPixelPosition(float x, float y) {
        setPhysicalPosition(getPhysicalX(x), getPhysicalY(y));
    }

    public void setPixelSize(float width, float height) {
        mPixelHeight = height;
        mPixelWidth = width;
        updatePhysicalSize();
    }

    public void setSensitivity(float sensitivity) {
        mSensitivity = sensitivity;
        configureFlicks(mUnscaledFlickMinDistance, mUnscaledFlickMaxDistance, mFlickMaxDuration);
    }

    public void setTouchEventListener(TouchNavSpaceTracker.TouchEventListener listener) {
        mPixelListener = listener;
    }

    public void unblockMovement() {
        this.mMovementBlockTime = 0L;
    }

    public interface KeyEventListener {
        boolean onKeyDown(int keyCode, KeyEvent event);

        boolean onKeyLongPress(int keyCode, KeyEvent event);

        boolean onKeyUp(int keyCode, KeyEvent event);
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

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return false;
        }

        public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            return false;
        }

        public boolean onKeyUp(int keyCode, KeyEvent event) {
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
