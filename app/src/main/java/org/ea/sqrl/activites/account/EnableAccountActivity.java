package org.ea.sqrl.activites.account;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;

public class EnableAccountActivity extends BaseActivity {
    private static final String TAG = "EnableAccountActivity";

    protected CommunicationFlowHandler communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable_account);

        communicationFlowHandler.setupAskPopupWindow(getLayoutInflater(), handler);
        communicationFlowHandler.setupErrorPopupWindow(getLayoutInflater());
        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        final EditText txtRecoverCode1 = findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = findViewById(R.id.txtRecoverCode6);

        txtRecoverCode1.requestFocus();

        findViewById(R.id.btnEnableAccountEnable).setOnClickListener((View v) -> {
            SQRLStorage storage = SQRLStorage.getInstance(EnableAccountActivity.this.getApplicationContext());

            if(!checkRescueCode(txtRecoverCode1)) return;
            if(!checkRescueCode(txtRecoverCode2)) return;
            if(!checkRescueCode(txtRecoverCode3)) return;
            if(!checkRescueCode(txtRecoverCode4)) return;
            if(!checkRescueCode(txtRecoverCode5)) return;
            if(!checkRescueCode(txtRecoverCode6)) return;

            handler.post(() -> showProgressPopup());

            new Thread(() -> {
                try {
                    String rescueCode = txtRecoverCode1.getText().toString();
                    rescueCode += txtRecoverCode2.getText().toString();
                    rescueCode += txtRecoverCode3.getText().toString();
                    rescueCode += txtRecoverCode4.getText().toString();
                    rescueCode += txtRecoverCode5.getText().toString();
                    rescueCode += txtRecoverCode6.getText().toString();

                    boolean decryptionOk = storage.decryptUnlockKey(rescueCode);
                    if (!decryptionOk) {
                        showErrorMessage(R.string.decrypt_identity_fail);
                        return;
                    }
                    storage.reInitializeMasterKeyIdentity();
                } catch (Exception e) {
                    showErrorMessage(e.getMessage());
                    Log.e(TAG, e.getMessage(), e);
                    this.closeActivity();
                    storage.clear();
                    return;
                } finally {
                    handler.post(() -> {
                        txtRecoverCode1.setText("");
                        txtRecoverCode2.setText("");
                        txtRecoverCode3.setText("");
                        txtRecoverCode4.setText("");
                        txtRecoverCode5.setText("");
                        txtRecoverCode6.setText("");
                    });
                }

                if(communicationFlowHandler.isUrlBasedLogin()) {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITH_SUK);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.UNLOCK_ACCOUNT_CPS);
                } else {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITH_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.UNLOCK_ACCOUNT);
                }

                communicationFlowHandler.setDoneAction(() -> {
                    storage.clear();
                    handler.post(() -> {
                        hideProgressPopup();
                        closeActivity();
                    });
                });

                communicationFlowHandler.setErrorAction(() -> {
                    storage.clear();
                    handler.post(() -> hideProgressPopup());
                });

                communicationFlowHandler.handleNextAction();
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

    protected void closeActivity() {
        if(communicationFlowHandler.isUrlBasedLogin()) {
            EnableAccountActivity.this.finishAffinity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                EnableAccountActivity.this.finishAndRemoveTask();
            }
        } else {
            EnableAccountActivity.this.finish();
        }
    }
}
