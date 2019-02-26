package org.ea.sqrl.activites.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;

public class AlternativeLoginActivity extends LoginBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alternative_identity);

        rootView = findViewById(R.id.alternativeIdentityActivityView);
        communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

        setupLoginPopupWindow(getLayoutInflater(), AlternativeLoginActivity.this);
        setupErrorPopupWindow(getLayoutInflater());
        setupBasePopups(getLayoutInflater(), false);

        final SQRLStorage storage = SQRLStorage.getInstance();

        final EditText txtLoginPassword = findViewById(R.id.txtLoginPassword);
        final EditText txtAlternativeId = findViewById(R.id.txtAlternativeId);

        final Button btnAlternativeLogin = findViewById(R.id.btnAlternativeLogin);
        btnAlternativeLogin.setOnClickListener(v -> {
            communicationFlowHandler.setAlternativeId(txtAlternativeId.getText().toString());

            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    APPS_PREFERENCES,
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(CURRENT_ID, 0);

            if(currentId != 0) {
                hideLoginPopup();
                showProgressPopup();
                closeKeyboard();

                new Thread(() -> {
                    boolean decryptionOk = storage.decryptIdentityKey(txtLoginPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptionOk) {
                        showErrorMessage(R.string.decrypt_identity_fail);
                        handler.post(() -> {
                            txtLoginPassword.setText("");
                            hideProgressPopup();
                        });
                        storage.clear();
                        return;
                    }
                    showClearNotification();

                    handler.post(() -> txtLoginPassword.setText(""));

                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);

                    communicationFlowHandler.setDoneAction(() -> {
                        storage.clear();
                        handler.post(() -> {
                            hideProgressPopup();
                            closeActivity();
                        });
                        AlternativeLoginActivity.this.finish();
                    });

                    communicationFlowHandler.setErrorAction(() -> {
                        storage.clear();
                        handler.post(() -> hideProgressPopup());
                    });

                    communicationFlowHandler.handleNextAction();
                }).start();
            }
        });
    }
}
