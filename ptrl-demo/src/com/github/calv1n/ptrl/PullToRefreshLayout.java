package com.github.calv1n.ptrl;

import android.content.Context;
import android.util.AttributeSet;

/**
 * 下拉刷新&上拉加载控件
 * Created by Calvin on 2016/6/1.
 */
public class PullToRefreshLayout extends RefreshFrameLayout {

    //超时时间
    public static final int DELAY_TIME = 20 * 1000;
    private JJZBRefreshView mHeaderView;
    private JJZBRefreshView mFooterView;
    private OnRefreshListener mRefreshListener;
    private OnLoadListener mLoadListener;

    private Runnable timeoutChecker = new Runnable() {
        @Override
        public void run() {
            doneRefresh();
        }
    };

    private RefreshBehavior mBehavior = new RefreshBehavior() {
        @Override
        public void onRefreshBegin(final IRefreshLayout frame, final int direction) {
            switch (direction) {
                case IRefreshLayout.DIRECTION_DOWN:
                    if (null != mRefreshListener) {
                        mRefreshListener.onRefresh();
                    }
                    //动画开始后,执行超时检查
                    postDelayed(timeoutChecker, DELAY_TIME);
                    break;
                case IRefreshLayout.DIRECTION_UP:
                    if (null != mLoadListener) {
                        mLoadListener.onLoad();
                    }
                    //动画开始后,执行超时检查
                    postDelayed(timeoutChecker, DELAY_TIME);
                    break;
            }
        }
    };

    public PullToRefreshLayout(Context context) {
        super(context);
        initView();
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        mHeaderView = new JJZBRefreshView(getContext(), true);
        mFooterView = new JJZBRefreshView(getContext(), false);
        setRefreshBehavior(mBehavior);
        setHeaderView(mHeaderView);
        //setFooterView(mFooterView);
    }

    /**
     * 设置刷新监听器
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mRefreshListener = listener;
    }

    public void setOnLoadListener(OnLoadListener listener) {
        this.mLoadListener = listener;
    }

    public void setOnRefreshLoadListener(OnRefreshLoadListener listener) {
        setOnRefreshListener(listener);
        setOnLoadListener(listener);
    }

    public void enableLoadMore(boolean bool) {
        if (bool) {
            setEnableLoadMoreSuper(true);
            if (getFooterView() != mFooterView) {
                setFooterView(mFooterView);
            }
        } else {
            setEnableLoadMoreSuper(false);
        }
    }

    @Override
    public void setEnableLoadMore(boolean enableLoadMore) {
        //super.setEnableLoadMore(enableLoadMore);
        enableLoadMore(enableLoadMore);
    }

    private void setEnableLoadMoreSuper(boolean enableLoadMore) {
        super.setEnableLoadMore(enableLoadMore);
    }

    /**
     * 刷新接口
     */
    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadListener {
        void onLoad();
    }

    /**
     * 刷新加载接口
     */
    public interface OnRefreshLoadListener extends OnRefreshListener, OnLoadListener {
        void onRefresh();

        void onLoad();
    }
}
