package com.erlei.tools.fpsmonitor;

import android.view.Choreographer;

import java.util.ArrayList;
import java.util.List;

class FPSCounter implements Choreographer.FrameCallback {

    private static final String TAG = "FPSCounter";
    private final long sampleTimeInNs;
    private final Consumer<List<Long>> mConsumer;
    private boolean running = false;
    private long startSampleTimeInNs = 0;
    private ArrayList<Long> dataSet;

    public boolean isRunning() {
        return running;
    }

    public void start() {
        running = true;
        Choreographer.getInstance().postFrameCallback(this);
    }


    public void stop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    public FPSCounter(FPSConfig mFpsConfig, Consumer<List<Long>> consumer) {
        sampleTimeInNs = mFpsConfig.sampleTimeInNs;
        mConsumer = consumer;
        dataSet = new ArrayList<>();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            clear();
            return;
        }
        //initial case
        if (startSampleTimeInNs == 0) {
            startSampleTimeInNs = frameTimeNanos;
        }
        if (frameTimeNanos - startSampleTimeInNs > sampleTimeInNs) {
            collectSampleAndSend(frameTimeNanos);
        }
        dataSet.add(frameTimeNanos);
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void collectSampleAndSend(long frameTimeNanos) {
        //this occurs only when we have gathered over the sample time ~700ms
        @SuppressWarnings("unchecked")
        ArrayList<Long> clone = (ArrayList<Long>) dataSet.clone();
        if (mConsumer != null && !clone.isEmpty()) {
            mConsumer.accept(clone);
        }
        // clear data
        dataSet.clear();
        //reset sample timer to last frame
        startSampleTimeInNs = frameTimeNanos;
    }

    public void clear() {
        dataSet.clear();
    }

    public interface Consumer<T> {

        void accept(T t);

    }

}
