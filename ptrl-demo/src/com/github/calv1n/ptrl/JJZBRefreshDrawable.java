package com.github.calv1n.ptrl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by Calvin on 2016/3/11 0011.
 */
public class JJZBRefreshDrawable extends Drawable implements Animatable {
    private final Context mContext;
    private final PorterDuffXfermode xfermode;
    private View mParent;
    private Paint mPaint;
    private Animation animation;
    private float mPer;
    private Bitmap mDstBitmap;
    private Bitmap mSrcBitmap;
    private Rect dstRect;
    private Rect srcRect;
    private Rect rect;
    private boolean isRuning;
    private boolean isRe;
    private String text = "正在加载...";

    private int defaultSize;
    private int defaultTextSize;
    private Rect bounds;
    private boolean isDrawText = true;

    public JJZBRefreshDrawable(View parent) {
        mParent = parent;
        mContext = parent.getContext();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.parseColor("#BA2E2E"));
        mPaint.setTextAlign(Paint.Align.CENTER);
        xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
        defaultSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                mContext.getResources().getDisplayMetrics());
        defaultTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f,
                mContext.getResources().getDisplayMetrics());
        mPaint.setTextSize(defaultTextSize);
        mPaint.setDither(true);
        //字体边框
        bounds = new Rect();
        createBitmaps();
        setupAnimations();
    }

    private void setupAnimations() {
        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                mPer = interpolatedTime;
                invalidateSelf();
            }
        };
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setDuration(800);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isRuning = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isRuning = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                isRe = !isRe;
            }
        });
    }

    private void createBitmaps() {
        mDstBitmap = BitmapFactory.decodeResource(mParent.getResources(),
                com.github.calv1n.ptrl.demo.R.drawable.ic_refreshing_dst);

        mSrcBitmap = BitmapFactory.decodeResource(mParent.getResources(),
                com.github.calv1n.ptrl.demo.R.drawable.ic_refreshing_src);

        rect = new Rect();

        srcRect = new Rect(0, 0, mSrcBitmap.getWidth(), mSrcBitmap.getHeight());
        dstRect = new Rect(0, 0, mDstBitmap.getWidth(), mDstBitmap.getHeight());
    }

    @Override
    public void start() {
        mParent.startAnimation(animation);
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRe = true;
        invalidateSelf();
    }

    @Override
    public boolean isRunning() {
        return isRuning;
    }

    @Override
    public void draw(Canvas canvas) {
        int h = mParent.getHeight();
        int w = mParent.getWidth();
        //防止parent控件过小过大
        int height = h == 0 || h > defaultSize ? defaultSize : h;
        int width = w == 0 || w > defaultSize ? defaultSize : w;

        int saveCount = canvas.save();
        //移动
        canvas.translate((w - width) / 2f, (h - height) / 2f);
        //首先绘制目标
        rect.right = width;
        rect.bottom = height;
        rect.top = 0;
        canvas.drawBitmap(mDstBitmap, dstRect, rect, mPaint);

        if (isRuning) {
            if (isRe) {
                srcRect.top = 0;
                srcRect.bottom = (int) (mSrcBitmap.getHeight() * mPer);
                rect.top = 0;
                rect.bottom = (int) (height * mPer);
            } else {
                srcRect.top = (int) (mSrcBitmap.getHeight() * mPer);
                srcRect.bottom = mSrcBitmap.getHeight();
                rect.top = (int) (height * mPer);
                rect.bottom = height;
            }

            mPaint.setXfermode(xfermode);
            //绘制源文件
            canvas.drawBitmap(mSrcBitmap, srcRect, rect, mPaint);
        }

        mPaint.setXfermode(null);

        if (isRuning && isDrawText) {
            canvas.save();
            mPaint.getTextBounds(text, 0, text.length() - 1, bounds);
            Paint.FontMetricsInt metricsInt = mPaint.getFontMetricsInt();
            canvas.translate(width / 2f, height + (bounds.bottom - bounds.top) / 1f
                    - (metricsInt.descent + metricsInt.ascent) / 2);
            canvas.drawText(text, 0, 0, mPaint);
        }

        canvas.restoreToCount(saveCount);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.RGBA_4444;
    }

    public void setText(String text) {
        this.text = text;
        isDrawText = null != text;
    }
}
