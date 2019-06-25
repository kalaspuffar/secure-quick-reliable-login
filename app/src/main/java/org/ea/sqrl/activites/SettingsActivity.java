package org.ea.sqrl.activites;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.SqrlApplication;

/**
 *
 * @author Daniel Persson
 */
public class SettingsActivity extends BaseActivity {
    private static final String TAG = "SettingsActivity";
    private static final int ONE_WEEK_IN_MINUTES = 60 * 24 * 7;

    private PopupWindow savePopupWindow;

    private EditText txtSettingsHintLength;
    private EditText txtSettingsPasswordVerify;
    private EditText txtSettingsIdleTimeout;
    private CheckBox cbSettingsSQRLOnly;
    private CheckBox cbSettingsNoBypass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupSavePopupWindow(getLayoutInflater());
        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        SQRLStorage storage = SQRLStorage.getInstance(SettingsActivity.this.getApplicationContext());

        txtSettingsHintLength = findViewById(R.id.txtSettingsHintLength);
        txtSettingsHintLength.setText(Integer.toString(storage.getHintLength()));
        txtSettingsPasswordVerify = findViewById(R.id.txtSettingsPasswordVerifyInSeconds);
        txtSettingsPasswordVerify.setText(Integer.toString(storage.getPasswordVerify()));
        txtSettingsIdleTimeout = findViewById(R.id.txtSettingsIdleTimeout);
        txtSettingsIdleTimeout.setText(Integer.toString(storage.getIdleTimeout()));
        cbSettingsSQRLOnly = findViewById(R.id.cbSettingsSQRLOnly);
        cbSettingsSQRLOnly.setChecked(storage.isSQRLOnly());
        cbSettingsNoBypass = findViewById(R.id.cbSettingsNoBypass);
        cbSettingsNoBypass.setChecked(storage.isNoByPass());

        final Button btnSettingsCancel = findViewById(R.id.btnSettingsCancel);
        btnSettingsCancel.setOnClickListener(v -> SettingsActivity.this.finish());

        final Button btnSettingsSave = findViewById(R.id.btnSettingsSave);
        btnSettingsSave.setOnClickListener(v ->
            savePopupWindow.showAtLocation(savePopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        txtSettingsIdleTimeout.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 2) {
                    int minutes = -1;
                    try {minutes = Integer.parseInt(editable.toString());} catch (Throwable t) {}
                    if (minutes > ONE_WEEK_IN_MINUTES) {
                        showErrorMessageInternal(getString(R.string.idle_timeout_guidance, ONE_WEEK_IN_MINUTES + ""), null, getString(R.string.guidance_heading));
                        txtSettingsIdleTimeout.setText(String.valueOf(ONE_WEEK_IN_MINUTES));
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        });

    }

    public int getIntValue(EditText txt, int errorMessage) {
        try {
            return Integer.parseInt(txt.getText().toString());
        } catch (NumberFormatException nfe) {
            showErrorMessage(errorMessage);
        }
        return -1;
    }

    public void setupSavePopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_save_settings, null);

        savePopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        savePopupWindow.setTouchable(true);
        savePopupWindow.setFocusable(true);

        final EditText txtPassword = popupView.findViewById(R.id.txtPassword);

        SQRLStorage storage = SQRLStorage.getInstance(SettingsActivity.this.getApplicationContext());

        popupView.findViewById(R.id.btnCloseSaveSettings).setOnClickListener(v -> savePopupWindow.dismiss());
        final Button btnSaveSettings = popupView.findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(v -> new Thread(() -> {
            handler.post(() -> {
                savePopupWindow.dismiss();
                showProgressPopup();
            });
            storage.clearQuickPass();
            boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString(), entropyHarvester, false);
            if(!decryptStatus) {
                showErrorMessage(R.string.decrypt_identity_fail);
                storage.clearQuickPass();
                storage.clear();
                handler.post(() -> {
                    hideProgressPopup();
                    txtPassword.setText("");
                });
                return;
            }

            int hintLength = getIntValue(txtSettingsHintLength, R.string.settings_hint_length_not_number);
            if(hintLength == -1) return;
            if(hintLength > 255) {
                showErrorMessage(R.string.settings_hint_length_to_large);
                handler.post(() -> {
                    hideProgressPopup();
                    txtPassword.setText("");
                });
                return;
            }
            int passwordVerify = getIntValue(txtSettingsPasswordVerify, R.string.settings_password_verify_not_number);
            if(passwordVerify == -1) return;
            int idleTimeout = getIntValue(txtSettingsIdleTimeout, R.string.settings_idle_timeout_not_number);
            if(idleTimeout == -1) return;

            storage.setHintLength(hintLength);
            storage.setPasswordVerify(passwordVerify);
            storage.setIdleTimeout(idleTimeout);
            storage.setSQRLOnly(cbSettingsSQRLOnly.isChecked());
            storage.setNoByPass(cbSettingsNoBypass.isChecked());

            boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
            if (!encryptStatus) {
                showErrorMessage(R.string.encrypt_identity_fail);
                handler.post(() -> {
                    hideProgressPopup();
                    txtPassword.setText("");
                });
                return;
            }
            storage.clear();
            storage.clearQuickPass();

            long currentId = SqrlApplication.getCurrentId(this.getApplication());
            mDbHelper.updateIdentityData(currentId, storage.createSaveData());

            handler.post(() -> {
                txtPassword.setText("");
                hideProgressPopup();
                SettingsActivity.this.finish();
            });

        }).start());
    }
}
