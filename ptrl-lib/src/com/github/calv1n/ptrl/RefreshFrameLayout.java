package com.github.calv1n.ptrl;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;
import com.github.calv1n.LogUtil;

/**
 * This layout view for "Pull to Refresh" support all of the view, you can
 * contain everything you want.
 * support: pull to refresh / release to refresh / auto refresh / keep header
 * view while refreshing / hide header view while refreshing
 * It defines {@link IRefreshHandler}, which allows you customize the UI easily.
 */
public class RefreshFrameLayout extends ViewGroup implements IRefreshLayout {

    public static boolean DEBUG = true;
    // auto refresh status
    private static byte FLAG_AUTO_REFRESH_AT_ONCE = 0x01;
    private static byte FLAG_AUTO_REFRESH_BUT_LATER = 0x01 << 1;
    private static byte FLAG_ENABLE_NEXT_REFRESH_AT_ONCE = 0x01 << 2;
    private static byte FLAG_PIN_CONTENT = 0x01 << 3;
    private static byte MASK_AUTO_REFRESH = 0x03;
    protected View mContentView;
    private byte mStatus = STATUS_INIT;
    private int mRefreshingDirection;
    // config
    private int mDurationToClose = 200;
    private int mDurationToCloseHeader = 600;
    private int mDurationToCloseFooter = 600;
    private boolean mKeepVisibleWhenRefresh = true;
    //是否到达刷新点后立即执行
    private boolean mEnableAtOnceRefresh = false;
    private boolean mEnableAtOnceLoadMore = false;
    //是否开启上拉/加载功能
    private boolean mEnableRefresh = false;
    private boolean mEnableLoadMore = false;
    private boolean mTempDisableRefresh = false;
    private boolean mTempDisableLoadMore = false;
    private View mHeaderView;
    private View mFooterView;
    private IRefreshHandler mHeaderHandler;
    private IRefreshHandler mFooterHandler;
    private IRefreshHandler mRefreshingHandler;
    private RefreshBehavior mRefreshBehavior;
    // working parameters
    private ScrollChecker mScrollChecker;
    private int mTouchSlop;
    private int mHeaderHeight;
    private int mFooterHeight;
    private boolean mDisableWhenHorizontalMove = false;
    private int mFlag = 0x00;

    // disable when detect moving horizontally
    private boolean mPreventForHorizontal = false;

    private MotionEvent mLastMoveEvent;

    private int mRefreshingMinTime = 500;
    private long mRefreshingStartTime = 0;
    private ViewScrollHelper mScrollHelper;
    private boolean mHasSendCancelEvent = false;
    private Runnable mPerformRefreshCompleteDelay = new Runnable() {
        @Override
        public void run() {
            performRefreshComplete();
        }
    };
    private int mInitDownPointerIndex;
    private float mInitDownX;
    private float mInitDownY;

    public RefreshFrameLayout(Context context) {
        this(context, null);
    }

    public RefreshFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initAttrs(context, attrs);

        initView();

        mScrollChecker = new ScrollChecker();

