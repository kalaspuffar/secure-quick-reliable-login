package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

public class SaveIdentityActivity extends LoginBaseActivity {
    private static final String TAG = "SaveIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_identity);

        setupProgressPopupWindow(getLayoutInflater());

        rootView = findViewById(R.id.saveIdentityActivityView);

        SQRLStorage storage = SQRLStorage.getInstance();

        final EditText txtIdentityName = findViewById(R.id.txtIdentityName);
        final EditText txtNewPassword = findViewById(R.id.txtNewPassword);
        final EditText txtRetypePassword = findViewById(R.id.txtRetypePassword);

        final Button btnSaveIdentity = findViewById(R.id.btnSaveIdentity);
        btnSaveIdentity.setOnClickListener(v -> {
            if(!txtNewPassword.getText().toString().equals(txtRetypePassword.getText().toString())) {
                Snackbar.make(rootView, getString(R.string.change_password_retyped_password_do_not_match), Snackbar.LENGTH_LONG).show();
                txtNewPassword.setError("");
                txtRetypePassword.setError("");
                return;
            }
            txtNewPassword.setError(null);
            txtRetypePassword.setError(null);

            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                try {
                    boolean encryptRescueCode = storage.encryptRescueKey(entropyHarvester);
                    if (!encryptRescueCode) {
                        Log.e(TAG, "Incorrect encryptRescue");
                        handler.post(() -> {
                            Snackbar.make(rootView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                        });
                        return;
                    }

                    storage.reInitializeMasterKeyIdentity();

                    boolean encryptStatus = storage.encryptIdentityKey(txtNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        Log.e(TAG, "Incorrect Password");
                        handler.post(() -> {
                            Snackbar.make(rootView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                        });
                        return;
                    }
                } finally {
                    storage.clear();
                    handler.post(() -> progressPopupWindow.dismiss());
                }

                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                mDbHelper.updateIdentityName(newIdentityId, txtIdentityName.getText().toString());

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        getString(R.string.preferences),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(getString(R.string.current_id), newIdentityId);
                editor.apply();

                handler.post(() -> {
                    txtIdentityName.setText("");
                    txtNewPassword.setText("");
                    txtRetypePassword.setText("");

                    startActivity(new Intent(this, MainActivity.class));
                });
            }).start();
        });
    }
}
