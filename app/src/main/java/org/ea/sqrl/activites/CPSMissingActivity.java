package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;


public class CPSMissingActivity  extends LoginBaseActivity {
    private static final String TAG = "CPSMissingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cps_missing);

        communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);
        communicationFlowHandler.setUrlBasedLogin(true);
        setupBasePopups(getLayoutInflater());

        final TextView txtSite = findViewById(R.id.txtSite);
        txtSite.setText(new String(communicationFlowHandler.getDomain()));

        SQRLStorage storage = SQRLStorage.getInstance(CPSMissingActivity.this.getApplicationContext());

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

        final Button btnCPSContinue = findViewById(R.id.btnCPSContinue);
        btnCPSContinue.setOnClickListener(v -> {
            showProgressPopup();
            new Thread(() -> {
                communicationFlowHandler.setNoCPSServer();
                communicationFlowHandler.handleNextAction();
            }).start();
        });

        final Button btnCPSCancel = findViewById(R.id.btnCPSCancel);
        btnCPSCancel.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            this.finish();
        });
    }

    @Override
    protected void closeActivity() {
        CPSMissingActivity.this.finishAffinity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CPSMissingActivity.this.finishAndRemoveTask();
        }
    }
}
