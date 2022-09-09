package com.erlei.tools.fpsmonitor;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 作者:  lll
 * 日期： 2016/12/17
 * 应用前后台变化监听器
 */
class LifecycleChangeListener implements Application.ActivityLifecycleCallbacks {

    public static final long CHECK_DELAY = 500;

    public interface Listener {

        void onBecameForeground();

        void onBecameBackground();

    }

    private static LifecycleChangeListener instance;

    private boolean foreground = false, paused = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private Runnable check;

    /**
     * Its not strictly necessary to use this method - _usually_ invoking
     * getDefault with a Context gives us a path to retrieve the Application and
     * initialise, but sometimes (e.g. in test harness) the ApplicationContext
     * is != the Application, and the docs make no guarantees.
     *
     * @param application  application
     */
    public static void init(Application application) {
        if (instance == null) {
            instance = new LifecycleChangeListener();
            application.registerActivityLifecycleCallbacks(instance);
        }
    }

    public static LifecycleChangeListener get(Application application) {
        if (instance == null) {
            init(application);
        }
        return instance;
    }

    public static LifecycleChangeListener get(Context ctx) {
        if (instance == null) {
            Context appCtx = ctx.getApplicationContext();
            if (appCtx instanceof Application) {
                init((Application) appCtx);
            }else {
                throw new IllegalStateException(
                        "Foreground is not initialised and " +
                                "cannot obtain the Application obj");
            }
        }
        return instance;
    }

    public static LifecycleChangeListener get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "Foreground is not initialised - invoke " +
                            "at least once with parameterised init/getDefault");
        }
        return instance;
    }

    public boolean isForeground() {
        return foreground;
    }

    public boolean isBackground() {
        return !foreground;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        paused = false;
        boolean wasBackground = !foreground;
        foreground = true;
        if (check != null)
            handler.removeCallbacks(check);

        if (wasBackground) {
            for (Listener l : listeners) {
                try {
                    l.onBecameForeground();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused = true;

        if (check != null)
            handler.removeCallbacks(check);

        handler.postDelayed(check = () -> {
            if (foreground && paused) {
                foreground = false;
                for (Listener l : listeners) {
                    try {
                        l.onBecameBackground();
                    } catch (Exception ignored) {
                    }
                }
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}

