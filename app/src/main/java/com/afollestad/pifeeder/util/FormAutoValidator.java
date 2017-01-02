package com.afollestad.pifeeder.util;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;


/**
 * @author Aidan Follestad (afollestad)
 */
public class FormAutoValidator implements TextWatcher {

    private final static long DELAY_MS = 150;

    private Button button;
    private EditText[] editTexts;
    private final Handler handler;
    private boolean lastIsValid = false;

    private final Runnable validateRunnable = () -> {
        for (EditText editText : editTexts) {
            if (!validate(editText)) {
                if (lastIsValid) {
                    lastIsValid = false;
                    onValidityChange(false);
                }
                return;
            }
        }
        if (!lastIsValid) {
            lastIsValid = true;
            onValidityChange(true);
        }
    };

    public FormAutoValidator(Button button, EditText... editTexts) {
        this.button = button;
        this.editTexts = editTexts;
        this.handler = new Handler();

        for (EditText editText : editTexts) {
            editText.addTextChangedListener(this);
        }
    }

    public boolean lastValidityState() {
        return lastIsValid;
    }

    private boolean validate(EditText editText) {
        return !editText.getText().toString().trim().isEmpty();
    }

    @Override
    public final void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override public final void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        this.handler.removeCallbacks(validateRunnable);
        this.handler.postDelayed(validateRunnable, DELAY_MS);
    }

    @Override public final void afterTextChanged(Editable editable) {
    }

    public void onValidityChange(boolean valid) {
        button.setEnabled(valid);
    }

    public void destroy() {
        for (EditText editText : editTexts) {
            editText.removeTextChangedListener(this);
        }
        editTexts = null;
    }
}
