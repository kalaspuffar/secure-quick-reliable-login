package org.ea.sqrl.activites.identity;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.PasswordStrengthMeter;
import org.ea.sqrl.utils.SqrlApplication;

public class ChangePasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        final EditText txtCurrentPassword = findViewById(R.id.txtCurrentPassword);
        final EditText txtNewPassword = findViewById(R.id.txtNewPassword);
        final EditText txtRetypePassword = findViewById(R.id.txtRetypePassword);
        final ViewGroup pwStrengthMeter = findViewById(R.id.passwordStrengthMeter);

        new PasswordStrengthMeter(this)
                .register(txtNewPassword, pwStrengthMeter);

        SQRLStorage storage = SQRLStorage.getInstance(ChangePasswordActivity.this.getApplicationContext());

        findViewById(R.id.btnDoChangePassword).setOnClickListener(v -> {
            if(!txtNewPassword.getText().toString().equals(txtRetypePassword.getText().toString())) {
                showErrorMessage(R.string.change_password_retyped_password_do_not_match);
                txtNewPassword.setText("");
                txtRetypePassword.setText("");
                txtNewPassword.requestFocus();
                return;
            }

            showProgressPopup();

            new Thread(() -> {
                boolean decryptStatus = storage.decryptIdentityKey(txtCurrentPassword.getText().toString(), entropyHarvester, false);
                if (!decryptStatus) {
                    showErrorMessage(R.string.decrypt_identity_fail);

                    handler.post(() -> {
                        hideProgressPopup();
                        txtCurrentPassword.setText("");
                        txtCurrentPassword.requestFocus();
                    });
                    storage.clearQuickPass();
                    storage.clear();

                    return;
                }

                boolean encryptStatus = storage.encryptIdentityKey(txtNewPassword.getText().toString(), entropyHarvester);
                if (!encryptStatus) {
                    showErrorMessage(R.string.encrypt_identity_fail);

                    handler.post(() -> {
                        hideProgressPopup();
                        txtCurrentPassword.setText("");
                        txtNewPassword.setText("");
                        txtRetypePassword.setText("");
                    });
                    return;
                }

                storage.clearQuickPass();
                storage.clear();

                long currentId = SqrlApplication.getCurrentId(this.getApplication());

                mDbHelper.updateIdentityData(currentId, storage.createSaveData());

                handler.post(() -> {
                    txtCurrentPassword.setText("");
                    txtNewPassword.setText("");
                    txtRetypePassword.setText("");
                    hideProgressPopup();
                    ChangePasswordActivity.this.finish();
                });
            }).start();
        });
    }


}
