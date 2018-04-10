package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

/**
 *
 * @author Daniel Persson
 */
public class NewIdentityDoneActivity extends LoginBaseActivity {
    private static final String TAG = "RekeyIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekey_identity);

        SQRLStorage.getInstance().clear();

        setupProgressPopupWindow(getLayoutInflater());

        rootView = findViewById(R.id.newIdentityDoneActivityView);

        final TextView txtRekeyIdentityMessage = findViewById(R.id.txtNewIdentityDoneMessage);
        txtRekeyIdentityMessage.setMovementMethod(LinkMovementMethod.getInstance());

        final Button btnNewIdentityDone = findViewById(R.id.btnNewIdentityDone);
        btnNewIdentityDone.setOnClickListener(
                v -> startActivity(new Intent(this, MainActivity.class))
        );
    }
}
