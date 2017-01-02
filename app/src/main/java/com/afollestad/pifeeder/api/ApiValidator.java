package com.afollestad.pifeeder.api;

import android.support.annotation.NonNull;

import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseValidator;

import org.json.JSONObject;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ApiValidator extends ResponseValidator {

    @Override public boolean validate(@NonNull Response response) throws Exception {
        if (response.contentType() != null && !response.contentType().startsWith("application/json")) {
            return true;
        }
        JSONObject json = response.asJsonObject();
        if (json != null && json.getString("status").equals("error")) {
            throw new Exception(json.getString("error"));
        }
        return true;
    }

    @NonNull @Override public String id() {
        return "api-validator";
    }
}
