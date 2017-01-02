package com.afollestad.pifeeder.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.bridge.Bridge;
import com.afollestad.pifeeder.R;
import com.afollestad.pifeeder.ui.base.BaseActivity;
import com.afollestad.pifeeder.util.AppUtils;
import com.afollestad.pifeeder.util.ViewUtil;
import com.orhanobut.hawk.Hawk;

import java.net.ConnectException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ErrorActivity extends BaseActivity {

    private static final String EXTRA_MODE = "mode";
    private static final String EXTRA_SOURCE = "source";

    public static final String MODE_NO_WIFI = "no_wifi";

    private Unbinder unbinder;
    private Runnable retryRunnable;
    private BroadcastReceiver wifiListener;

    @BindView(R.id.message) TextView message;
    @BindView(R.id.status_progress) ProgressBar statusProgress;
    @BindView(R.id.status_text) TextView statusText;
    @BindView(R.id.status_frame) View statusFrame;
    @BindView(R.id.retry_button) Button retryBtn;
    @BindView(R.id.logout_button) Button logoutBtn;

    public static void showForNoWifi(@NonNull Activity context) {
        context.startActivity(new Intent(context, ErrorActivity.class)
                .putExtra(EXTRA_MODE, MODE_NO_WIFI)
                .putExtra(EXTRA_SOURCE, context.getClass().getName()));
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);
        unbinder = ButterKnife.bind(this);

        final String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (MODE_NO_WIFI.equals(mode)) {
            setupNoWifi();
        } else {
            throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (wifiListener != null) {
            registerReceiver(wifiListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override protected void onPause() {
        super.onPause();
        unregisterWifiListener();
        if (isFinishing()) {
            retryRunnable = null;
            unbinder.unbind();
            unbinder = null;
        }
    }

    private void unregisterWifiListener() {
        if (wifiListener != null) {
            try {
                unregisterReceiver(wifiListener);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            wifiListener = null;
        }
    }

    private void exitToSource() {
        String source = getIntent().getStringExtra(EXTRA_SOURCE);
        if (source == null) {
            source = LoginActivity.class.getName();
        }
        try {
            startActivity(new Intent(this, Class.forName(source)));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        finish();
    }

    private void verifyConnection() {
        if (LoginActivity.class.getName().equals(getIntent().getStringExtra(EXTRA_SOURCE))) {
            exitToSource();
            return;
        }

        message.setText(R.string.verifying_connection);
        statusText.setText(R.string.contacting_feeder);
        ViewUtil.hide(retryBtn, logoutBtn);
        ViewUtil.show(statusFrame, retryBtn, statusProgress);
        retryBtn.setEnabled(false);
        retryRunnable = this::verifyConnection;

        get("/mobile_config").asJsonObject((response, object, e) -> {
            if (e != null) {
                ViewUtil.show(logoutBtn);
                message.setText(R.string.failed_verify);
                if (e.getCause() instanceof ConnectException) {
                    statusText.setText(R.string.verified_connection_refused);
                    ViewUtil.showFirstHideOthers(statusFrame, statusProgress);
                } else {
                    ViewUtil.showFirstHideOthers(logoutBtn, statusFrame);
                }
                retryBtn.setEnabled(true);
                return;
            }
            exitToSource();
        });
    }

    private void setupNoWifi() {
        message.setText(R.string.no_wifi_connection);
        statusText.setText(R.string.waiting_for_wifi);
        ViewUtil.hideFirstShowOthers(retryBtn, statusFrame, statusProgress);

        wifiListener = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (AppUtils.isWifiConnected(context)) {
                    verifyConnection();
                    unregisterWifiListener();
                }
            }
        };
    }

    @OnClick(R.id.retry_button) public void didClickRetry() {
        retryBtn.setEnabled(false);
        if (retryRunnable != null) {
            retryRunnable.run();
        }
    }

    @OnClick(R.id.logout_button) public void didClickLogout() {
        Hawk.deleteAll();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
