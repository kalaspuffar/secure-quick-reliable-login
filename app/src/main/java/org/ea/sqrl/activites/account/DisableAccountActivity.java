package org.ea.sqrl.activites.account;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;

public class DisableAccountActivity extends BaseActivity {
    protected CommunicationFlowHandler communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disable_account);

        communicationFlowHandler.setupAskPopupWindow(getLayoutInflater(), handler);
        communicationFlowHandler.setupErrorPopupWindow(getLayoutInflater());
        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        SQRLStorage storage = SQRLStorage.getInstance();

        final EditText txtDisablePassword = findViewById(R.id.txtDisablePassword);
        findViewById(R.id.btnDisableAccount).setOnClickListener(v -> {
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                boolean decryptionOk = storage.decryptIdentityKey(txtDisablePassword.getText().toString(), entropyHarvester, false);
                if(decryptionOk) {
                    showClearNotification();
                } else {
                    showErrorMessage(R.string.decrypt_identity_fail);
                    storage.clear();
                    handler.post(() -> {
                        txtDisablePassword.setText("");
                        progressPopupWindow.dismiss();
                    });
                    return;
                }
                txtDisablePassword.setText("");

                if(communicationFlowHandler.isUrlBasedLogin()) {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOCK_ACCOUNT_CPS);
                } else {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOCK_ACCOUNT);
                }

                communicationFlowHandler.setDoneAction(() -> {
                    storage.clear();
                    handler.post(() -> {
                        progressPopupWindow.dismiss();
                        closeActivity();
                    });
                });

                communicationFlowHandler.setErrorAction(() -> {
                    storage.clear();
                    handler.post(() -> progressPopupWindow.dismiss());
                });

                communicationFlowHandler.handleNextAction();

            }).start();
        });
    }

    protected void closeActivity() {
        if(communicationFlowHandler.isUrlBasedLogin()) {
            DisableAccountActivity.this.finishAffinity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DisableAccountActivity.this.finishAndRemoveTask();
            }
        } else {
            DisableAccountActivity.this.finish();
        }
    }
}
