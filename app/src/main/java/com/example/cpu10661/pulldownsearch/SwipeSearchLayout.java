package com.example.cpu10661.pulldownsearch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;

/**
 * Created by cpu10661 on 12/28/17.
 *
 * This layout must be used as a separated layout taking up the whole screen,
 * in other words both height and width should be assigned match_parent value
 */

public class SwipeSearchLayout extends ConstraintLayout {

    private static final String TAG = SwipeSearchLayout.class.getSimpleName();

    private static final float DRAG_RATE = 1f;
    private static final int INVALID_POINTER = -1;
    private static final int SLIDE_DURATION_MILLIS = 150;

    private int mTouchSlop;
    private boolean mIsBeingDragged = false;
    private boolean mIsRearViewShown = false;

    private View mToolbar, mFrontView, mRearView;
    private int mContentViewTop;
    private float mMaxDragDistance, mDistanceDiff;
    private float mRevealThreshold;

    private int mActivePointerId;
    private float mInitialMotionY;

    public SwipeSearchLayout(Context context) {
        super(context);
        init(context);
    }

    public SwipeSearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipeSearchLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) throws IllegalArgumentException {

        // inflate layout
        LayoutInflater.from(context).inflate(R.layout.swipe_search_layout, this);
        if (getChildCount() != 3) {
            throw new IllegalArgumentException("This view must have 3 child views");
        }
        mToolbar = findViewById(R.id.toolbar);
        mFrontView = findViewById(R.id.front_view);
        mRearView = findViewById(R.id.rear_view);
        findViewById(R.id.btn_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                returnBackButtonFunction();
            }
        });

        // TODO: 12/28/17 come up with a more accurate method to obtain toolbar height
        // instead of relying on the assumption that toolbar uses the ?attr/actionBarSize value
        TypedArray ta = getContext().obtainStyledAttributes(new int[] {R.attr.actionBarSize});
        int toolbarHeight = ta.getDimensionPixelSize(0, -1);
        ta.recycle();

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mContentViewTop = toolbarHeight;
        mMaxDragDistance = toolbarHeight;
        mDistanceDiff = toolbarHeight;
        mRevealThreshold = mMaxDragDistance * 1.6f;     // obtain through trial and error
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (!isEnabled() || canChildScrollUp()) {
            return false;
        }

        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                final float initialMotionY = getMotionEventY(ev, mActivePointerId);
                if (initialMotionY == -1) {
                    return false;
                }
                mInitialMotionY = initialMotionY;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialMotionY;
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        return mIsBeingDragged;
    }

    @SuppressLint("ObsoleteSdkInt")
    private boolean canChildScrollUp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mFrontView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mFrontView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return !mIsRearViewShown && mFrontView.getScrollY() > 0;
            }
        } else {
            return !mIsRearViewShown && mFrontView.canScrollVertically(-1);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {

        if (!mIsBeingDragged) {
            return super.onTouchEvent(ev);
        }

        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                // calculate moving distance
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                final float yDiff = y - mInitialMotionY;
                final float overScrollTop = yDiff * DRAG_RATE;

                if (!mIsRearViewShown) {
                    // scrolling views
                    float toolbarSlideY = getValueInRange(0, mMaxDragDistance,
                            overScrollTop - mDistanceDiff);
                    mToolbar.setY(-toolbarSlideY);
                    mToolbar.setAlpha(1 - toolbarSlideY / mMaxDragDistance);
                    mRearView.setY(mContentViewTop - toolbarSlideY);
                    mFrontView.setY(mContentViewTop + getValueInRange(0,
                            mMaxDragDistance + mDistanceDiff, overScrollTop));
                } else {
                    if (overScrollTop < 0 && Math.abs(overScrollTop) > mMaxDragDistance) {
                        returnBackButtonFunction();
                    }
                }

                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = ev.getActionIndex();
                mActivePointerId = ev.getPointerId(index);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                // calculate moving distance
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float y = ev.getY(pointerIndex);
                final float overScrollTop = (y - mInitialMotionY) * DRAG_RATE;

                if (overScrollTop > mRevealThreshold) {
                    // review search screen
                    mToolbar.animate().setDuration(SLIDE_DURATION_MILLIS).y(-mMaxDragDistance);
                    mFrontView.animate().setDuration(SLIDE_DURATION_MILLIS).y(getHeight());
                    mRearView.animate().setDuration(SLIDE_DURATION_MILLIS).y(0);

                    // temporarily override back button function
                    setFocusableInTouchMode(true);
                    requestFocus();
                    mIsRearViewShown = true;
                } else {
                    animateViewsBackToOriginalPosition();
                }
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    private float getValueInRange(float min, float max, float value) {
        float lowerBound = Math.max(min, value);
        return Math.min(lowerBound, max);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = ev.findPointerIndex(activePointerId);
        if (index < 0) {
            return -1;
        }
        return ev.getY(index);
    }

    private void animateViewsBackToOriginalPosition() {
        mToolbar.animate().setDuration(SLIDE_DURATION_MILLIS).y(0);
        mToolbar.animate().setDuration(SLIDE_DURATION_MILLIS).alpha(1);

        mFrontView.animate().setDuration(SLIDE_DURATION_MILLIS).y(mMaxDragDistance);

        mRearView.animate().setDuration(SLIDE_DURATION_MILLIS).y(mMaxDragDistance);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // override back button function
        if (keyCode == KeyEvent.KEYCODE_BACK && mIsRearViewShown) {
            returnBackButtonFunction();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void returnBackButtonFunction() {
        animateViewsBackToOriginalPosition();
        mIsRearViewShown = false;

        setFocusableInTouchMode(false);
        clearFocus();
    }
}
