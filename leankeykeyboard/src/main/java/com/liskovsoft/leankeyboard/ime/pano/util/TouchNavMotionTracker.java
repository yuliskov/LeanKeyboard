package com.liskovsoft.leankeyboard.ime.pano.util;

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

        mResolutionX = resolutionX;
        if (resolutionY <= 0.0F) {
            resolutionY = 6.3F;
        }

        mResolutionY = resolutionY;
        mMaxFlingVelocityX = mResolutionX * MAXIMUM_FLING_VELOCITY;
        mMaxFlingVelocityY = mResolutionY * MAXIMUM_FLING_VELOCITY;
        mMinFlingVelocityX = mResolutionX * MINIMUM_FLING_VELOCITY;
        mMinFlingVelocityY = mResolutionY * MINIMUM_FLING_VELOCITY;
        mMinScrollX = mResolutionX * minScrollDist;
        mMinScrollY = mResolutionY * minScrollDist;
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

    public void addMovement(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(event);
    }

    public void clear() {
        if (mDownEvent != null) {
            mDownEvent.recycle();
            mDownEvent = null;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }

    }

    public boolean computeVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        mVelX = Math.min(mMaxFlingVelocityX, mVelocityTracker.getXVelocity());
        mVelY = Math.min(mMaxFlingVelocityY, mVelocityTracker.getYVelocity());
        return Math.abs(mVelX) > mMinFlingVelocityX || Math.abs(mVelY) > mMinFlingVelocityY;
    }

    public MotionEvent getDownEvent() {
        return mDownEvent;
    }

    public float getPhysicalX(float x) {
        return x / mResolutionX;
    }

    public float getPhysicalY(float y) {
        return y / mResolutionY;
    }

    public float getScrollX() {
        return mScrollX;
    }

    public float getScrollY() {
        return mScrollY;
    }

    public float getXResolution() {
        return mResolutionX;
    }

    public float getXVel() {
        return mVelX;
    }

    public float getYResolution() {
        return mResolutionY;
    }

    public float getYVel() {
        return mVelY;
    }

    public void setDownEvent(MotionEvent event) {
        if (mDownEvent != null && event != mDownEvent) {
            mDownEvent.recycle();
        }

        mDownEvent = event;
    }

    public boolean setNewValues(float currX, float currY) {
        mCurrX = currX;
        mCurrY = currY;
        mScrollX = mCurrX - mPrevX;
        mScrollY = mCurrY - mPrevY;
        return Math.abs(mScrollX) > mMinScrollX || Math.abs(mScrollY) > mMinScrollY;
    }

    public void updatePrevValues() {
        mPrevX = mCurrX;
        mPrevY = mCurrY;
    }
}
