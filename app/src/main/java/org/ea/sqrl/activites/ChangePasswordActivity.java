package org.ea.sqrl.activites;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

public class ChangePasswordActivity extends BaseActivity {
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());
        final EditText txtCurrentPassword = findViewById(R.id.txtCurrentPassword);
        final EditText txtNewPassword = findViewById(R.id.txtNewPassword);
        final EditText txtRetypePassword = findViewById(R.id.txtRetypePassword);

        SQRLStorage storage = SQRLStorage.getInstance();

        findViewById(R.id.btnDoChangePassword).setOnClickListener(v -> {
            if(!txtNewPassword.getText().toString().equals(txtRetypePassword.getText().toString())) {
                showErrorMessage(R.string.change_password_retyped_password_do_not_match);
                txtCurrentPassword.setText("");
                txtNewPassword.setText("");
                txtRetypePassword.setText("");
                return;
            }

            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                boolean decryptStatus = storage.decryptIdentityKey(txtCurrentPassword.getText().toString(), entropyHarvester, false);
                if (!decryptStatus) {
                    showErrorMessage(R.string.decrypt_identity_fail);

                    handler.post(() -> {
                        progressPopupWindow.dismiss();
                        txtCurrentPassword.setText("");
                        txtNewPassword.setText("");
                        txtRetypePassword.setText("");
                    });
                    return;
                }
                showClearNotification();

                boolean encryptStatus = storage.encryptIdentityKey(txtNewPassword.getText().toString(), entropyHarvester);
                if (!encryptStatus) {
                    showErrorMessage(R.string.encrypt_identity_fail);

                    handler.post(() -> {
                        progressPopupWindow.dismiss();
                        txtCurrentPassword.setText("");
                        txtNewPassword.setText("");
                        txtRetypePassword.setText("");
                    });
                    return;
                }

                storage.clear();

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        APPS_PREFERENCES,
                        Context.MODE_PRIVATE
                );
                long currentId = sharedPref.getLong(CURRENT_ID, 0);
                mDbHelper.updateIdentityData(currentId, storage.createSaveData());

                handler.post(() -> {
                    txtCurrentPassword.setText("");
                    txtNewPassword.setText("");
                    txtRetypePassword.setText("");
                    progressPopupWindow.dismiss();
                    ChangePasswordActivity.this.finish();
                });
            }).start();
        });
    }


}
