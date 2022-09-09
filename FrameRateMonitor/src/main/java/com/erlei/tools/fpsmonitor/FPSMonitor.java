package com.erlei.tools.fpsmonitor;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class FPSMonitor {

    private FPSConfig mFpsConfig;
    private final Context mContext;
    private FPSView mFPSView;
    private int mTotalDropped;
    private int mTotalFrames;
    /**
     * 总次数
     *
     * @see FPSConfig#sampleTimeInNs 每一个帧率信息的时间长度,默认为一秒
     */
    private int mFPSInfoCount;
    private float mTotalFPS;
    private float mFrameRatio;
    private long mStartTimeMillis;
    @SuppressLint("StaticFieldLeak")
    private static volatile FPSMonitor sFPSMonitor = null;

    public static FPSMonitor getInstance(Context context) {
        if (null == sFPSMonitor) {
            synchronized (FPSMonitor.class) {
                if (null == sFPSMonitor) {
                    sFPSMonitor = new FPSMonitor(context.getApplicationContext());
                }
            }
        }
        return sFPSMonitor;
    }

    private final LifecycleChangeListener.Listener lifecycleChangeListener = new LifecycleChangeListener.Listener() {
        @Override
        public void onBecameForeground() {
            show();
        }

        @Override
        public void onBecameBackground() {
            hide();
        }
    };
    private FPSCounter mFpsCounter;

    protected FPSMonitor(Context applicationContext) {
        mContext = applicationContext;
        mFpsConfig = new FPSConfig();
        setDefaultConfig(mContext);
    }


    private void setDefaultConfig(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mFpsConfig.refreshRateInMs = 1000f / display.getRefreshRate();
        mFpsConfig.refreshRate = display.getRefreshRate();
        Point outSize = new Point();
        display.getSize(outSize);
        mFpsConfig.rect = new Rect(0, 0, (int) (outSize.x * 0.6F), (int) (outSize.x * 0.3F));
        mFpsConfig.lineHeight = mFpsConfig.rect.height();
    }

    public FPSMonitor setLayoutPosition(Rect rect) {
        mFpsConfig.rect = rect;
        mFpsConfig.lineHeight = mFpsConfig.rect.height();
        return this;
    }


    public FPSMonitor setColorEvaluator(ColorEvaluator evaluator) {
        mFpsConfig.colorEvaluator = evaluator;
        return this;
    }

    public boolean isRunning() {
        return mFpsCounter != null && mFpsCounter.isRunning();
    }

    /**
     * 开始监控FPS
     */
    public void start() {
        if (mFpsCounter == null) {
            mFpsCounter = new FPSCounter(mFpsConfig, longs -> {
                FPSView.FrameInfo frameInfo = buildFrameInfo(longs);
                if (mFPSView != null) {
                    mFPSView.addFrameInfo(frameInfo);
                }
                mTotalFPS += frameInfo.fps;
                mFPSInfoCount++;
                mFrameRatio = mTotalFPS / mFPSInfoCount;
                mTotalDropped += frameInfo.dropped;
                mTotalFrames += frameInfo.frameCount;
            });
        }
        mFpsCounter.start();
        mStartTimeMillis = System.currentTimeMillis();
    }

    /**
     * 停止监控FPS
     */
    public void stop() {
        if (mFpsCounter != null) {
            mFpsCounter.stop();
        }
    }

    /**
     * 开始监控FPS并显示一个可视化弹窗到Window上
     */
    public void show() {
        if (checkPermission(mContext)) {
            return;
        }
        if (mFPSView == null) {
            mFPSView = new FPSView(mContext, mFpsConfig);
        }
        mFPSView.show();
        start();
        LifecycleChangeListener.get(mContext).addListener(lifecycleChangeListener);
    }

    private FPSView.FrameInfo buildFrameInfo(List<Long> longs) {
        List<Integer> droppedSet = Calculation.getDroppedSet(mFpsConfig, longs);
        return Calculation.calculateMetric(mFpsConfig, longs, droppedSet);
    }

    /**
     * 销毁监控器
     */
    public void destroy() {
        if (mFpsCounter != null) {
            mFpsCounter.stop();
            mFpsCounter = null;
        }
        if (mFPSView != null) {
            mFPSView.destroy();
            mFPSView = null;
        }
        mFpsConfig = null;
        sFPSMonitor = null;
        LifecycleChangeListener.get(mContext).removeListener(lifecycleChangeListener);
    }

    /**
     * @return 获取所有卡顿帧的个数
     */
    public int getTotalDropped() {
        return mTotalDropped;
    }


    /**
     * @return 获取总绘制帧数
     */
    public int getTotalFrames() {
        return mTotalFrames;
    }

    /**
     * @return 获取总的帧率信息个数
     */
    public int getTotalFrameInfoCount() {
        return mFPSInfoCount;
    }

    /**
     * @return 获取平均帧率
     * 每个记录周期 / 总次数
     */
    public float getFrameRate() {
        return mFrameRatio;
    }

    /**
     * @return 获取运行时间 毫秒
     */
    public long getRunningTimeInMillis() {
        return System.currentTimeMillis() - mStartTimeMillis;
    }


    /**
     * 清除所有绘制的数据
     */
    public void clear() {
        if (mFPSView != null) {
            mFPSView.setFrameInfo(new LinkedList<>());
        }
        if (mFpsCounter != null) {
            mFpsCounter.clear();
        }
        mStartTimeMillis = 0;
        mTotalFPS = 0;
        mFPSInfoCount = 0;
        mFrameRatio = 0;
        mTotalDropped = 0;
        mTotalFrames = 0;
    }

    /**
     * 隐藏并停止监控FPS
     */
    public void hide() {
        stop();
        if (mFPSView != null) {
            mFPSView.hide(false);
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private boolean checkPermission(Context context) {
        boolean permNeeded = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                permNeeded = true;
            }
        }
        return permNeeded;
    }


}
