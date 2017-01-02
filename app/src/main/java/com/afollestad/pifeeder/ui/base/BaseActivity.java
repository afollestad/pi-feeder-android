package com.afollestad.pifeeder.ui.base;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.afollestad.assent.AssentActivity;
import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.RequestBuilder;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.pifeeder.R;
import com.orhanobut.hawk.Hawk;

import static com.afollestad.pifeeder.util.Constants.KEY_TARGET_IP;
import static com.afollestad.pifeeder.util.Constants.KEY_TOKEN;

/**
 * @author Aidan Follestad (afollestad)
 */
public class BaseActivity extends AssentActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bridge.config().defaultHeader("User-Agent", "Pi Feeder Android");
        if (Hawk.contains(KEY_TARGET_IP)) {
            String ip = Hawk.get(KEY_TARGET_IP);
            Bridge.config().host("http://" + ip);
        }
        if (Hawk.contains(KEY_TOKEN)) {
            Bridge.config()
                    .defaultHeader("token", Hawk.get(KEY_TOKEN));
        }
    }

    protected void handleError(Throwable t) {
        t.printStackTrace();
        new MaterialDialog.Builder(this)
                .title(R.string.error)
                .content(t.getMessage())
                .positiveText(android.R.string.ok)
                .show();
    }

    protected RequestBuilder get(String url, Object... args) {
        return Bridge.get(url, args)
                .throwIfNotSuccess()
                .tag(getClass().getName());
    }

    protected RequestBuilder post(String url, Object... args) {
        return Bridge.post(url, args)
                .throwIfNotSuccess()
                .tag(getClass().getName());
    }

    @Override protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            Bridge.cancelAll()
                    .tag(getClass().getName())
                    .commit();
        }
    }
}
