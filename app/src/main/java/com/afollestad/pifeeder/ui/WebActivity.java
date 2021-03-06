package com.afollestad.pifeeder.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.pifeeder.R;
import com.afollestad.pifeeder.ui.base.BaseActivity;
import com.orhanobut.hawk.Hawk;

import static com.afollestad.pifeeder.util.Constants.KEY_TOKEN;

/**
 * @author Aidan Follestad (afollestad)
 */
public class WebActivity extends BaseActivity {

    public static void start(Activity context, String url) {
        context.startActivity(new Intent(context, WebActivity.class)
                .putExtra("url", url));
    }

    ProgressBar progress;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        String url = getIntent().getStringExtra("url");
        if (!url.startsWith("http"))
            url = "http://" + url;
        url += "?token=" + Hawk.get(KEY_TOKEN);

        WebView webView = (WebView) findViewById(R.id.webView);
        progress = (ProgressBar) findViewById(R.id.progress);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("SkillFitness-Android");
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("about:exit")) {
                    finish();
                    return true;
                }
                progress.setVisibility(View.VISIBLE);
                view.clearView();
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                handleError(new Exception(description));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new MaterialDialog.Builder(WebActivity.this)
                        .content(message)
                        .positiveText(android.R.string.ok)
                        .show();
                return true;
            }
        });
        webView.loadUrl(url);
    }
}