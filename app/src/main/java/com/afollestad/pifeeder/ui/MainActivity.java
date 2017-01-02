package com.afollestad.pifeeder.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.assent.Assent;
import com.afollestad.pifeeder.R;
import com.afollestad.pifeeder.ui.base.BaseActivity;
import com.afollestad.pifeeder.util.AppUtils;
import com.orhanobut.hawk.Hawk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.afollestad.pifeeder.util.Constants.KEY_TARGET_IP;
import static com.afollestad.pifeeder.util.Constants.KEY_TOKEN;

public class MainActivity extends BaseActivity {

    @BindView(R.id.button_sms_notifications) Button smsNotificationsBtn;
    @BindView(R.id.button_activate_now) Button activateNowBtn;

    Unbinder unbinder;
    boolean smsOn;

    private boolean verifyWifi() {
        if (!AppUtils.isWifiConnected(this)) {
            ErrorActivity.showForNoWifi(this);
            finish();
            return true;
        }
        return false;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (verifyWifi()) return;

        if (!Hawk.contains(KEY_TOKEN)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        unbinder = ButterKnife.bind(this);
    }

    @Override protected void handleError(Throwable t) {
        if (t.getCause() instanceof ConnectException &&
                AppUtils.isWifiConnected(MainActivity.this)) {
            Toast.makeText(MainActivity.this, R.string.please_login_again, Toast.LENGTH_SHORT).show();
            didClickLogout();
            return;
        }
        super.handleError(t);
    }

    private void refreshMobileConfig(boolean verifiedPermission) {
        if (verifyWifi()) return;
        if (!Assent.isPermissionGranted(Assent.READ_PHONE_STATE)) {
            if (verifiedPermission) {
                handleError(new Exception("Access to your phone state is required in order to get your phone number!"));
                return;
            }
            Assent.requestPermissions(result -> refreshMobileConfig(true), 6969, Assent.READ_PHONE_STATE);
            return;
        }

        smsOn = false;
        smsNotificationsBtn.setEnabled(false);
        smsNotificationsBtn.setText(R.string.loading_config);

        get("/mobile_config").asJsonObject((response, object, e) -> {
            if (e != null) {
                handleError(e);
                smsNotificationsBtn.setText(R.string.load_config_error_retry);
                smsNotificationsBtn.setEnabled(true);
                return;
            }

            //noinspection MissingPermission
            final String phoneNumber = AppUtils.getPhoneNumber(this);
            final JSONArray phonesAry = object.optJSONArray("phones");

            if (phonesAry != null) {
                for (int i = 0; i < phonesAry.length(); i++) {
                    if (phonesAry.optString(i).equals(phoneNumber)) {
                        smsOn = true;
                        break;
                    }
                }
                smsNotificationsBtn.setEnabled(true);
                smsNotificationsBtn.setText(smsOn ?
                        R.string.turn_off_sms_notifications : R.string.turn_on_sms_notifications);
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        refreshMobileConfig(false);
    }

    @Override protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            unbinder.unbind();
            unbinder = null;
        }
    }

    @OnClick(R.id.button_activate_now) public void didClickActivateNow() {
        activateNowBtn.setEnabled(false);
        post("/activate").asJsonObject((response, object, e) -> {
            if (e != null) {
                handleError(e);
                activateNowBtn.setEnabled(true);
                return;
            }
            long duration;
            try {
                duration = object.getLong("duration");
            } catch (JSONException e2) {
                handleError(e2);
                return;
            }
            new Handler().postDelayed(() ->
                    activateNowBtn.setEnabled(true), duration * 1000);
        });
    }

    @OnClick(R.id.button_sms_notifications) public void didClickSmsNotifications() {
        if (!Assent.isPermissionGranted(Assent.READ_PHONE_STATE)) {
            handleError(new Exception("Access to your phone state is required in order to get your phone number!"));
            return;
        }

        smsNotificationsBtn.setEnabled(false);
        JSONObject json = new JSONObject();
        try {
            //noinspection MissingPermission
            json.put("phone", AppUtils.getPhoneNumber(this));
            json.put("remove", smsOn);
        } catch (JSONException e) {
            handleError(e);
            return;
        }

        post("/mobile_config").body(json).asJsonObject((response, object, e) -> {
            smsNotificationsBtn.setEnabled(true);
            if (e != null) {
                handleError(e);
                return;
            }
            smsOn = !smsOn;
            smsNotificationsBtn.setText(smsOn ?
                    R.string.turn_off_sms_notifications : R.string.turn_on_sms_notifications);
        });
    }

    @OnClick(R.id.button_dashboard) public void didClickDashboard() {
        startActivity(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("http://" + Hawk.get(KEY_TARGET_IP))));
    }

    @OnClick(R.id.button_logout) public void didClickLogout() {
        Hawk.deleteAll();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
