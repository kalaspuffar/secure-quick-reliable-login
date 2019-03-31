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
 * Start activity should be a base for the user so we bring them into the application and they know
 * how to use it when installed and identities are added. So where we add some text for to inform
 * as well as a link to import your first identity.
 *
 * @author Daniel Persson
 */
public class CreateIdentityActivity extends AppCompatActivity {
    private static final String TAG = "CreateIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_identity);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);

        Utils.setLanguage(this);

        SQRLStorage.getInstance(CreateIdentityActivity.this.getApplicationContext()).cleanIdentity();

        final TextView txtCreateIdentityMessage = findViewById(R.id.txtCreateIdentityMessage);
        txtCreateIdentityMessage.setMovementMethod(LinkMovementMethod.getInstance());

        final Button btnCreateIdentityCreate = findViewById(R.id.btnCreateIdentityCreate);
        btnCreateIdentityCreate.setOnClickListener(
                v -> {
                    this.finish();
                    startActivity(new Intent(this, EntropyGatherActivity.class));
                }
        );
    }
}
