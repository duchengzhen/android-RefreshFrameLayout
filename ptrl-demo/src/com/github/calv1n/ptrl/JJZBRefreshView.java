package com.github.calv1n.ptrl;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.calv1n.LogUtil;

/**
 * 刷新和加载更多视图
 * Created by Calvin on 2016/4/1.
 */
public class JJZBRefreshView extends FrameLayout implements IRefreshHandler {

    private TextView tvText;
    private ImageView ivIcon;
    private boolean isRefresh = true;
    private JJZBRefreshDrawable mJjzbRefreshDrawable;

    public JJZBRefreshView(Context context, boolean isRefresh) {
        super(context);
        this.isRefresh = isRefresh;
        initView();
    }

    public JJZBRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        View.inflate(getContext(), com.github.calv1n.ptrl.demo.R.layout.layout_refresh, this);
        tvText = (TextView) findViewById(com.github.calv1n.ptrl.demo.R.id.tvText);
        ivIcon = (ImageView) findViewById(com.github.calv1n.ptrl.demo.R.id.ivIcon);
        mJjzbRefreshDrawable = new JJZBRefreshDrawable(ivIcon);
    }

    @Override
    public void onReset(IRefreshLayout frame) {
        LogUtil.i("onUIReset");
        pullToRelease();
    }

    private void pullToRelease() {
        if (isRefresh) {
            tvText.setText("下拉刷新");
            ivIcon.setImageResource(com.github.calv1n.ptrl.demo.R.drawable.ic_pull_to_release);
        } else {
            tvText.setText("上拉加载");
            ivIcon.setImageResource(com.github.calv1n.ptrl.demo.R.drawable.ic_release_to_refresh);
        }
    }

    @Override
    public void onRefreshPrepare(IRefreshLayout frame) {
        LogUtil.i("onUIRefreshPrepare");
        pullToRelease();
        //releaseToRefresh();
        //onUIReset(frame);
    }

    private void releaseToRefresh() {
        if (isRefresh) {
            tvText.setText("松开刷新");
            ivIcon.setImageResource(com.github.calv1n.ptrl.demo.R.drawable.ic_release_to_refresh);
        } else {
            tvText.setText("松开加载");
            ivIcon.setImageResource(com.github.calv1n.ptrl.demo.R.drawable.ic_pull_to_release);
        }
    }

    @Override
    public void onRefreshBegin(IRefreshLayout frame) {
        LogUtil.i("onUIRefreshBegin");
        if (isRefresh) {
            tvText.setText("正在刷新");
            ivIcon.setImageDrawable(mJjzbRefreshDrawable);
        } else {
            tvText.setText("正在加载");
            ivIcon.setImageDrawable(mJjzbRefreshDrawable);
        }
        //开始动画
        startAnimation();
    }

    private void startAnimation() {
        Drawable drawable = ivIcon.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
    }

    private void stopAnimation() {
        Drawable drawable = ivIcon.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).stop();
        }
    }

    @Override
    public void onRefreshComplete(IRefreshLayout frame) {
        LogUtil.i("onUIRefreshComplete");
        stopAnimation();
        if (isRefresh) {
            tvText.setText("刷新完成");
            ivIcon.setImageResource(com.github.calv1n.ptrl.demo.R.drawable.ic_refreshing_src);
        } else {
            tvText.setText("加载完成");
            ivIcon.setImageResource(com.github.calv1n.ptrl.demo.R.drawable.ic_refreshing_src);
        }
    }

    @Override
    public void onPositionChange(IRefreshLayout frame, boolean isUnderTouch, byte status,
            ViewScrollHelper viewScrollHelper) {

        final int currentPos = viewScrollHelper.getCurrentPosY();
        final int lastPos = viewScrollHelper.getLastPosY();

        int mOffsetToRefreshAbs = 0;
        switch (frame.getRefreshingDirection()) {
            case IRefreshLayout.DIRECTION_DOWN:
                mOffsetToRefreshAbs = Math.abs(frame.getOffsetToRefresh());
                break;
            case IRefreshLayout.DIRECTION_UP:
                mOffsetToRefreshAbs = Math.abs(frame.getOffsetToLoadMore());
                break;
        }

        //LogUtil.i("onUIPositionChange,currentPos=%s", currentPos);
        if (Math.abs(currentPos) < mOffsetToRefreshAbs
                && Math.abs(lastPos) >= mOffsetToRefreshAbs) {
            if (isUnderTouch && status == IRefreshLayout.STATUS_PREPARE) {
                pullToRelease();
            }
        } else if (Math.abs(currentPos) > mOffsetToRefreshAbs
                && Math.abs(lastPos) <= mOffsetToRefreshAbs) {
            if (isUnderTouch && status == IRefreshLayout.STATUS_PREPARE) {
                releaseToRefresh();
            }
        }
    }

    public void setIsRefresh(boolean isRefresh) {
        this.isRefresh = isRefresh;
    }
}
