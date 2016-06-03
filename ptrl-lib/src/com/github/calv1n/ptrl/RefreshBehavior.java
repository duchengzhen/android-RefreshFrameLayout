package com.github.calv1n.ptrl;

import android.view.View;

public abstract class RefreshBehavior {

    /**
     * Check can do refresh or not. For example the content is empty or the
     * first child is in view.
     * <p>
     */
    public boolean checkCanDoRefresh(final IRefreshLayout frame, final View content,
            final int direction) {
        switch (direction) {
            case IRefreshLayout.DIRECTION_DOWN:
                //内容不拦截向下滑动,说明可以下拉刷新
                return !ViewScrollChecker.canViewScrollVerticallyDown(content);

            case IRefreshLayout.DIRECTION_UP:
                //内容不拦截向上滑动,说明可以上拉加载
                return !ViewScrollChecker.canViewScrollVerticallyUp(content);
        }
        return false;
    }

    /**
     * When refresh begin
     *
     * @param direction {@see IRefreshLayout.DIRECTION_DOWN}
     */
    public abstract void onRefreshBegin(final IRefreshLayout frame, final int direction);
}