package com.erlei.tools.fpsmonitor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@SuppressLint("ViewConstructor")
class FPSView extends View {

    private final FPSConfig mFpsConfig;
    private final WindowManager windowManager;
    private final GestureDetector mGestureDetector;
    private WindowManager.LayoutParams mParams;
    private final LinkedList<FrameInfo> mFrames = new LinkedList<>();
    private Paint mPaint;
    private Paint mTextPaint;
    private Paint mLevelPaint;
    private final float right = dip2px(30);

    public FPSView(Context mContext, FPSConfig mFpsConfig) {
        super(mContext);
        this.mFpsConfig = mFpsConfig;
        windowManager = (WindowManager) getContext().getSystemService(Service.WINDOW_SERVICE);
        GestureDetector.OnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                FPSMonitor.getInstance(getContext()).destroy();
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                FPSMonitor.getInstance(getContext()).clear();
                return true;
            }
        };
        mGestureDetector = new GestureDetector(getContext(), onGestureListener);
        setHapticFeedbackEnabled(false);
        addToWindow();
        initPaint();
    }

    private void initPaint() {
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mFpsConfig.textSize);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(mFpsConfig.lineWidth);
        mLevelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLevelPaint.setStrokeWidth(dip2px(0.5F));
        mLevelPaint.setPathEffect(new DashPathEffect(new float[]{4, 4}, 0));
    }

    private void addToWindow() {
        mParams = new WindowManager.LayoutParams(
                mFpsConfig.rect.width(),
                mFpsConfig.rect.height(),
                mFpsConfig.rect.left,
                mFpsConfig.rect.top,
                getWindowFlag(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(this, mParams);
    }

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = mParams.x;
                initialY = mParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                mParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                mParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                windowManager.updateViewLayout(this, mParams);
                break;
        }
        return mGestureDetector.onTouchEvent(event);
    }

    public void setFrameInfo(List<FrameInfo> info) {
        mFrames.clear();
        mFrames.addAll(info);
        invalidate();
    }


    public void addFrameInfo(FrameInfo frameInfo) {
        if (ifSkip(frameInfo)) {
            return;
        }

        if (ifOverWidth()) {
            mFrames.removeFirst();
        }
        mFrames.addLast(frameInfo);
        invalidate();
    }

    private int sameCount = 0;

    private boolean ifSkip(FrameInfo frameInfo) {
        if (mFpsConfig.skipSameFpsCount > 0) {
            if (Math.abs(Math.round(mFpsConfig.refreshRate) - frameInfo.fps) <= mFpsConfig.refreshRate * 0.02) {
                sameCount++;
            } else {
                sameCount = 0;
            }
            return sameCount >= mFpsConfig.skipSameFpsCount;
        } else {
            return false;
        }
    }

    private boolean ifOverWidth() {
        return mFrames.size() * (mFpsConfig.lineWidth + mFpsConfig.lineSpacing) >= (mFpsConfig.rect.width() - right);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDeviceRefreshRatio(canvas);
        drawCurrentFPS(canvas);
        drawHorizontalLines(canvas);
        drawFrameLines(canvas);
    }

    private void drawFrameLines(Canvas canvas) {
        Iterator<FrameInfo> iterator = mFrames.iterator();
        int index = 0;
        int measuredHeight = getMeasuredHeight();
        while (iterator.hasNext()) {
            FrameInfo info = iterator.next();
            mPaint.setColor(info.color);
            float x = (mFpsConfig.lineWidth + mFpsConfig.lineSpacing) * index;
            float y = (measuredHeight * (info.percent));
            canvas.drawLine(x, y, x, measuredHeight, mPaint);
            index++;
        }
    }

    /**
     * 绘制当前fps
     */
    private void drawCurrentFPS(Canvas canvas) {

    }

    /**
     * 绘制设备刷新率
     */
    private void drawDeviceRefreshRatio(Canvas canvas) {
    }

    private final Rect mTextBounds = new Rect();

    /**
     * 绘制三根横线
     */
    private void drawHorizontalLines(Canvas canvas) {
        Float[] levelLines = mFpsConfig.levelLines;
        ColorEvaluator colorEvaluator = mFpsConfig.colorEvaluator;
        int measuredWidth = (int) (getMeasuredWidth() - right);
        for (Float levelLine : levelLines) {
            int color = colorEvaluator.evaluate(levelLine);
            float y = mFpsConfig.lineHeight * (1 - levelLine);
            mLevelPaint.setColor(color);
            mTextPaint.setColor(color);

            String hz = String.format(Locale.CHINA, "%shz", Math.round(mFpsConfig.refreshRate * levelLine));
            mTextPaint.getTextBounds(hz, 0, hz.length(), mTextBounds);
            float ty = y + mTextBounds.height() / 2F;
            float tx = getMeasuredWidth() - mTextBounds.width();
            if (ty < mTextBounds.height()) {
                ty = mTextBounds.height();
            }
            if (ty > getMeasuredHeight()) {
                ty = getMeasuredHeight();
            }
            canvas.drawText(hz, tx, ty, mTextPaint);
            canvas.drawLine(0F, y, measuredWidth, y, mLevelPaint);
        }
    }

    private float dip2px(float num) {
        return num * getContext().getResources().getDisplayMetrics().density + 0.5f;
    }

    public void show() {
        setAlpha(0f);
        setVisibility(View.VISIBLE);
        animate().alpha(1f).setDuration(mFpsConfig.showAnimateDuration).setListener(null);
    }

    public void destroy() {
        hide(true);
    }

    public void hide(final boolean remove) {
        animate().alpha(0f)
                .setDuration(mFpsConfig.hideAnimateDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setVisibility(GONE);
                        if (remove) {
                            windowManager.removeView(FPSView.this);
                        }
                    }
                });
    }

    @SuppressWarnings("deprecation")
    private int getWindowFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

    public static class FrameInfo {

        protected final int color;
        protected final long fps;
        protected final int dropped;
        protected final int frameCount;
        protected final float percent;

        public FrameInfo(int color, long fps, int frameCount,int dropped, float percent) {
            this.color = color;
            this.fps = fps;
            this.dropped = dropped;
            this.percent = percent;
            this.frameCount = frameCount;
        }

        @Override
        public String toString() {
            return "FrameInfo{" + "color=" + color +
                    ", fps=" + fps +
                    ", dropped=" + dropped +
                    ", frameCount=" + frameCount +
                    '}';
        }
    }
}
