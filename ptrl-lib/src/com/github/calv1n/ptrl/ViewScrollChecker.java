package com.github.calv1n.ptrl;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import com.github.calv1n.LogUtil;

/**
 * Created by biaowu.
 */
public class ViewScrollChecker {

    private static final boolean DEBUG_SCROLL_CHECK = false;

    public static boolean isDirectionDown(byte direction) {
        return direction == IRefreshLayout.DIRECTION_DOWN;
    }

    public static boolean isDirectionUp(byte direction) {
        return direction == IRefreshLayout.DIRECTION_UP;
    }

    public static boolean canViewScrollVerticallyDown(View view) {
        return canViewScrollVertically(view, IRefreshLayout.DIRECTION_DOWN);
    }

    public static boolean canViewScrollVerticallyUp(View view) {
        return canViewScrollVertically(view, IRefreshLayout.DIRECTION_UP);
    }

    /**
     * 该方法递归遍历子控件检查是否有可滑动的view
     */
    public static boolean canViewScrollVertically(View view, int direction) {
        if (checkViewScrollVertically(view, direction)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            int childCount = ((ViewGroup) view).getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    View childAt = ((ViewGroup) view).getChildAt(i);
                    if (canViewScrollVertically(childAt, direction)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean checkViewScrollVertically(View view, int direction) {
        boolean result;
        if (Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {//list view etc.
                result = performAbsListView((AbsListView) view, direction);
            } else {
                try {//i known it's a bad way!
                    Class<? extends View> viewClass = view.getClass();
                    final int offset =
                            (int) viewClass.getDeclaredMethod("computeVerticalScrollOffset")
                                    .invoke(view);
                    final int range =
                            (int) viewClass.getDeclaredMethod("computeVerticalScrollRange")
                                    .invoke(view) - (int) viewClass.getDeclaredMethod(
                                    "computeVerticalScrollExtent").invoke(view);
                    if (range == 0) return false;
                    if (direction < 0) {
                        result = (offset > 0);
                    } else {
                        result = offset < range - 1;
                    }
                } catch (Exception e) {
                    if (DEBUG_SCROLL_CHECK) LogUtil.e("no such method!!");
                    result = view.getScrollY() > 0;
                }
            }
        } else {
            result = view.canScrollVertically(direction);
        }
        if (DEBUG_SCROLL_CHECK) {
            LogUtil.e("view = %s , direction is %s, canViewScrollVertically = %s", view,
                    (direction > 0 ? "up" : "down"), result);
        }
        return result;
    }

    private static boolean performAbsListView(AbsListView view, int direction) {
        int childCount = view.getChildCount();
        if (childCount > 0) {
            switch (direction) {
                case IRefreshLayout.DIRECTION_DOWN:
                    ViewGroup.MarginLayoutParams lp =
                            (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                    int firstItemTop = view.getChildAt(0).getTop();
                    int listViewTop = view.getTop() + view.getPaddingTop() - lp.topMargin;
                    if (DEBUG_SCROLL_CHECK) {
                        LogUtil.e("firstItemTop=%s,listViewTop=%s", firstItemTop, listViewTop);
                    }
                    return (view.getFirstVisiblePosition() > 0 || firstItemTop < listViewTop);
                case IRefreshLayout.DIRECTION_UP:
                    int lastItemBottom = view.getChildAt(childCount - 1).getBottom();
                    int listViewBottom = view.getBottom() - view.getPaddingBottom();
                    if (DEBUG_SCROLL_CHECK) {
                        LogUtil.e("lastItemBottom=%s,listViewBottom=%s", lastItemBottom,
                                listViewBottom);
                    }
                    return (view.getLastVisiblePosition() < childCount - 1
                            || lastItemBottom > listViewBottom);
            }
        }
        if (DEBUG_SCROLL_CHECK) {
            LogUtil.e("AbsListView cannot scroll vertically or childCount is 0!!");
        }
        return false;
    }
}
