package com.afollestad.pifeeder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.bridge.Bridge;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.pifeeder.R;
import com.afollestad.pifeeder.ui.base.BaseActivity;
import com.afollestad.pifeeder.util.FormAutoValidator;
import com.afollestad.udpdiscovery.Discovery;
import com.afollestad.udpdiscovery.Entity;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.afollestad.pifeeder.util.Constants.KEY_TARGET_IP;
import static com.afollestad.pifeeder.util.Constants.KEY_TOKEN;

/**
 * @author Aidan Follestad (afollestad)
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.discovered_list) LinearLayout discoveredList;
    @BindView(R.id.input_username) EditText inputUsername;
    @BindView(R.id.input_password) EditText inputPassword;
    @BindView(R.id.button_login) Button buttonLogin;

    private Unbinder unbinder;
    private FormAutoValidator autoValidator;
    private Entity selectedEntity;

    private void addDiscoveredEntityToList(Entity entity) {
        View view = getLayoutInflater().inflate(R.layout.list_item_discoveredentity, discoveredList, false);
        ((TextView) view.findViewById(R.id.name)).setText(entity.name());
        ((TextView) view.findViewById(R.id.address)).setText(entity.address());
        view.setTag(entity);
        view.setOnClickListener(this);
        discoveredList.addView(view);
    }

    private void invalidateEntityList(@Nullable Entity entity) {
        for (int i = 0; i < discoveredList.getChildCount(); i++) {
            View view = discoveredList.getChildAt(i);
            Entity tag = (Entity) view.getTag();
            view.setActivated(false);
            if (tag == null || entity == null) continue;
            view.setActivated(tag.equals(entity));
            ((ImageView) view.findViewById(R.id.checked_state)).setImageResource(
                    tag.equals(entity) ? R.drawable.ic_radio_filled : R.drawable.ic_radio_unfilled);
        }
        buttonLogin.setEnabled(autoValidator.lastValidityState() && selectedEntity != null);
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        unbinder = ButterKnife.bind(this);
        autoValidator = new FormAutoValidator(buttonLogin, inputUsername, inputPassword) {
            @Override public void onValidityChange(boolean valid) {
                buttonLogin.setEnabled(selectedEntity != null && valid);
            }
        };
    }

    private void discover() {
        discoveredList.removeAllViews();
        Discovery.instance(this)
                .discover(this::addDiscoveredEntityToList, this::handleError);
    }

    @Override protected void onResume() {
        super.onResume();
        discover();
    }

    @Override protected void onPause() {
        super.onPause();
        Discovery.destroy();
        if (isFinishing()) {
            unbinder.unbind();
            unbinder = null;
            autoValidator.destroy();
            autoValidator = null;
        }
    }

    @OnClick(R.id.button_login) public void onLoginClick() {
        if (selectedEntity == null) {
            return;
        }
        buttonLogin.setEnabled(false);
        Bridge.config().host("http://" + selectedEntity.address());

        JSONObject json = new JSONObject();
        try {
            json.put("username", inputUsername.getText().toString().trim());
            json.put("password", inputPassword.getText().toString().trim());
        } catch (Exception e) {
            handleError(e);
            return;
        }

        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .progress(true, -1)
                .content(R.string.logging_in)
                .show();

        post("/api_login").body(json)
                .asJsonObject((response, object, e) -> {
                    dialog.dismiss();

                    if (e != null) {
                        buttonLogin.setEnabled(true);
                        handleError(e);
                        return;
                    }
                    String token;
                    try {
                        token = object.getString("token");
                    } catch (JSONException e2) {
                        handleError(e2);
                        return;
                    }

                    Hawk.put(KEY_TARGET_IP, selectedEntity.address());
                    Hawk.put(KEY_TOKEN, token);

                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }

    @Override public void onClick(View view) {
        Entity tag = (Entity) view.getTag();
        if (tag == null) {
            return;
        }
        this.selectedEntity = tag;
        invalidateEntityList(tag);
    }
}
