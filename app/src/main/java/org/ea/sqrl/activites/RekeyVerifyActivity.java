package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

/**
 * Start activity should be a base for the user so we bring them into the application and they know
 * how to use it when installed and identities are added. So where we add some text for to inform
 * as well as a link to import your first identity.
 *
 * @author Daniel Persson
 */
public class RekeyVerifyActivity extends LoginBaseActivity {
    private static final String TAG = "RekeyIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekey_verify);

        SQRLStorage.getInstance().clear();

        setupProgressPopupWindow(getLayoutInflater());

        rootView = findViewById(R.id.rekeyVerifyActivityView);

        final EditText txtRecoverCode1 = findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = findViewById(R.id.txtRecoverCode6);

        SQRLStorage storage = SQRLStorage.getInstance();

        final Button btnRekeyIdentityStart = findViewById(R.id.btnRekeyIdentityStart);
        btnRekeyIdentityStart.setOnClickListener(
                v -> {
                    progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

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
                            handler.post(() -> {
                                Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            });
                            progressPopupWindow.dismiss();
                            return;
                        }
                        Intent intent = new Intent(this, EntropyGatherActivity.class);
                        startActivity(intent);
                    }).start();
                }
        );
    }
}
