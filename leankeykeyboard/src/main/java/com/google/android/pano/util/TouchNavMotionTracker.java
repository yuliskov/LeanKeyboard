package com.google.android.pano.util;

import android.annotation.SuppressLint;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.InputDevice.MotionRange;

public class TouchNavMotionTracker {
    private static final float MAXIMUM_FLING_VELOCITY = 1270.0F;
    private static final float MINIMUM_FLING_VELOCITY = 200.0F;
    private float mCurrX;
    private float mCurrY;
    private MotionEvent mDownEvent;
    private final float mMaxFlingVelocityX;
    private final float mMaxFlingVelocityY;
    private final float mMinFlingVelocityX;
    private final float mMinFlingVelocityY;
    private final float mMinScrollX;
    private final float mMinScrollY;
    private float mPrevX;
    private float mPrevY;
    private final float mResolutionX;
    private final float mResolutionY;
    private float mScrollX;
    private float mScrollY;
    private float mVelX;
    private float mVelY;
    private VelocityTracker mVelocityTracker;

    public TouchNavMotionTracker(float resolutionX, float resolutionY, float minScrollDist) {
        if (resolutionX <= 0.0F) {
            resolutionX = 6.3F;
        }

        this.mResolutionX = resolutionX;
        if (resolutionY <= 0.0F) {
            resolutionY = 6.3F;
        }

        this.mResolutionY = resolutionY;
        this.mMaxFlingVelocityX = this.mResolutionX * 1270.0F;
        this.mMaxFlingVelocityY = this.mResolutionY * 1270.0F;
        this.mMinFlingVelocityX = this.mResolutionX * 200.0F;
        this.mMinFlingVelocityY = this.mResolutionY * 200.0F;
        this.mMinScrollX = this.mResolutionX * minScrollDist;
        this.mMinScrollY = this.mResolutionY * minScrollDist;
    }

    @SuppressLint("NewApi")
    public static TouchNavMotionTracker buildTrackerForDevice(final InputDevice device, final float minScrollDist) {
        MotionRange range = device.getMotionRange(0);
        float resolution;
        if (range == null) {
            resolution = 0.0F;
        } else {
            resolution = range.getResolution();
        }

        float resolutionX = resolution;
        if (resolution <= 0.0F) {
            resolutionX = 6.3F;
        }

        MotionRange range2 = device.getMotionRange(1);
        if (range2 == null) {
            resolution = 0.0F;
        } else {
            resolution = range2.getResolution();
        }

        float resolutionY = resolution;
        if (resolution <= 0.0F) {
            resolutionY = 6.3F;
        }

        return new TouchNavMotionTracker(resolutionX, resolutionY, minScrollDist);
    }

    public void addMovement(MotionEvent var1) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }

        this.mVelocityTracker.addMovement(var1);
    }

    public void clear() {
        if (this.mDownEvent != null) {
            this.mDownEvent.recycle();
            this.mDownEvent = null;
        }

        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }

    }

    public boolean computeVelocity() {
        this.mVelocityTracker.computeCurrentVelocity(1000);
        this.mVelX = Math.min(this.mMaxFlingVelocityX, this.mVelocityTracker.getXVelocity());
        this.mVelY = Math.min(this.mMaxFlingVelocityY, this.mVelocityTracker.getYVelocity());
        return Math.abs(this.mVelX) > this.mMinFlingVelocityX || Math.abs(this.mVelY) > this.mMinFlingVelocityY;
    }

    public MotionEvent getDownEvent() {
        return this.mDownEvent;
    }

    public float getPhysicalX(float var1) {
        return var1 / this.mResolutionX;
    }

    public float getPhysicalY(float var1) {
        return var1 / this.mResolutionY;
    }

    public float getScrollX() {
        return this.mScrollX;
    }

    public float getScrollY() {
        return this.mScrollY;
    }

    public float getXResolution() {
        return this.mResolutionX;
    }

    public float getXVel() {
        return this.mVelX;
    }

    public float getYResolution() {
        return this.mResolutionY;
    }

    public float getYVel() {
        return this.mVelY;
    }

    public void setDownEvent(MotionEvent var1) {
        if (this.mDownEvent != null && var1 != this.mDownEvent) {
            this.mDownEvent.recycle();
        }

        this.mDownEvent = var1;
    }

    public boolean setNewValues(float var1, float var2) {
        this.mCurrX = var1;
        this.mCurrY = var2;
        this.mScrollX = this.mCurrX - this.mPrevX;
        this.mScrollY = this.mCurrY - this.mPrevY;
        return Math.abs(this.mScrollX) > this.mMinScrollX || Math.abs(this.mScrollY) > this.mMinScrollY;
    }

    public void updatePrevValues() {
        this.mPrevX = this.mCurrX;
        this.mPrevY = this.mCurrY;
    }
}
