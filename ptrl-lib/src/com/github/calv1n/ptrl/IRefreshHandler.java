package com.github.calv1n.ptrl;

/**
 *
 */
public interface IRefreshHandler {

    /**
     * When the content view has reached top and refresh has been completed,
     * view will be reset.
     */
    void onReset(IRefreshLayout frame);

    /**
     * prepare for loading
     */
    void onRefreshPrepare(IRefreshLayout frame);

    /**
     * perform refreshing UI
     */
    void onRefreshBegin(IRefreshLayout frame);

    /**
     * perform UI after refresh
     */
    void onRefreshComplete(IRefreshLayout frame);

    void onPositionChange(IRefreshLayout frame, boolean isUnderTouch, byte status,
            ViewScrollHelper viewScrollHelper);
}