        final ViewConfiguration conf = ViewConfiguration.get(getContext());
        mTouchSlop = conf.getScaledTouchSlop();
    }

    private void initView() {

    }

    private void initAttrs(Context context, AttributeSet attrs) {
        mScrollHelper = new ViewScrollHelper();

        TypedArray arr =
                context.obtainStyledAttributes(attrs, R.styleable.RefreshFrameLayout, 0, 0);
        if (arr != null) {
            mScrollHelper.setResistance(arr.getFloat(R.styleable.RefreshFrameLayout_resistance,
                    mScrollHelper.getResistance()));
            mDurationToClose =
                    arr.getInt(R.styleable.RefreshFrameLayout_duration_to_close, mDurationToClose);
            mDurationToCloseHeader =
                    arr.getInt(R.styleable.RefreshFrameLayout_duration_to_close_header,
                            mDurationToCloseHeader);
            mDurationToCloseFooter =
                    arr.getInt(R.styleable.RefreshFrameLayout_duration_to_close_footer,
                            mDurationToCloseFooter);
            mKeepVisibleWhenRefresh =
                    arr.getBoolean(R.styleable.RefreshFrameLayout_keep_header_when_refresh,
                            mKeepVisibleWhenRefresh);
            mEnableRefresh =
                    arr.getBoolean(R.styleable.RefreshFrameLayout_enable_refresh, mEnableRefresh);
            mEnableLoadMore =
                    arr.getBoolean(R.styleable.RefreshFrameLayout_enable_load, mEnableLoadMore);
            arr.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mHeaderView != null) {
            mHeaderView.bringToFront();
        }
        findContentView();
    }

    private void findContentView() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            if (!(view instanceof IRefreshHandler)) {
                mContentView = view;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mScrollChecker != null) {
            mScrollChecker.destroy();
        }

        if (mPerformRefreshCompleteDelay != null) {
            removeCallbacks(mPerformRefreshCompleteDelay);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mHeaderView != null) {
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            LayoutParams lp = (LayoutParams) mHeaderView.getLayoutParams();
            mHeaderHeight = mHeaderView.getMeasuredHeight() + lp.topMargin +
                    lp.bottomMargin;
            mScrollHelper.setHeight(mHeaderHeight);
        }
        if (mFooterView != null) {
            measureChildWithMargins(mFooterView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            LayoutParams lp = (LayoutParams) mFooterView.getLayoutParams();
            mFooterHeight = mFooterView.getMeasuredHeight() + lp.topMargin +
                    lp.bottomMargin;
            mScrollHelper.setFooterHeight(mFooterHeight);
        }
        if (mContentView != null) {
            measureChildWithMargins(mContentView, widthMeasureSpec, 0, heightMeasureSpec, 0);
        }
        if (DEBUG) {
            LogUtil.i("content, width: %s, height: %s, headerView height=%s,footerView height=%s",
                    getMeasuredWidth(), getMeasuredHeight(), mHeaderHeight, mFooterHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutChildren();
    }

    private void layoutChildren() {
        int offsetY = mScrollHelper.getCurrentPosY();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        if (mHeaderView != null) {
            LayoutParams lp = (LayoutParams) mHeaderView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin + offsetY - mHeaderHeight;
            final int right = left + mHeaderView.getMeasuredWidth();
            final int bottom = top + mHeaderView.getMeasuredHeight();
            mHeaderView.layout(left, top, right, bottom);
            if (DEBUG) {
                LogUtil.i("onLayout header: %s %s %s %s", left, top, right, bottom);
            }
        }
        if (mContentView != null) {
            if (isPinContent()) {
                offsetY = 0;
            }
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin + offsetY;
            final int right = left + mContentView.getMeasuredWidth();
            final int bottom = top + mContentView.getMeasuredHeight();
            if (DEBUG) {
                LogUtil.i("onLayout content: %s %s %s %s", left, top, right, bottom);
            }
            mContentView.layout(left, top, right, bottom);
        }

        if (mFooterView != null) {
            LayoutParams lp = (LayoutParams) mFooterView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = getMeasuredHeight() + lp.topMargin + offsetY;
            final int right = left + mFooterView.getMeasuredWidth();
            final int bottom = top + mFooterView.getMeasuredHeight();
            mFooterView.layout(left, top, right, bottom);
            if (DEBUG) {
                LogUtil.i("onLayout footer: %s %s %s %s", left, top, right, bottom);
            }
        }
    }

    public boolean dispatchTouchEventSupper(MotionEvent e) {
        return super.dispatchTouchEvent(e);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if ((!isEnableLoadMore() && !isEnableRefresh()) || mContentView == null) {
            return dispatchTouchEventSupper(e);
        }
        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //首次按下的手指编号
                mInitDownPointerIndex = e.getActionIndex();

                mHasSendCancelEvent = false;
                //记录初始按下位置
                mInitDownX = e.getX();
                mInitDownY = e.getY();
                mScrollHelper.onPressDown(mInitDownX, mInitDownY);

                mScrollChecker.abortIfWorking();

                mPreventForHorizontal = false;
                // The cancel event will be sent once the position is moved.
                // So let the event pass to children.
                // fix #93, #102
                dispatchTouchEventSupper(e);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                //多点触摸,另外有手指按下
                int actionIndex = e.getActionIndex();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //多点触摸,有手指抬起
                break;
            case MotionEvent.ACTION_MOVE:
                mLastMoveEvent = e;
                final float x = e.getX();
                final float y = e.getY();
                //初始滑动偏移量(方向)
                final float initOffsetY = y - mInitDownY;
                final int refreshingDirection = initOffsetY > 0f ? DIRECTION_DOWN : DIRECTION_UP;

                //记录当前位置,计算偏移量offset
                mScrollHelper.onMove(x, y);
                //计算摩擦系数后的位移
                float offsetX = mScrollHelper.getOffsetX();
                float offsetY = mScrollHelper.getOffsetY();

                if (mDisableWhenHorizontalMove && !mPreventForHorizontal && (Math.abs(offsetX)
                        > Math.abs(offsetY) && Math.abs(offsetX) > mTouchSlop * 2)) {
                    if (mScrollHelper.isInStartPosition()) {
                        mPreventForHorizontal = true;
                    }
                }
                //如果横向滑动或竖直滑动距离过小,不处理
                if (mPreventForHorizontal || Math.abs(initOffsetY) < mTouchSlop) {
                    return dispatchTouchEventSupper(e);
                }

                boolean movingDown = offsetY > 0;
                boolean movingUp = !movingDown;

                Boolean canLoadMore = null;
                Boolean canRefresh = null;
                if (DEBUG) {
                    //离开初始位置向下,则能向上移动
                    canLoadMore = checkCanLoadMore();
                    canRefresh = checkCanRefresh();
                    LogUtil.i(
                            "ACTION_MOVE: offsetY:%s, currentPos: %s,status=%s,movingUp: %s,  movingDown: %s: canLoadMore: %s,canRefresh:%s,refreshDirection=%s",
                            offsetY, mScrollHelper.getCurrentPosY(), STATUS_INIT, movingUp,
                            movingDown, canLoadMore, canRefresh, refreshingDirection);
                }
                // 不能再向上向下滑
                boolean isInit = mStatus == STATUS_INIT || (mStatus == STATUS_REFRESHING
                        && mScrollHelper.isInStartPosition());
                if ((movingDown && isInit && (mTempDisableRefresh || !checkCanRefresh())) || (
                        movingUp
                                && isInit
                                && (mTempDisableLoadMore || !checkCanLoadMore()))) {
                    LogUtil.d("不处理");
                    return dispatchTouchEventSupper(e);
                }

                //处理滑动
                movePosY(offsetY, refreshingDirection);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mScrollHelper.onRelease();
                //如果不在原始位置
                if (!mScrollHelper.isInStartPosition()) {
                    if (DEBUG) {
                        LogUtil.i("call onRelease when user release");
                    }
                    onRelease(false);
                    if (mScrollHelper.hasMovedAfterPressedDown()) {
                        sendCancelEvent();
                        return true;
                    }
                    return dispatchTouchEventSupper(e);
                } else {
                    return dispatchTouchEventSupper(e);
                }
        }
        return dispatchTouchEventSupper(e);
    }

    private boolean checkCanRefresh() {
        return mRefreshBehavior != null && isEnableRefresh() && mRefreshBehavior.checkCanDoRefresh(
                this, mContentView, DIRECTION_DOWN);
    }

    private boolean checkCanLoadMore() {
        return mRefreshBehavior != null && isEnableLoadMore() && mRefreshBehavior.checkCanDoRefresh(
                this, mContentView, DIRECTION_UP);
    }

    /**
     * if deltaY > 0, move the content down
     *
     * @param deltaY 要位移的距离
     */
    private void movePosY(float deltaY, int direction) {
        mRefreshingDirection = direction;

        switch (mRefreshingDirection) {
            case DIRECTION_DOWN:
                mTempDisableRefresh = false;
                boolean isRefreshingUnderTouch =
                        mStatus == STATUS_REFRESHING && mScrollHelper.isUnderTouch();
                boolean reachedTop = deltaY < 0 && mScrollHelper.isInStartPosition();
                if (isRefreshingUnderTouch && deltaY > 0 && mScrollHelper.hasAboveStartPosition()) {
                    //正在下拉刷新,下拉时候,当前下滑,
                    mRefreshingDirection = DIRECTION_UP;
                    if (DEBUG) {
                        LogUtil.d("change direction to up,deltaY=%s", deltaY);
                    }
                    //此时临时禁止刷新
                    mTempDisableRefresh = true;
                }
                // has reached the top
                // 初始刷新时,回到顶部后继续上滑
                if (reachedTop) {
                    if (DEBUG) {
                        LogUtil.d("has reached the top,deltaY=%s", deltaY);
                    }
                    return;
                    //mRefreshingDirection = DIRECTION_UP;
                }
                break;
            case DIRECTION_UP:
                mTempDisableLoadMore = false;
                isRefreshingUnderTouch =
                        mStatus == STATUS_REFRESHING && mScrollHelper.isUnderTouch();
                boolean reachedBottom = deltaY > 0 && mScrollHelper.isInStartPosition();
                if (isRefreshingUnderTouch && deltaY < 0 && mScrollHelper.hasBelowStartPosition()) {
                    //正在加载刷新,上拉加载时候,当前上滑
                    mRefreshingDirection = DIRECTION_DOWN;
                    if (DEBUG) {
                        LogUtil.d("change direction to down,deltaY=%s", deltaY);
                    }
                    //此时临时禁止加载更多
                    mTempDisableLoadMore = true;
                }
                // 初始加载更多时,回到底部后继续下滑
                if (reachedBottom) {
                    if (DEBUG) {
                        LogUtil.d("has reached the bottom, deltaY=%s", deltaY);
                    }
                    return;
                    //mRefreshingDirection = DIRECTION_DOWN;
                }
                break;
        }
        int to = mScrollHelper.getCurrentPosY() + (int) deltaY;

        switch (mRefreshingDirection) {
            case DIRECTION_DOWN:
                // over top
                if (mScrollHelper.willOverTop(to)) {
                    if (DEBUG) {
                        LogUtil.i("over top");
                    }
                    to = ViewScrollHelper.POS_START;
                }
                break;
            case DIRECTION_UP:
                // over top
                if (mScrollHelper.willOverBottom(to)) {
                    if (DEBUG) {
                        LogUtil.i("over bottom");
                    }
                    to = ViewScrollHelper.POS_START;
                }
                break;
        }
        //赋值
        mScrollHelper.setCurrentPosY(to);
        //再次计算差值距离
        int change = to - mScrollHelper.getLastPosY();
        updatePosY(change);
    }

    private void updatePosY(int change) {
        if (change == 0) {
            return;
        }

        boolean isUnderTouch = mScrollHelper.isUnderTouch();

        // once moved, cancel event will be sent to child
        if (isUnderTouch && !mHasSendCancelEvent &&
                mScrollHelper.hasMovedAfterPressedDown()) {
            mHasSendCancelEvent = true;
            sendCancelEvent();
        }

        View handlerView = null;
        boolean hasJustLeftStartPosition = false;
        switch (mRefreshingDirection) {
            case DIRECTION_DOWN:
                mRefreshingHandler = mHeaderHandler;
                handlerView = mHeaderView;
                hasJustLeftStartPosition = mScrollHelper.hasJustBelowStartPosition();
                break;
            case DIRECTION_UP:
                mRefreshingHandler = mFooterHandler;
                handlerView = mFooterView;
                hasJustLeftStartPosition = mScrollHelper.hasJustAboveStartPosition();
                break;
        }

        // 离开初始位置 or 刚刚完成刷新
        if ((hasJustLeftStartPosition && mStatus == STATUS_INIT)
                || (mScrollHelper.goCrossFinishPosition()
                && mStatus == STATUS_COMPLETE
                && isEnabledNextRefreshAtOnce())) {
            mStatus = STATUS_PREPARE;
            if (null != mRefreshingHandler) {
                mRefreshingHandler.onRefreshPrepare(this);
            }
            if (DEBUG) {
                LogUtil.i("IRefreshHandler: onRefreshPrepare, mFlag %s", mFlag);
            }
        }

        // 返回到初始位置
        if (mScrollHelper.hasJustReturnToStartPosition()) {
            tryToNotifyReset();

            // 还原上次移动时MotionEvent事件给子类recover event to children
            if (isUnderTouch) {
                sendDownEvent();
            }
        }

        boolean hasJustReachedRefreshLine = false;
        boolean isEnableAtOnceRefresh = false;
        boolean hasJustReachedHeight = false;
        switch (mRefreshingDirection) {
            case DIRECTION_DOWN:
                handlerView = mHeaderView;
                mRefreshingHandler = mHeaderHandler;
                hasJustReachedRefreshLine = mScrollHelper.hasJustReachedRefreshLine();
                isEnableAtOnceRefresh = isEnableAtOnceRefresh();
                hasJustReachedHeight = mScrollHelper.hasJustReachedHeaderHeight();
                break;
            case DIRECTION_UP:
                handlerView = mFooterView;
                mRefreshingHandler = mFooterHandler;
                hasJustReachedRefreshLine = mScrollHelper.hasJustReachedLoadMoreLine();
                isEnableAtOnceRefresh = isEnableAtOnceLoadMore();
                hasJustReachedHeight = mScrollHelper.hasJustReachedFooterHeight();
                break;
        }

        LogUtil.i(
                "status=%s,hasJustReachedRefreshLine=%s,hasJustReachedHeight=%s,isAutoRefrsh=%s,isAutoRefreshButLater=%s",
                mStatus, hasJustReachedRefreshLine, hasJustReachedHeight, isAutoRefresh(),
                performAutoRefreshButLater());
        //下拉到刷新
        if (mStatus == STATUS_PREPARE) {
            // 到达指定刷新高度reach fresh height while moving from top to bottom
            if (isUnderTouch
                    && !isAutoRefresh()
                    && isEnableAtOnceRefresh
                    && hasJustReachedRefreshLine) {
                tryToPerformRefresh();
            }
            // reach header height while auto refresh
            boolean autoRefreshButLater = performAutoRefreshButLater();
            if (autoRefreshButLater && hasJustReachedHeight) {
                tryToPerformRefresh();
            }
        }

        if (DEBUG) {
            LogUtil.i(
                    "updatePosY: change: %s, current: %s last: %s, top: %s, headerHeight: %s,footerHeight:%s,handlerView=%s",
                    change, mScrollHelper.getCurrentPosY(), mScrollHelper.getLastPosY(),
                    mContentView.getTop(), mHeaderHeight, mFooterHeight,
                    handlerView == null ? "null" : handlerView.getTag());
        }

        if (null != handlerView) {
            handlerView.offsetTopAndBottom(change);
        }
        if (!isPinContent()) {
            mContentView.offsetTopAndBottom(change);
        }
        invalidate();

        if (null != mRefreshingHandler) {
            mRefreshingHandler.onPositionChange(this, isUnderTouch, mStatus, mScrollHelper);
        }
        //位置改变
        onPositionChange(isUnderTouch, mStatus, mScrollHelper);
    }

    protected void onPositionChange(boolean isInTouching, byte status, ViewScrollHelper helper) {
    }

    /**
     * 释放后的动作
     *
     * @param stayForRefreshing 是否保持当前位置直接执行刷新
     */
    private void onRelease(boolean stayForRefreshing) {

        tryToPerformRefresh();

        if (mStatus == STATUS_REFRESHING) {
            // keep header for fresh
            if (mKeepVisibleWhenRefresh) {
                // scroll header back
                if (mScrollHelper.isOverOffsetToKeepWhileWorking(mRefreshingDirection)
                        && !stayForRefreshing) {
                    mScrollChecker.tryToScrollTo(
                            mScrollHelper.getOffsetToKeepWhileWorking(mRefreshingDirection),
                            mDurationToClose);
                } else {
                    // do nothing
                }
            } else {
                tryScrollBackToTopWhileRefreshing();
            }
        } else {
            if (mStatus == STATUS_COMPLETE) {
                notifyUIRefreshComplete();
            } else {
                tryScrollBackToTopAbortRefresh();
            }
        }
    }

    /**
     * Scroll back to to if is not under touch
     */
    private void tryScrollBackToStart() {
        if (!mScrollHelper.isUnderTouch()) {
            mScrollChecker.tryToScrollTo(ViewScrollHelper.POS_START, mDurationToCloseHeader);
        }
    }

    /**
     * just make easier to understand
     */
    private void tryScrollBackToTopWhileRefreshing() {
        tryScrollBackToStart();
    }

    /**
     * just make easier to understand
     */
    private void tryScrollBackToStartAfterComplete() {
        tryScrollBackToStart();
    }

    /**
     * just make easier to understand
     */
    private void tryScrollBackToTopAbortRefresh() {
        tryScrollBackToStart();
    }

    /**
     * 尝试执行刷新
     */
    private boolean tryToPerformRefresh() {
        if (mStatus != STATUS_PREPARE) {
            return false;
        }

        if (DEBUG) {
            LogUtil.i("tryToPerformRefresh");
        }
        //
        if ((mScrollHelper.isOverOffsetToKeepWhileWorking(mRefreshingDirection) && isAutoRefresh())
                || mScrollHelper.isOverOffsetToWork(mRefreshingDirection)) {
            mStatus = STATUS_REFRESHING;
            performRefresh();
        }
        return false;
    }

    /**
     * 执行刷新操作
     */
    private void performRefresh() {
        mRefreshingStartTime = System.currentTimeMillis();
        if (null != mRefreshingHandler) {
            mRefreshingHandler.onRefreshBegin(this);
            if (DEBUG) {
                LogUtil.i("IRefreshHandler: onRefreshBegin");
            }
        }
        if (mRefreshBehavior != null) {
            mRefreshBehavior.onRefreshBegin(this, mRefreshingDirection);
        }
    }

    /**
     * If at the top and not in loading, reset
     */
    private boolean tryToNotifyReset() {
        if ((mStatus == STATUS_COMPLETE || mStatus == STATUS_PREPARE)
                && mScrollHelper.isInStartPosition()) {
            if (null != mRefreshingHandler) {
                mRefreshingHandler.onReset(this);
                if (DEBUG) {
                    LogUtil.i("IRefreshHandler: onReset");
                }
            }
            mStatus = STATUS_INIT;
            mRefreshingHandler = null;
            mRefreshingDirection = 0;
            mTempDisableLoadMore = false;
            mTempDisableRefresh = false;
            clearFlag();
            return true;
        }
        return false;
    }

    protected void onRefreshScrollAbort() {
        if (mScrollHelper.hasBelowStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                LogUtil.i("call onRelease after scroll abort");
            }
            onRelease(true);
        }
    }

    protected void onRefreshScrollFinish() {
        if (mScrollHelper.hasBelowStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                LogUtil.i("call onRelease after scroll finish");
            }
            onRelease(true);
        }
    }

    public int getRefreshingDirection() {
        return mRefreshingDirection;
    }

    /**
     * Detect whether is refreshing.
     */
    public boolean isRefreshing() {
        return mStatus == STATUS_REFRESHING;
    }

    /**
     * Call this when data is refreshed.
     * The UI will perform complete at once or after a delay, depends on the
     * time elapsed is greater then {@link #mRefreshingMinTime} or not.
     */
    final public void doneRefresh() {
        if (DEBUG) {
            LogUtil.i("doneRefresh");
        }

        int delay =
                (int) (mRefreshingMinTime - (System.currentTimeMillis() - mRefreshingStartTime));
        if (delay <= 0) {
            if (DEBUG) {
                LogUtil.i("perform Refresh Complete at once");
            }
            performRefreshComplete();
        } else {
            postDelayed(mPerformRefreshCompleteDelay, delay);
            if (DEBUG) {
                LogUtil.i("perform Refresh Complete after delay: %s", delay);
            }
        }
    }

    /**
     * Do refresh complete work when time elapsed is greater than {@link
     * #mRefreshingMinTime}
     */
    private void performRefreshComplete() {
        mStatus = STATUS_COMPLETE;

        // if is auto refresh do nothing, wait scroller stop
        if (mScrollChecker.mIsRunning && isAutoRefresh()) {
            // do nothing
            if (DEBUG) {
                LogUtil.i("performRefreshComplete do nothing, scrolling: %s, auto refresh: %s",
                        mScrollChecker.mIsRunning, mFlag);
            }
            return;
        }

        notifyUIRefreshComplete();
    }

    /**
     * Do real refresh work. If there is a hook, execute the hook first.
     */
    private void notifyUIRefreshComplete() {
        if (null != mRefreshingHandler) {
            if (DEBUG) {
                LogUtil.i("IRefreshHandler: onRefreshComplete");
            }
            mRefreshingHandler.onRefreshComplete(this);
        }
        //// TODO: 2016/5/31 检查
        mScrollHelper.onUIRefreshComplete();
        tryScrollBackToStartAfterComplete();
        tryToNotifyReset();
    }

    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    public int getFooterHeight() {
        return mFooterHeight;
    }

    public void doRefresh() {
        autoRefresh(true, mDurationToCloseHeader, DIRECTION_DOWN);
    }

    @Override
    public void doLoadMore() {
        autoRefresh(true, mDurationToCloseFooter, DIRECTION_UP);
    }

    public void autoRefresh(boolean atOnce, int direction) {
        autoRefresh(atOnce, mDurationToCloseHeader, direction);
    }

    private void clearFlag() {
        // remove auto fresh flag
        mFlag = mFlag & ~MASK_AUTO_REFRESH;
    }

    /**
     * @param atOnce 是否立即执行
     * @param duration 下拉动画执行时长
     * @param direction 刷新方向
     */
    public void autoRefresh(boolean atOnce, int duration, int direction) {

        if (mStatus != STATUS_INIT) {
            return;
        }

        mFlag |= atOnce ? FLAG_AUTO_REFRESH_AT_ONCE : FLAG_AUTO_REFRESH_BUT_LATER;

        mStatus = STATUS_PREPARE;
        mRefreshingDirection = direction;
        int offsetToRefresh = 0;
        switch (direction) {
            case DIRECTION_DOWN:
                mRefreshingHandler = mHeaderHandler;
                offsetToRefresh = mScrollHelper.getOffsetToRefresh();
                break;
            case DIRECTION_UP:
                mRefreshingHandler = mFooterHandler;
                offsetToRefresh = mScrollHelper.getOffsetToLoadMore();
                break;
        }

        if (null != mRefreshingHandler) {
            mRefreshingHandler.onRefreshPrepare(this);
            if (DEBUG) {
                LogUtil.i("IRefreshHandler: onRefreshPrepare, mFlag %s", mFlag);
            }
        }
        mScrollChecker.tryToScrollTo(offsetToRefresh, duration);
        if (atOnce) {
            mStatus = STATUS_REFRESHING;
            performRefresh();
        }
    }

    public boolean isAutoRefresh() {
        return (mFlag & MASK_AUTO_REFRESH) > 0;
    }

    private boolean performAutoRefreshButLater() {
        return (mFlag & MASK_AUTO_REFRESH) == FLAG_AUTO_REFRESH_BUT_LATER;
    }

    public boolean isEnabledNextRefreshAtOnce() {
        return (mFlag & FLAG_ENABLE_NEXT_REFRESH_AT_ONCE) > 0;
    }

    /**
     * If @param enable has been set to true. The user can perform next PTR at
     * once.
     */
    public void setEnabledNextPtrAtOnce(boolean enable) {
        if (enable) {
            mFlag = mFlag | FLAG_ENABLE_NEXT_REFRESH_AT_ONCE;
        } else {
            mFlag = mFlag & ~FLAG_ENABLE_NEXT_REFRESH_AT_ONCE;
        }
    }

    public boolean isPinContent() {
        return (mFlag & FLAG_PIN_CONTENT) > 0;
    }

    /**
     * The content view will now move when {@param pinContent} set to true.
     */
    public void setPinContent(boolean pinContent) {
        if (pinContent) {
            mFlag = mFlag | FLAG_PIN_CONTENT;
        } else {
            mFlag = mFlag & ~FLAG_PIN_CONTENT;
        }
    }

    /**
     * It's useful when working with viewpager.
     */
    public void setDisableWhenHorizontalMove(boolean disable) {
        mDisableWhenHorizontalMove = disable;
    }

    /**
     * loading will last at least for so long
     */
    public void setRefreshingMinTime(int time) {
        mRefreshingMinTime = time;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    @Override
    public void setContentView(View content) {
        if (mContentView != null && content != null && mContentView != content) {
            removeView(mContentView);
        }
        ViewGroup.LayoutParams lp = content.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -1);
            content.setLayoutParams(lp);
        }
        mContentView = content;
        addView(content);

        if (null != mHeaderView) {
            bringChildToFront(mHeaderView);
        }
    }

    @Override
    public void setRefreshBehavior(RefreshBehavior refreshBehavior) {
        mRefreshBehavior = refreshBehavior;
    }

    @Override
    public void setHeaderHandler(IRefreshHandler headerHandler) {
        this.mHeaderHandler = headerHandler;
    }

    @Override
    public void setFooterHandler(IRefreshHandler footerHandler) {
        mFooterHandler = footerHandler;
    }

    public void setScrollHelper(ViewScrollHelper helper) {
        if (mScrollHelper != null && mScrollHelper != helper) {
            helper.convertFrom(mScrollHelper);
        }
        mScrollHelper = helper;
    }

    public float getResistance() {
        return mScrollHelper.getResistance();
    }

    public void setResistance(float resistance) {
        mScrollHelper.setResistance(resistance);
    }

    public float getDurationToClose() {
        return mDurationToClose;
    }

    /**
     * The duration to return back to the refresh position
     */
    public void setDurationToClose(int duration) {
        mDurationToClose = duration;
    }

    public long getDurationToCloseHeader() {
        return mDurationToCloseHeader;
    }

    /**
     * The duration to close time
     */
    public void setDurationToCloseHeader(int duration) {
        mDurationToCloseHeader = duration;
    }

    public void setRatioOfHeightToRefresh(float ratio) {
        mScrollHelper.setRatioOfHeightToRefresh(ratio);
    }

    public void setRatioOfHeightToLoadMore(float ratio) {
        mScrollHelper.setRatioOfHeightToLoadMore(ratio);
    }

    @Override
    public int getOffsetToRefresh() {
        return mScrollHelper.getOffsetToRefresh();
    }

    @Override
    public void setOffsetToRefresh(int offset) {
        mScrollHelper.setOffsetToRefresh(offset);
    }

    @Override
    public int getOffsetToLoadMore() {
        return mScrollHelper.getOffsetToLoadMore();
    }

    @Override
    public void setOffsetToLoadMore(int offset) {
        mScrollHelper.setOffsetToLoadMore(offset);
    }

    public float getRatioOfHeightRefresh() {
        return mScrollHelper.getRatioOfHeightRefresh();
    }

    public int getOffsetToKeepWhileLoading() {
        return mScrollHelper.getOffsetToKeepWhileRefreshing();
    }

    public void setOffsetToKeepWhileLoading(int offset) {
        mScrollHelper.setOffsetToKeepWhileLoading(offset);
    }

    public boolean isKeepVisibleWhenRefresh() {
        return mKeepVisibleWhenRefresh;
    }

    public void setKeepVisibleWhenRefresh(boolean keepOrNot) {
        mKeepVisibleWhenRefresh = keepOrNot;
    }

    public boolean isEnableRefresh() {
        return null != mHeaderView && mEnableRefresh;
    }

    @Override
    public void setEnableRefresh(boolean enableRefresh) {
        mEnableRefresh = enableRefresh;
    }

    public boolean isEnableLoadMore() {
        return null != mFooterView && mEnableLoadMore;
    }

    @Override
    public void setEnableLoadMore(boolean enableLoadMore) {
        this.mEnableLoadMore = enableLoadMore;
    }

    public boolean isEnableAtOnceRefresh() {
        return mEnableAtOnceRefresh;
    }

    public void setEnableAtOnceRefresh(boolean enableAtOnceRefresh) {
        mEnableAtOnceRefresh = enableAtOnceRefresh;
    }

    public boolean isEnableAtOnceLoadMore() {
        return mEnableAtOnceLoadMore;
    }

    public void setEnableAtOnceLoadMore(boolean enableAtOnceLoadMore) {
        mEnableAtOnceLoadMore = enableAtOnceLoadMore;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    public void setHeaderView(View header) {
        mEnableRefresh = true;
        if (mHeaderView != null && header != null && mHeaderView != header) {
            removeView(mHeaderView);
        }
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            header.setLayoutParams(lp);
        }
        mHeaderView = header;
        mHeaderView.setTag("header");
        if (mHeaderView instanceof IRefreshHandler) {
            setHeaderHandler((IRefreshHandler) mHeaderView);
        }
        addView(header);
    }

    public View getFooterView() {
        return this.mFooterView;
    }

    public void setFooterView(View footer) {
        //添加footer默认开启上拉加载功能
        mEnableLoadMore = true;
        if (mFooterView != null && footer != null && mFooterView != footer) {
            removeView(mFooterView);
        }
        ViewGroup.LayoutParams lp = footer.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            footer.setLayoutParams(lp);
        }
        mFooterView = footer;
        mFooterView.setTag("footer");
        if (mFooterView instanceof IRefreshHandler) {
            setFooterHandler((IRefreshHandler) mFooterView);
        }
        addView(footer);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    private void sendCancelEvent() {
        if (DEBUG) {
            LogUtil.i("send cancel event");
        }
        // The ScrollChecker will update position and lead to send cancel event when mLastMoveEvent is null.
        // fix #104, #80, #92
        if (mLastMoveEvent == null) {
            return;
        }
        MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(),
                last.getEventTime() + ViewConfiguration.getLongPressTimeout(),
                MotionEvent.ACTION_CANCEL, last.getX(), last.getY(), last.getMetaState());
        dispatchTouchEventSupper(e);
    }

    private void sendDownEvent() {
        if (DEBUG) {
            LogUtil.i("send down event");
        }
        final MotionEvent last = mLastMoveEvent;
        MotionEvent e =
                MotionEvent.obtain(last.getDownTime(), last.getEventTime(), MotionEvent.ACTION_DOWN,
                        last.getX(), last.getY(), last.getMetaState());
        dispatchTouchEventSupper(e);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        @SuppressWarnings({ "unused" })
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    class ScrollChecker implements Runnable {

        private int mLastFlingY;
        private Scroller mScroller;
        private boolean mIsRunning = false;
        private int mStart;
        private int mTo;

        public ScrollChecker() {
            mScroller = new Scroller(getContext());
        }

        public void run() {
            boolean finish = !mScroller.computeScrollOffset() || mScroller.isFinished();
            int curY = mScroller.getCurrY();
            int deltaY = curY - mLastFlingY;
            if (DEBUG) {
                if (deltaY != 0) {
                    LogUtil.i(
                            "scroll finish: %s, start: %s, to: %s, currentPos: %s, current :%s, last: %s, delta: %s",
                            finish, mStart, mTo, mScrollHelper.getCurrentPosY(), curY, mLastFlingY,
                            deltaY);
                }
            }
            if (!finish) {
                mLastFlingY = curY;
                movePosY(deltaY, mRefreshingDirection);
                post(this);
            } else {
                finish();
            }
        }

        private void finish() {
            if (DEBUG) {
                LogUtil.i("finish, currentPos:%s", mScrollHelper.getCurrentPosY());
            }
            reset();
            onRefreshScrollFinish();
        }

        private void reset() {
            mIsRunning = false;
            mLastFlingY = 0;
            removeCallbacks(this);
        }

        private void destroy() {
            reset();
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
        }

        public void abortIfWorking() {
            if (mIsRunning) {
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }
                onRefreshScrollAbort();
                reset();
            }
        }

        public void tryToScrollTo(int to, int duration) {
            if (mScrollHelper.isAlreadyHere(to)) {
                return;
            }
            mStart = mScrollHelper.getCurrentPosY();
            mTo = to;
            int distance = to - mStart;
            if (DEBUG) {
                LogUtil.i("tryToScrollTo: start: %s, distance:%s, to:%s", mStart, distance, to);
            }
            removeCallbacks(this);

            mLastFlingY = 0;

            // fix #47: Scroller should be reused, https://github.com/liaohuqiu/android-Ultra-Pull-To-Refresh/issues/47
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            mScroller.startScroll(0, 0, 0, distance, duration);
            post(this);
            mIsRunning = true;
        }
    }
}
