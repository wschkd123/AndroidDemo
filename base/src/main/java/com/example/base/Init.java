package com.example.base;

import android.app.Application;
import android.content.Context;

/**
 * @author zhanglulu
 * @date : 2020/12/14 4:58 PM
 */
public class Init {

    public static Application application;
    public static Context applicationContext;

    public static void setApplication(Application app) {
        application = app;
        applicationContext = app.getApplicationContext();
    }

    public static Context getApplicationContext() {
        return applicationContext;
    }
}
