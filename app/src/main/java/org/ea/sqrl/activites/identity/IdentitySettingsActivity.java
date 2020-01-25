package org.ea.sqrl.activites.identity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import org.ea.sqrl.utils.IdentitySelector;
import org.ea.sqrl.utils.SqrlApplication;
import org.ea.sqrl.utils.Utils;

/**
 *
 * @author Daniel Persson
 */
public class IdentitySettingsActivity extends BaseActivity implements TextWatcher {
    private static final String TAG = "IdentitySettingsActivity";

    private PopupWindow savePopupWindow;
    private IdentitySelector mIdentitySelector = null;

    private EditText txtQuickPassLength;
    private EditText txtPwdVerifySecs;
    private EditText txtQuickPassTimeout;
    private CheckBox cbSQRLOnly;
    private CheckBox cbNoBypass;
    private Button btnSettingsSave;

    private boolean mInputFieldsValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_settings);

        setupSavePopupWindow(getLayoutInflater());
        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        txtQuickPassLength = findViewById(R.id.txtSettingsQuickPassLength);
        txtPwdVerifySecs = findViewById(R.id.txtSettingsPasswordVerifyInSeconds);
        txtQuickPassTimeout = findViewById(R.id.txtSettingsQuickPassTimeout);
        cbSQRLOnly = findViewById(R.id.cbSettingsSQRLOnly);
        cbNoBypass = findViewById(R.id.cbSettingsNoBypass);

        txtQuickPassLength.addTextChangedListener(this);
        txtPwdVerifySecs.addTextChangedListener(this);
        txtQuickPassTimeout.addTextChangedListener(this);

        final Button btnSettingsCancel = findViewById(R.id.btnSettingsCancel);
        btnSettingsCancel.setOnClickListener(v -> IdentitySettingsActivity.this.finish());

        btnSettingsSave = findViewById(R.id.btnSettingsSave);
        btnSettingsSave.setOnClickListener(v ->
            savePopupWindow.showAtLocation(savePopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );
        btnSettingsSave.setEnabled(false);

        mIdentitySelector = new IdentitySelector(this, true, false, true);
        mIdentitySelector.registerLayout(findViewById(R.id.identitySelector));
        mIdentitySelector.setIdentityChangedListener((identityIndex, identityName) -> update() );

        findViewById(R.id.imgSettingsPasswordVerifyInSecondsInfo).setOnClickListener(view ->
                showInfoMessage(R.string.settings_password_verify, R.string.helptext_password_verify_seconds)
        );

        findViewById(R.id.imgSettingsQuickPassLengthInfo).setOnClickListener(view ->
                showInfoMessage(R.string.settings_hint_length, R.string.helptext_quickpass_length)
        );

        findViewById(R.id.imgSettingsQuickPassTimeoutInfo).setOnClickListener(view ->
                showInfoMessage(R.string.settings_idle_timeout, R.string.helptext_quickpass_timeout)
        );

        findViewById(R.id.imgSettingsSQRLOnlyInfo).setOnClickListener(view ->
                showInfoMessage(R.string.settings_sqrl_only, R.string.helptext_request_sqrl_only_login)
        );

        findViewById(R.id.imgSettingsNoBypassInfo).setOnClickListener(view ->
                showInfoMessage(R.string.settings_no_sqrl_bypass, R.string.helptext_request_no_sqrl_bypass)
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        mIdentitySelector.update();
        update();
        validateInputFields();
    }

    @Override
    public void afterTextChanged(Editable s) {
        validateInputFields();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {  /* Don't care */ }
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {  /* Don't care */ }

    /**
     * Validates the contents of all user controlled input fields, sets error messages on the input
     * fields if necessary and controls whether the "save" button gets enabled or disabled accordingly.
     */
    private void validateInputFields() {
        mInputFieldsValid = true;

        String text = txtQuickPassLength.getText().toString();
        txtQuickPassLength.setError(null);

        if (text.equals("")) {
            setInputFieldError(txtQuickPassLength, R.string.error_field_may_not_be_empty);
        } else if (!Utils.isNumeric(text)) {
            setInputFieldError(txtQuickPassLength, R.string.settings_hint_length_not_number);
        } else if (getIntValue(txtQuickPassLength) > 255) {
            setInputFieldError(txtQuickPassLength, R.string.error_value_too_large_0_to_255);
        }

        text = txtPwdVerifySecs.getText().toString();
        txtPwdVerifySecs.setError(null);

        if (text.equals("")) {
            setInputFieldError(txtPwdVerifySecs, R.string.error_field_may_not_be_empty);
        } else if (!Utils.isNumeric(text)) {
            setInputFieldError(txtPwdVerifySecs, R.string.settings_password_verify_not_number);
        } else if (getIntValue(txtPwdVerifySecs) > 255) {
            setInputFieldError(txtPwdVerifySecs, R.string.error_value_too_large_0_to_255);
        }

        text = txtQuickPassTimeout.getText().toString();
        txtQuickPassTimeout.setError(null);

        if (text.equals("")) {
            setInputFieldError(txtQuickPassTimeout, R.string.error_field_may_not_be_empty);
        } else if (!Utils.isNumeric(text)) {
            setInputFieldError(txtQuickPassTimeout, R.string.settings_idle_timeout_not_number);
        } else if (getIntValue(txtQuickPassTimeout) > 65535) {
            setInputFieldError(txtQuickPassTimeout, R.string.error_value_too_large_0_to_65535);
        }

        btnSettingsSave.setEnabled(mInputFieldsValid);
    }

    private void setInputFieldError(EditText textField, int errorStringId) {
        textField.setError(getResources().getString(errorStringId));
        mInputFieldsValid = false;
    }

    private void update() {
        final SQRLStorage storage = SQRLStorage.getInstance(IdentitySettingsActivity.this.getApplicationContext());

        txtQuickPassLength.setText(Integer.toString(storage.getHintLength()));
        txtPwdVerifySecs.setText(Integer.toString(storage.getPasswordVerify()));
        txtQuickPassTimeout.setText(Integer.toString(storage.getIdleTimeout()));
        cbSQRLOnly.setChecked(storage.isSQRLOnly());
        cbNoBypass.setChecked(storage.isNoByPass());
    }

    public int getIntValue(EditText txt) {
        try {
            return Integer.parseInt(txt.getText().toString());
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    public void setupSavePopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_save_settings, null);

        savePopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        savePopupWindow.setTouchable(true);
        savePopupWindow.setFocusable(true);

        final EditText txtPassword = popupView.findViewById(R.id.txtPassword);

        SQRLStorage storage = SQRLStorage.getInstance(IdentitySettingsActivity.this.getApplicationContext());

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

            int quickPassLength = getIntValue(txtQuickPassLength);
            int passwordVerify = getIntValue(txtPwdVerifySecs);
            int quickPassTimeout = getIntValue(txtQuickPassTimeout);

            storage.setHintLength(quickPassLength);
            storage.setPasswordVerify(passwordVerify);
            storage.setIdleTimeout(quickPassTimeout);
            storage.setSQRLOnly(cbSQRLOnly.isChecked());
            storage.setNoByPass(cbNoBypass.isChecked());

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
                IdentitySettingsActivity.this.finish();
            });

        }).start());
    }
}
