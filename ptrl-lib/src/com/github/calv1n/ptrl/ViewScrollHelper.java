package com.github.calv1n.ptrl;

import android.graphics.PointF;

public class ViewScrollHelper {

    public final static int POS_START = 0;
    protected int mOffsetToRefresh = 0;
    protected int mOffsetToLoadMore = 0;
    private PointF mLastMove = new PointF();
    private float mOffsetX;
    private float mOffsetY;
    private int mCurrentPosY = 0;
    private int mLastPosY = 0;
    private int mPressedPosY = 0;
    private int mHeight;
    private int mFooterHeight;

    private float mRatioOfHeightToRefresh = 1f;
    private float mRatioOfHeightToLoadMore = 1f;
    private float mResistance = 1.7f;
    private boolean mIsUnderTouch = false;

    private Integer mOffsetToKeepWhileRefreshing;
    private Integer mOffsetToKeepWhileLoading;
    // record the refresh complete position
    private int mRefreshCompleteY = 0;

    public boolean isUnderTouch() {
        return mIsUnderTouch;
    }

    public float getResistance() {
        return mResistance;
    }

    public void setResistance(float resistance) {
        mResistance = resistance;
    }

    public void onRelease() {
        mIsUnderTouch = false;
    }

    public void onUIRefreshComplete() {
        mRefreshCompleteY = mCurrentPosY;
    }

    public boolean goCrossFinishPosition() {
        return Math.abs(mCurrentPosY) >= mRefreshCompleteY;
    }

    protected void processOnMove(float offsetX, float offsetY) {
        setOffset(offsetX, offsetY / mResistance);
    }

    protected void processOnMove(float currentX, float currentY, float offsetX, float offsetY) {
        setOffset(offsetX, offsetY / mResistance);
    }

    public void setRatioOfHeightToRefresh(float ratio) {
        mRatioOfHeightToRefresh = ratio;
        mOffsetToRefresh = (int) (mHeight * ratio);
    }

    public float getRatioOfHeightRefresh() {
        return mRatioOfHeightToRefresh;
    }

    public float getRatioOfHeightToLoadMore() {
        return mRatioOfHeightToLoadMore;
    }

    public void setRatioOfHeightToLoadMore(float ratio) {
        mRatioOfHeightToLoadMore = ratio;
        mOffsetToLoadMore = (int) (-mFooterHeight * ratio);
    }

    public int getOffsetToRefresh() {
        return mOffsetToRefresh;
    }

    public void setOffsetToRefresh(int offset) {
        mRatioOfHeightToRefresh = mHeight * 1f / offset;
        mOffsetToRefresh = offset;
    }

    public int getOffsetToLoadMore() {
        return mOffsetToLoadMore;
    }

    public void setOffsetToLoadMore(int offset) {
        mRatioOfHeightToLoadMore = mFooterHeight * 1f / offset;
        mOffsetToLoadMore = -offset;
    }

    public void onPressDown(float x, float y) {
        mIsUnderTouch = true;
        mPressedPosY = mCurrentPosY;
        mLastMove.set(x, y);
    }

    public final void onMove(float x, float y) {
        float offsetX = x - mLastMove.x;
        float offsetY = y - mLastMove.y;
        //根据摩擦系数处理实际偏移量
        processOnMove(offsetX, offsetY);
        mLastMove.set(x, y);
    }

