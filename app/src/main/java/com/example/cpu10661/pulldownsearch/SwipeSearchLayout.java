package com.example.cpu10661.pulldownsearch;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;

/**
 * Created by cpu10661 on 12/28/17.
 *
 * This layout must be used as a separated layout taking up the whole screen,
 * in other words both height and width should be assigned match_parent value
 */

public class SwipeSearchLayout extends ConstraintLayout {

    private static final String TAG = SwipeSearchLayout.class.getSimpleName();

    private static final float DRAG_RATE = 0.3f;
    private static final int INVALID_POINTER = -1;
    private static final int SLIDE_DURATION_MILLIS = 150;

    private int mTouchSlop;
    private boolean mIsBeingDragged = false;
    private boolean mIsShowingFront = true;

    private View mToolbar, mFrontView, mRearView;
    private int mContentViewTop;
    private float mMaxDragDistance, mDistanceDiff;
    private float mRevealThreshold;
    private InputMethodManager mInputMethodManager;

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
        mRearView.setClickable(true);
        findViewById(R.id.btn_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFrontView();
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

        mInputMethodManager = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        ((Activity)getContext()).getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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
                boolean isSwipeGestureValid = (mIsShowingFront ? yDiff : -yDiff) > mTouchSlop;
                if (isSwipeGestureValid && !mIsBeingDragged) {
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
                return mIsShowingFront && mFrontView.getScrollY() > 0;
            }
        } else {
            return mIsShowingFront && mFrontView.canScrollVertically(-1);
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

                if (mIsShowingFront) {
                    // scrolling views
                    float toolbarSlideY = getValueInRange(0, mMaxDragDistance,
                            overScrollTop - mDistanceDiff);
                    mToolbar.setY(-toolbarSlideY);
                    mToolbar.setAlpha(1 - toolbarSlideY / mMaxDragDistance);
                    mRearView.setY(mContentViewTop - toolbarSlideY);
                    mFrontView.setY(mContentViewTop + getValueInRange(0,
                            mMaxDragDistance + mDistanceDiff, overScrollTop));
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

                if (mIsShowingFront) {
                    if (overScrollTop > mRevealThreshold) {
                        showRearView();
                    } else {
                        animateViewsBackToOriginalPosition();
                    }
                } else {
                    if (overScrollTop < 0 && -overScrollTop > mMaxDragDistance / 2) {
                        showFrontView();
                    }
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

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEventPreIme: ");
        // override back button function
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && !mIsShowingFront) {
            showFrontView();
            return true;
        }
        return super.dispatchKeyEventPreIme(event);
    }

    private void showRearView() {
        if (!mIsShowingFront) {
            return;
        }

        // animate views
        mToolbar.animate().setDuration(SLIDE_DURATION_MILLIS).y(-mMaxDragDistance);
        mFrontView.animate().setDuration(SLIDE_DURATION_MILLIS).y(getHeight());
        mRearView.animate().setDuration(SLIDE_DURATION_MILLIS).y(0);

        overrideBackButtonFunction(true);

        // automatically show the keyboard
        mRearView.requestFocus();
        mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);

        mIsShowingFront = false;
    }

    private void showFrontView() {
        if (mIsShowingFront) {
            return;
        }

        overrideBackButtonFunction(false);
        animateViewsBackToOriginalPosition();

        // hide the keyboard
        mInputMethodManager.hideSoftInputFromWindow(mRearView.getWindowToken(), 0);
        mFrontView.requestFocus();

        mIsShowingFront = true;
    }

    private void overrideBackButtonFunction(boolean override) {
        if (override) {
            setFocusableInTouchMode(true);
            requestFocus();
        } else {
            setFocusableInTouchMode(false);
            clearFocus();
        }
    }

    private void animateViewsBackToOriginalPosition() {
        mToolbar.animate().setDuration(SLIDE_DURATION_MILLIS).y(0);
        mToolbar.animate().setDuration(SLIDE_DURATION_MILLIS).alpha(1);
        mFrontView.animate().setDuration(SLIDE_DURATION_MILLIS).y(mMaxDragDistance);
        mRearView.animate().setDuration(SLIDE_DURATION_MILLIS).y(mMaxDragDistance);
    }
}
