package org.ea.sqrl.activites.create;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.Utils;

/**
 *
 * @author Daniel Persson
 */
public class RekeyIdentityActivity extends AppCompatActivity {
    private static final String TAG = "RekeyIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekey_identity);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);

        Utils.setLanguage(this);

        SQRLStorage.getInstance(RekeyIdentityActivity.this.getApplicationContext()).clear();

        final TextView txtRekeyIdentityMessage = findViewById(R.id.txtRekeyIdentityMessage);
        txtRekeyIdentityMessage.setMovementMethod(LinkMovementMethod.getInstance());

        final Button btnRekeyIdentityStart = findViewById(R.id.btnRekeyIdentityStart);
        btnRekeyIdentityStart.setOnClickListener(
                v -> {
                    this.finish();
                    startActivity(new Intent(this, RekeyVerifyActivity.class));
                }
        );
    }
}