    protected void setOffset(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getOffsetY() {
        return mOffsetY;
    }

    public int getLastPosY() {
        return mLastPosY;
    }

    public int getCurrentPosY() {
        return mCurrentPosY;
    }

    /**
     * Update current position before update the UI
     */
    public final void setCurrentPosY(int current) {
        mLastPosY = mCurrentPosY;
        mCurrentPosY = current;
        onUpdatePos(current, mLastPosY);
    }

    protected void onUpdatePos(int current, int last) {

    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
        updateHeaderHeight();
    }

    public void setFooterHeight(int height) {
        mFooterHeight = height;
        updateFooterHeight();
    }

    protected void updateHeaderHeight() {
        mOffsetToRefresh = (int) (mRatioOfHeightToRefresh * mHeight);
    }

    protected void updateFooterHeight() {
        mOffsetToLoadMore = (int) (-mRatioOfHeightToLoadMore * mFooterHeight);
    }

    public void convertFrom(ViewScrollHelper helper) {
        mCurrentPosY = helper.mCurrentPosY;
        mLastPosY = helper.mLastPosY;
        mHeight = helper.mHeight;
        mFooterHeight = helper.mFooterHeight;
    }

    public boolean hasBelowStartPosition() {
        return mCurrentPosY > POS_START;
    }

    public boolean hasAboveStartPosition() {
        return mCurrentPosY < POS_START;
    }

    public boolean hasJustBelowStartPosition() {
        return mLastPosY == POS_START && hasBelowStartPosition();
    }

    public boolean hasJustAboveStartPosition() {
        return mLastPosY == POS_START && hasAboveStartPosition();
    }

    public boolean hasJustReturnToStartPosition() {
        return mLastPosY != POS_START && isInStartPosition();
    }

    public boolean isOverOffsetToWork(int direction) {
        switch (direction) {
            case IRefreshLayout.DIRECTION_DOWN:
                return isOverOffsetToRefresh();
            case IRefreshLayout.DIRECTION_UP:
                return isOverOffsetToLoadMore();
        }
        return false;
    }

    public boolean isOverOffsetToRefresh() {
        return Math.abs(mCurrentPosY) >= getOffsetToRefresh();
    }

    public boolean isOverOffsetToLoadMore() {
        return Math.abs(mCurrentPosY) >= -getOffsetToLoadMore();
    }

    public boolean hasMovedAfterPressedDown() {
        return mCurrentPosY != mPressedPosY;
    }

    public boolean isInStartPosition() {
        return mCurrentPosY == POS_START;
    }

    public boolean hasJustReachedRefreshLine() {
        return Math.abs(mLastPosY) < getOffsetToRefresh()
                && Math.abs(mCurrentPosY) >= getOffsetToRefresh();
    }

    public boolean hasJustReturnedRefreshLine() {
        return Math.abs(mLastPosY) > getOffsetToRefresh()
                && Math.abs(mCurrentPosY) <= getOffsetToRefresh();
    }

    public boolean hasJustReturnedLoadMoreLine() {
        return Math.abs(mLastPosY) > -getOffsetToLoadMore()
                && Math.abs(mCurrentPosY) <= -getOffsetToLoadMore();
    }

    public boolean hasJustReachedLoadMoreLine() {
        return Math.abs(mLastPosY) < -getOffsetToLoadMore()
                && Math.abs(mCurrentPosY) >= -getOffsetToLoadMore();
    }

    public boolean hasJustReachedHeaderHeight() {
        return Math.abs(mLastPosY) < mHeight && Math.abs(mCurrentPosY) >= mHeight;
    }

    public boolean hasJustReturnedHeaderHeight() {
        return Math.abs(mLastPosY) > mHeight && Math.abs(mCurrentPosY) <= mHeight;
    }

    public boolean hasJustReachedFooterHeight() {
        return Math.abs(mLastPosY) < mFooterHeight && Math.abs(mCurrentPosY) >= mFooterHeight;
    }

    public boolean hasJustReturneddFooterHeight() {
        return Math.abs(mLastPosY) > mFooterHeight && Math.abs(mCurrentPosY) <= mFooterHeight;
    }

    public boolean isOverOffsetToKeepWhileWorking(int direction) {
        switch (direction) {
            case IRefreshLayout.DIRECTION_DOWN:
                return isOverOffsetToKeepWhileRefreshing();
            case IRefreshLayout.DIRECTION_UP:
                return isOverOffsetToKeepWhileLoading();
        }
        return false;
    }

    public boolean isOverOffsetToKeepWhileRefreshing() {
        return Math.abs(mCurrentPosY) > getOffsetToKeepWhileRefreshing();
    }

    public boolean isOverOffsetToKeepWhileLoading() {
        return Math.abs(mCurrentPosY) > getOffsetToKeepWhileLoading();
    }

    public int getOffsetToKeepWhileRefreshing() {
        return mOffsetToKeepWhileRefreshing != null ? mOffsetToKeepWhileRefreshing : mHeight;
    }

    public void setOffsetToKeepWhileRefreshing(int offset) {
        mOffsetToKeepWhileRefreshing = offset;
    }

    public int getOffsetToKeepWhileLoading() {
        return mOffsetToKeepWhileLoading != null ? mOffsetToKeepWhileLoading : -mFooterHeight;
    }

    public void setOffsetToKeepWhileLoading(int offset) {
        mOffsetToKeepWhileLoading = -offset;
    }

    public int getOffsetToKeepWhileWorking(int direction) {
        switch (direction) {
            case IRefreshLayout.DIRECTION_DOWN:
                return getOffsetToKeepWhileRefreshing();
            case IRefreshLayout.DIRECTION_UP:
                return getOffsetToKeepWhileLoading();
        }
        return 0;
    }

    public boolean isAlreadyHere(int to) {
        return mCurrentPosY == to;
    }

    public float getLastPercent() {
        return mHeight == 0 ? 0 : Math.abs(mLastPosY) * 1f / mHeight;
    }

    public float getCurrentPercent() {
        return mHeight == 0 ? 0 : Math.abs(mCurrentPosY) * 1f / mHeight;
    }

    public boolean willOverTop(int to) {
        return to < POS_START;
    }

    public boolean willOverBottom(int to) {
        return to > POS_START;
    }
}
