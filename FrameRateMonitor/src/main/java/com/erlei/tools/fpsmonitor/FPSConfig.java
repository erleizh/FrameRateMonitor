package com.erlei.tools.fpsmonitor;

import android.graphics.Color;
import android.graphics.Rect;

import java.util.concurrent.TimeUnit;

class FPSConfig {
    public static ColorEvaluator sColorEvaluator = fraction -> {
        int color = Color.TRANSPARENT;
        if (fraction >= 0.85) {
            color = Color.GREEN;
        } else if (fraction > 0.6 && fraction <= 0.85) {
            color = Color.YELLOW;
        } else if (fraction <= 0.6) {
            color = Color.RED;
        } else if (fraction == 0) {
            color = Color.BLACK;
        }
        return color;
    };
    public float refreshRateInMs;
    public float refreshRate;
    public float textSize = 20;
    public long showAnimateDuration = 200;
    public long hideAnimateDuration = 200;
    public long sampleTimeInNs = TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS);
    public ColorEvaluator colorEvaluator = sColorEvaluator;
    public float lineWidth = 2;
    public float lineHeight = 0;
    public float lineSpacing = 2;
    public Rect rect;
    public Float[] levelLines = new Float[]{1F, 0.85F, 0.7F, 0.4F, 0.2F};
    /**
     * 当连续N个fps等于设备最大fps时,跳过绘制
     */
    public int skipSameFpsCount = 2;
}
