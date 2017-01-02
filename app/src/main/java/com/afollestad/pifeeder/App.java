package com.afollestad.pifeeder;

import android.app.Application;

import com.afollestad.bridge.Bridge;
import com.afollestad.pifeeder.api.ApiValidator;
import com.orhanobut.hawk.Hawk;

/**
 * @author Aidan Follestad (afollestad)
 */
public class App extends Application {

    @Override public void onCreate() {
        super.onCreate();
        Hawk.init(this).build();
        Bridge.config()
                .validators(new ApiValidator());
    }

    @Override public void onTerminate() {
        super.onTerminate();
        Hawk.destroy();
        Bridge.destroy();
    }
}
