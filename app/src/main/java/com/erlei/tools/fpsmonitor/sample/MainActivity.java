package com.erlei.tools.fpsmonitor.sample;

import android.app.Activity;
import android.os.Bundle;

import com.erlei.tools.fpsmonitor.FPSMonitor;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FPSMonitor.getInstance(this).show();
        setContentView(R.layout.activity_main);

    }

}