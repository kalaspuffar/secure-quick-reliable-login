package org.ea.sqrl.activites.account;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.RescueCodeInputHelper;

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

        ViewGroup rootLayout = findViewById(R.id.enableAccountActivityView);
        Button btnEnableAccount = findViewById(R.id.btnEnableAccountEnable);

        RescueCodeInputHelper rescueCodeInputHelper = new RescueCodeInputHelper(
                this, rootLayout, btnEnableAccount, false);
        rescueCodeInputHelper.setStatusChangedListener(successfullyCompleted -> {
            btnEnableAccount.setEnabled(successfullyCompleted);
        });
        rescueCodeInputHelper.requestFocus();

        btnEnableAccount.setEnabled(false);

        btnEnableAccount.setOnClickListener((View v) -> {
            SQRLStorage storage = SQRLStorage.getInstance(EnableAccountActivity.this.getApplicationContext());

            handler.post(() -> showProgressPopup());

            new Thread(() -> {
                try {
                    String rescueCode = rescueCodeInputHelper.getRescueCodeInput();

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
                        rescueCodeInputHelper.clearForm();
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
