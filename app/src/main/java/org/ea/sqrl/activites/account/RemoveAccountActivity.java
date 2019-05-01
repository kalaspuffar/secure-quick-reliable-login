package org.ea.sqrl.activites.account;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.RescueCodeInputHelper;

public class RemoveAccountActivity extends BaseActivity {
    private static final String TAG = "RemoveAccountActivity";

    protected CommunicationFlowHandler communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_account);

        communicationFlowHandler.setupAskPopupWindow(getLayoutInflater(), handler);
        communicationFlowHandler.setupErrorPopupWindow(getLayoutInflater());
        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        Button btnRemoveAccount = findViewById(R.id.btnRemoveAccountRemove);
        ViewGroup rootView = findViewById(R.id.removeAccountActivityView);

        RescueCodeInputHelper rescueCodeInputHelper = new RescueCodeInputHelper(
                this, rootView, btnRemoveAccount, false);
        rescueCodeInputHelper.setStatusChangedListener(successfullyCompleted -> {
            btnRemoveAccount.setEnabled(successfullyCompleted);
        });

        btnRemoveAccount.setEnabled(false);
        btnRemoveAccount.setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance(RemoveAccountActivity.this.getApplicationContext());

            showProgressPopup();

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
                    storage.clear();
                    handler.post(() -> hideProgressPopup());
                    handler.postDelayed(() -> closeActivity(), 5000);
                    return;
                } finally {
                    handler.post(() -> {
                        rescueCodeInputHelper.clearForm();
                    });
                }

                if(communicationFlowHandler.isUrlBasedLogin()) {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITH_SUK);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOCK_ACCOUNT_CPS);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.REMOVE_ACCOUNT_CPS);
                } else {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITH_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOCK_ACCOUNT);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.REMOVE_ACCOUNT);
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
            RemoveAccountActivity.this.finishAffinity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                RemoveAccountActivity.this.finishAndRemoveTask();
            }
        } else {
            RemoveAccountActivity.this.finish();
        }
    }
}
