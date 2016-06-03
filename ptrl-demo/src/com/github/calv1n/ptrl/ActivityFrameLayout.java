package com.github.calv1n.ptrl;

import android.content.Context;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by Calvin on 2016/5/26.
 */
public class ActivityFrameLayout extends FrameLayout {
    private int mActivityLeft = -1;
    private int mActivityTop = -1;
    private ImageView mActivityView;
    private ViewDragHelper mDragHelper = ViewDragHelper.create(this, new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mActivityView;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final int leftBound = getPaddingLeft();
            final int rightBound = getWidth() - child.getWidth() - leftBound;

            final int newLeft = Math.min(Math.max(left, leftBound), rightBound);
            return newLeft;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound = getPaddingTop();
            final int bottomBound = getHeight() - child.getHeight() - topBound;

            final int newTop = Math.min(Math.max(topBound, top), bottomBound);
            return newTop;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return getMeasuredHeight() - child.getMeasuredHeight();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return getMeasuredWidth() - child.getMeasuredWidth();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            //松开后根据当前位置确定位置
            int parentWidth = getWidth() / 2;
            Log.e("calvin", "onViewReleased,mActivityLeft->" + mActivityLeft * 2);
            int left;
            if (parentWidth > mActivityLeft) {
                //靠左
                left = getPaddingLeft();
            } else {
                //靠右
                left = getWidth() - getPaddingRight() - releasedChild.getWidth();
            }
            Log.e("calvin", "onViewReleased,left->" + left + "-top-" + mActivityTop);
            //mDragHelper.smoothSlideViewTo(releasedChild, left, mActivityTop);
            mDragHelper.settleCapturedViewAt(left, mActivityTop);
            postInvalidate();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            mActivityLeft = left;
            mActivityTop = top;
        }
    });
    private boolean mIntercept;

    public ActivityFrameLayout(Context context) {
        super(context);
        initView();
    }

    public ActivityFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ActivityFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //将活动移"上层"
        if (null != mActivityView) {
            mActivityView.bringToFront();
        }
    }

    private void initView() {
        mActivityView = new ImageView(getContext());
        LayoutParams params = generateDefaultLayoutParams();
        params.height = LayoutParams.WRAP_CONTENT;
        params.width = LayoutParams.WRAP_CONTENT;

        mActivityView.setImageResource(com.github.calv1n.ptrl.demo.R.drawable.ic_launcher);

        addView(mActivityView, params);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mIntercept = mDragHelper.shouldInterceptTouchEvent(ev);
        return mIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        if (mIntercept) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (null != mActivityView) {
            //默认停放在右侧高度2/3处
            int height = getMeasuredHeight();
            int width = getMeasuredWidth();

            int activityViewWidth = mActivityView.getMeasuredWidth();
            int activityViewHeight = mActivityView.getMeasuredHeight();

            int activityViewTop = height * 2 / 3;
            int activityViewLeft = width - activityViewWidth - getPaddingRight();
            if (mActivityTop != -1) {
                activityViewTop = mActivityTop;
            }
            if (mActivityLeft != -1) {
                activityViewLeft = mActivityLeft;
            }

            int activityViewBottom = Math.min(height, activityViewTop + activityViewHeight);

            mActivityView.layout(activityViewLeft, activityViewTop,
                    activityViewLeft + activityViewWidth, activityViewBottom);
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            postInvalidate();
        }
    }

    public ImageView getActivityView() {
        return this.mActivityView;
    }
}
