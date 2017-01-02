package com.afollestad.pifeeder.util;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ViewUtil {

    private ViewUtil() {
    }

    public static void show(@NonNull View... views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }

    public static void hide(@NonNull View... views) {
        for (View v : views) {
            v.setVisibility(View.GONE);
        }
    }

    public static void showFirstHideOthers(@NonNull View view, @NonNull View... views) {
        view.setVisibility(View.VISIBLE);
        for (View v : views) {
            v.setVisibility(View.GONE);
        }
    }

    public static void hideFirstShowOthers(@NonNull View view, @NonNull View... views) {
        view.setVisibility(View.GONE);
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }

    public interface LayoutCallback {

        void onLayout(View view);
    }

    public static void waitForLayout(@NonNull View view, @NonNull LayoutCallback callback) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                callback.onLayout(view);
            }
        });
    }
}
