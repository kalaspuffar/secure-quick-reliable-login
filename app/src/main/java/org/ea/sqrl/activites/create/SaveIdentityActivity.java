package org.ea.sqrl.activites.create;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.PasswordStrengthMeter;

/**
 *
 * @author Daniel Persson
 */
public class SaveIdentityActivity extends LoginBaseActivity {
    private static final String TAG = "SaveIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_identity);

        rootView = findViewById(R.id.saveIdentityActivityView);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        SQRLStorage storage = SQRLStorage.getInstance(SaveIdentityActivity.this.getApplicationContext());

        final EditText txtIdentityName = findViewById(R.id.txtIdentityName);
        final EditText txtNewPassword = findViewById(R.id.txtNewPassword);
        final EditText txtRetypePassword = findViewById(R.id.txtRetypePassword);
        final ViewGroup pwStrengthMeter = findViewById(R.id.passwordStrengthMeter);

        final PasswordStrengthMeter passwordStrengthMeter = new PasswordStrengthMeter(
                this, txtNewPassword, pwStrengthMeter);


        final Button btnSaveIdentity = findViewById(R.id.btnSaveIdentity);
        btnSaveIdentity.setOnClickListener(v -> {
            if(!txtNewPassword.getText().toString().equals(txtRetypePassword.getText().toString())) {
                txtRetypePassword.setError(getString(R.string.change_password_retyped_password_do_not_match));
                return;
            }
            txtRetypePassword.setError(null);

            showProgressPopup();

            new Thread(() -> {
                try {
                    boolean encryptRescueCode = storage.encryptRescueKey(entropyHarvester);
                    if (!encryptRescueCode) {
                        Log.e(TAG, "Incorrect encryptRescue");
                        showErrorMessage(R.string.encrypt_identity_fail);
                        return;
                    }

                    storage.reInitializeMasterKeyIdentity();

                    boolean encryptStatus = storage.encryptIdentityKey(txtNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        Log.e(TAG, "Incorrect Password");
                        showErrorMessage(R.string.encrypt_identity_fail);
                        return;
                    }
                } finally {
                    storage.clear();
                    handler.post(() -> hideProgressPopup());
                }

                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                mDbHelper.updateIdentityName(newIdentityId, txtIdentityName.getText().toString());

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        APPS_PREFERENCES,
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(CURRENT_ID, newIdentityId);
                editor.apply();

                handler.post(() -> {
                    txtIdentityName.setText("");
                    txtNewPassword.setText("");
                    txtRetypePassword.setText("");

                    SaveIdentityActivity.this.finish();
                    startActivity(new Intent(this, NewIdentityDoneActivity.class));
                });
            }).start();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }
}
