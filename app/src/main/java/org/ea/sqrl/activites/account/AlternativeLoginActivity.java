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

        setupLoginPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());
        setupBasePopups(getLayoutInflater(), false);

        final SQRLStorage storage = SQRLStorage.getInstance(AlternativeLoginActivity.this.getApplicationContext());

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
                doLogin(storage, txtLoginPassword, false, false, AlternativeLoginActivity.this, this);
            }
        });
    }
}
