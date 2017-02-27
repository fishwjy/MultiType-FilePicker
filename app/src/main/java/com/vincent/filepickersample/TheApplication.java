package com.vincent.filepickersample;

import android.app.Application;

/**
 * Created by Vincent Woo
 * Date: 2017/2/27
 * Time: 14:46
 */

public class TheApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new LocalFileUncaughtExceptionHandler(this,
                Thread.getDefaultUncaughtExceptionHandler()));
    }
}
