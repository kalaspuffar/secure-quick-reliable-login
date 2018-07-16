package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

/**
 *
 * @author Daniel Persson
 */
public class RekeyVerifyActivity extends LoginBaseActivity {
    private static final String TAG = "RekeyIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekey_verify);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.clear();

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        rootView = findViewById(R.id.rekeyVerifyActivityView);

        final TextView txtTooManyRekey = findViewById(R.id.txtTooManyRekey);
        if(storage.hasAllPreviousKeys()) {
            txtTooManyRekey.setVisibility(View.VISIBLE);
        }


        final EditText txtRecoverCode1 = findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = findViewById(R.id.txtRecoverCode6);

        final Button btnRekeyIdentityStart = findViewById(R.id.btnRekeyIdentityStart);
        btnRekeyIdentityStart.setOnClickListener(
                v -> {
                    handler.post(() -> progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0));

                    new Thread(() -> {
                        String rescueCode = "";
                        rescueCode += txtRecoverCode1.getText().toString();
                        rescueCode += txtRecoverCode2.getText().toString();
                        rescueCode += txtRecoverCode3.getText().toString();
                        rescueCode += txtRecoverCode4.getText().toString();
                        rescueCode += txtRecoverCode5.getText().toString();
                        rescueCode += txtRecoverCode6.getText().toString();

                        boolean decryptRescueCode = storage.decryptUnlockKey(rescueCode);
                        if (!decryptRescueCode) {
                            Log.e(TAG, "Incorrect decryptRescue");
                            showErrorMessage(R.string.decrypt_identity_fail);
                            handler.post(() -> progressPopupWindow.dismiss());
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
