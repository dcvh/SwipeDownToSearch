package com.example.cpu10661.pulldownsearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ImageView;

/**
 * Created by cpu10661 on 12/28/17.
 *
 * Prerequisites:
 * 1. This layout must be used as a separated layout taking up the whole screen,
 *    in other words both height and width should be assigned match_parent value
 * 2. The number of child views must be 3, otherwise it will throw an exception
 * 3. The order of child views must be as follows: toolbar, rear view, then front view.
 * 4. All constraint attributes will be initialized in code so it does not need to be defined in
 *    xml layout. Additionally, width/height attributes (except for toolbar height) must be 0dp,
 *    otherwise the layout might not render properly.
 *
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
    private boolean mIsConstraintsInitialized = false;

    private int mActivePointerId;
    private float mInitialMotionY;

    public SwipeSearchLayout(Context context) {
        super(context);
    }

    public SwipeSearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SwipeSearchLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        // initialize variables
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mInputMethodManager = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        // force the keyboard to overlay the behind view
        ((Activity)getContext()).getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        initializeLayout();

        int toolbarHeight = mToolbar.getHeight();
        mContentViewTop = toolbarHeight;
        mMaxDragDistance = toolbarHeight;
        mDistanceDiff = toolbarHeight;
        mRevealThreshold = mMaxDragDistance * 1.6f;     // obtain through trial and error
    }

    private void initializeLayout() throws IllegalArgumentException {
        // check number of child views
        if (getChildCount() != 3) {
            throw new IllegalArgumentException("This view must have 3 child views, currently having "
                    + getChildCount() + " view(s)");
        }

        mToolbar = getChildAt(0);
        mRearView = getChildAt(1);
        mFrontView = getChildAt(2);

        // initialize constraints
        if (!mIsConstraintsInitialized) {
//            resetWidthHeight();
            initializeConstraints();
            mIsConstraintsInitialized = true;
        }

        // to detect swipe up gesture (to return to front view)
        mFrontView.setClickable(true);
        mRearView.setClickable(true);

        // TODO: 12/29/17 come up with a more intuitive solution
        ImageView upButton = findViewById(R.id.btn_back);
        if (upButton != null) {
            upButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFrontView();
                }
            });
        }
    }

    private void resetWidthHeight() {
        // toolbar
        LayoutParams toolbarParams = (LayoutParams) mToolbar.getLayoutParams();
        toolbarParams.width = 0;
        mToolbar.setLayoutParams(toolbarParams);

        // front and rear view
        LayoutParams layoutParams = new LayoutParams(0, 0);
        mFrontView.setLayoutParams(layoutParams);
        mRearView.setLayoutParams(layoutParams);
    }

    private void initializeConstraints() {
        // get view IDs
        int toolbarId = mToolbar.getId();
        int frontViewId = mFrontView.getId();
        int rearViewId = mRearView.getId();
        int parentId = ConstraintSet.PARENT_ID;

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);

        // toolbar constraint
        constraintSet.connect(toolbarId, ConstraintSet.TOP, parentId, ConstraintSet.TOP,0);
        constraintSet.connect(toolbarId, ConstraintSet.LEFT, parentId, ConstraintSet.LEFT,0);
        constraintSet.connect(toolbarId, ConstraintSet.RIGHT, parentId, ConstraintSet.RIGHT,0);

        // front view constraint
        constraintSet.connect(frontViewId, ConstraintSet.TOP, toolbarId, ConstraintSet.BOTTOM,0);
        constraintSet.connect(frontViewId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM,0);
        constraintSet.connect(frontViewId, ConstraintSet.LEFT, parentId, ConstraintSet.LEFT,0);
        constraintSet.connect(frontViewId, ConstraintSet.RIGHT, parentId, ConstraintSet.RIGHT,0);

        // rear view constraint
        constraintSet.connect(rearViewId, ConstraintSet.TOP, toolbarId, ConstraintSet.BOTTOM,0);
        constraintSet.connect(rearViewId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM,0);
        constraintSet.connect(rearViewId, ConstraintSet.LEFT, parentId, ConstraintSet.LEFT,0);
        constraintSet.connect(rearViewId, ConstraintSet.RIGHT, parentId, ConstraintSet.RIGHT,0);

        // apply constraints
        constraintSet.applyTo(this);
    }

    public void setButtonAsUpEnabled(int upButtonResId) {
        findViewById(upButtonResId).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFrontView();
            }
        });
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
                    // animate those views
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
                    // return to front view
                    if (overScrollTop < 0 &&
                            -overScrollTop > mMaxDragDistance / 2 /* trial and error */) {
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

        // slide up toolbar and rear view
        mToolbar.animate().setDuration(SLIDE_DURATION_MILLIS).y(-mMaxDragDistance);
        mRearView.animate().setDuration(SLIDE_DURATION_MILLIS).y(0);

        // slide down the front view
//        mFrontView.animate().setDuration(SLIDE_DURATION_MILLIS).y(getHeight());
        float segment = (getHeight() - mFrontView.getY()) / SLIDE_DURATION_MILLIS;
        float curY = mFrontView.getY();
        Handler handler = new Handler();
        for (int i = 0; curY < getHeight(); i++) {
            curY += segment;
            final float nextY = curY;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFrontView.setY(nextY);
                }
            }, i);
        }

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