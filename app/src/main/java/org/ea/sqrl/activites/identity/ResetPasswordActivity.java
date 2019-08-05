package org.ea.sqrl.activites.identity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.MainActivity;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.PasswordStrengthMeter;
import org.ea.sqrl.utils.RescueCodeInputHelper;
import org.ea.sqrl.utils.SqrlApplication;

public class ResetPasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        final boolean newIdentity = getIntent().getBooleanExtra(SQRLStorage.NEW_IDENTITY, false);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        final EditText txtResetPasswordNewPassword = findViewById(R.id.txtResetPasswordNewPassword);
        final TextView txtResetPasswordDescription = findViewById(R.id.txtResetPasswordDescription);
        final ViewGroup pwStrengthMeter = findViewById(R.id.passwordStrengthMeter);
        final ViewGroup rootView = findViewById(R.id.resetPasswordActivityView);
        final Button btnResetPassword = findViewById(R.id.btnResetPassword);

        // activity was started from an identity import, change ui texts accordingly
        if (newIdentity) {
            this.setTitle(R.string.title_set_password);
            txtResetPasswordDescription.setText(R.string.set_password_import_desc);
            btnResetPassword.setText(R.string.button_set_password);
        }

        new PasswordStrengthMeter(this)
                .register(txtResetPasswordNewPassword, pwStrengthMeter);

        txtResetPasswordNewPassword.setOnFocusChangeListener((v, hasFocus) -> {
            txtResetPasswordDescription.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
        });

        RescueCodeInputHelper rescueCodeInputHelper = new RescueCodeInputHelper(
                this, rootView, txtResetPasswordNewPassword, false);
        rescueCodeInputHelper.setStatusChangedListener(successfullyCompleted -> {
            btnResetPassword.setEnabled(successfullyCompleted);
        });
        rescueCodeInputHelper.requestFocus();

        btnResetPassword.setEnabled(false);
        btnResetPassword.setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance(ResetPasswordActivity.this.getApplicationContext());

            showProgressPopup();

            new Thread(() -> {
                String rescueCode = rescueCodeInputHelper.getRescueCodeInput();

                boolean decryptionOk = storage.decryptUnlockKey(rescueCode);
                if (!decryptionOk) {
                    handler.post(() -> hideProgressPopup());
                    showErrorMessage(R.string.decrypt_identity_fail);
                    return;
                }

                storage.reInitializeMasterKeyIdentity();

                boolean encryptStatus = storage.encryptIdentityKey(txtResetPasswordNewPassword.getText().toString(), entropyHarvester);
                if (!encryptStatus) {
                    handler.post(() -> hideProgressPopup());
                    showErrorMessage(R.string.encrypt_identity_fail);
                    return;
                }
                storage.clear();

                storage.clear();
                handler.post(() -> {
                    hideProgressPopup();
                    txtResetPasswordNewPassword.setText("");
                    rescueCodeInputHelper.clearForm();
                });

                if(mDbHelper.hasIdentities() && !newIdentity) {
                    long currentId = SqrlApplication.getCurrentId(this.getApplication());

                    if(currentId != 0) {
                        mDbHelper.updateIdentityData(currentId, storage.createSaveData());
                    }
                    handler.post(() -> ResetPasswordActivity.this.finish());
                } else {
                    long newIdentityId = mDbHelper.newIdentity(
                            ResetPasswordActivity.this, storage.createSaveData());
                    SqrlApplication.saveCurrentId(this.getApplication(), newIdentityId);

                    if(newIdentity) {
                        handler.post(() -> {
                            ResetPasswordActivity.this.finish();
                            startActivity(new Intent(this, RenameActivity.class));
                        });
                    } else {
                        handler.post(() -> {
                            ResetPasswordActivity.this.finish();
                            startActivity(new Intent(this, MainActivity.class));
                        });
                    }
                }

            }).start();
        });
    }
}
