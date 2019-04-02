package org.ea.sqrl.activites.identity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.SimplifiedActivity;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;

public class ResetPasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        final boolean newIdentity = getIntent().getBooleanExtra(SQRLStorage.NEW_IDENTITY, false);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        final EditText txtRecoverCode1 = findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = findViewById(R.id.txtRecoverCode6);
        final EditText txtResetPasswordNewPassword = findViewById(R.id.txtResetPasswordNewPassword);

        txtRecoverCode1.requestFocus();

        findViewById(R.id.btnResetPassword).setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance(ResetPasswordActivity.this.getApplicationContext());

            if(!checkRescueCode(txtRecoverCode1)) return;
            if(!checkRescueCode(txtRecoverCode2)) return;
            if(!checkRescueCode(txtRecoverCode3)) return;
            if(!checkRescueCode(txtRecoverCode4)) return;
            if(!checkRescueCode(txtRecoverCode5)) return;
            if(!checkRescueCode(txtRecoverCode6)) return;

            showProgressPopup();

            new Thread(() -> {
                String rescueCode = txtRecoverCode1.getText().toString();
                rescueCode += txtRecoverCode2.getText().toString();
                rescueCode += txtRecoverCode3.getText().toString();
                rescueCode += txtRecoverCode4.getText().toString();
                rescueCode += txtRecoverCode5.getText().toString();
                rescueCode += txtRecoverCode6.getText().toString();

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

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        APPS_PREFERENCES,
                        Context.MODE_PRIVATE
                );

                storage.clear();
                handler.post(() -> {
                    hideProgressPopup();
                    txtResetPasswordNewPassword.setText("");
                    txtRecoverCode1.setText("");
                    txtRecoverCode2.setText("");
                    txtRecoverCode3.setText("");
                    txtRecoverCode4.setText("");
                    txtRecoverCode5.setText("");
                    txtRecoverCode6.setText("");
                });

                if(mDbHelper.hasIdentities() && !newIdentity) {
                    long currentId = sharedPref.getLong(CURRENT_ID, 0);
                    if(currentId != 0) {
                        mDbHelper.updateIdentityData(currentId, storage.createSaveData());
                    }
                    handler.post(() -> ResetPasswordActivity.this.finish());
                } else {
                    long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putLong(CURRENT_ID, newIdentityId);
                    editor.apply();
                    if(newIdentity) {
                        handler.post(() -> {
                            ResetPasswordActivity.this.finish();
                            startActivity(new Intent(this, RenameActivity.class));
                        });
                    } else {
                        handler.post(() -> {
                            ResetPasswordActivity.this.finish();
                            startActivity(new Intent(this, SimplifiedActivity.class));
                        });
                    }
                }

            }).start();
        });
    }

    protected boolean checkRescueCode(EditText code) {
        if(code.length() != 4) {
            showErrorMessage(R.string.rescue_code_incorrect_input);
            code.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(code.getText().toString());
        } catch (NumberFormatException nfe) {
            showErrorMessage(R.string.rescue_code_incorrect_input);
            code.requestFocus();
            return false;
        }
        return true;
    }

}
