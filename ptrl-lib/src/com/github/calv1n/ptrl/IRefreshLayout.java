package com.github.calv1n.ptrl;

import android.view.View;

/**
 * 刷新控件接口
 * Created by Calvin on 2016/4/6.
 */
public interface IRefreshLayout {

    // status enum
    byte STATUS_INIT = 1;
    byte STATUS_PREPARE = 2;
    byte STATUS_REFRESHING = 3;
    byte STATUS_COMPLETE = 4;
    int DIRECTION_DOWN = -1;
    int DIRECTION_UP = 1;

    void doRefresh();

    void doLoadMore();

    void doneRefresh();

    boolean isRefreshing();

    void setRefreshBehavior(RefreshBehavior refreshBehavior);

    void setHeaderHandler(IRefreshHandler refreshHandler);

    void setFooterHandler(IRefreshHandler refreshHandler);

    void setDisableWhenHorizontalMove(boolean disable);

    void setResistance(float resistance);

    void setKeepVisibleWhenRefresh(boolean keepOrNot);

    void setEnableRefresh(boolean enableRefresh);

    void setEnableLoadMore(boolean enableLoadMore);

    View getContentView();

    void setContentView(View content);

    /**
     * @return {@see IRefreshLayout#DIRECTION_DOWN}
     */
    int getRefreshingDirection();

    int getOffsetToRefresh();

    void setOffsetToRefresh(int offset);

    int getOffsetToLoadMore();

    void setOffsetToLoadMore(int offset);
}
