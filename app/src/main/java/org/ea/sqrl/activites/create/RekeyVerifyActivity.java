package org.ea.sqrl.activites.create;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.RescueCodeInputHelper;

/**
 *
 * @author Daniel Persson
 */
public class RekeyVerifyActivity extends LoginBaseActivity {
    private static final String TAG = "RekeyVerifyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekey_verify);

        SQRLStorage storage = SQRLStorage.getInstance(RekeyVerifyActivity.this.getApplicationContext());
        storage.clear();

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        rootView = findViewById(R.id.rekeyVerifyActivityView);
        final TextView txtTooManyRekey = findViewById(R.id.txtTooManyRekey);
        final Button btnRekeyIdentityStart = findViewById(R.id.btnRekeyIdentityStart);

        RescueCodeInputHelper rescueCodeInputHelper = new RescueCodeInputHelper(
                this, rootView, btnRekeyIdentityStart, false);
        rescueCodeInputHelper.setStatusChangedListener(successfullyCompleted -> {
            btnRekeyIdentityStart.setEnabled(successfullyCompleted);
        });

        if(storage.hasAllPreviousKeys()) {
            txtTooManyRekey.setVisibility(View.VISIBLE);
        }

        btnRekeyIdentityStart.setEnabled(false);
        btnRekeyIdentityStart.setOnClickListener(
                v -> {
                    handler.post(() -> showProgressPopup());

                    new Thread(() -> {
                        String rescueCode = rescueCodeInputHelper.getRescueCodeInput();
                        boolean decryptRescueCode = storage.decryptUnlockKey(rescueCode);
                        if (!decryptRescueCode) {
                            Log.e(TAG, "Incorrect decryptRescue");
                            showErrorMessage(R.string.decrypt_identity_fail);
                            handler.post(() -> hideProgressPopup());
                            return;
                        }
                        this.finish();
                        startActivity(new Intent(this, EntropyGatherActivity.class));
                    }).start();
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }
}
