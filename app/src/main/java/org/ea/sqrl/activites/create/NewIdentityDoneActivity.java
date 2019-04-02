package org.ea.sqrl.activites.create;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.identity.ExportOptionsActivity;
import org.ea.sqrl.activites.MainActivity;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.SQRLStorage;

/**
 *
 * @author Daniel Persson
 */
public class NewIdentityDoneActivity extends LoginBaseActivity {
    private static final String TAG = "NewIdentityDoneActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_identity_done);

        SQRLStorage.getInstance(NewIdentityDoneActivity.this.getApplicationContext()).clear();

        setupErrorPopupWindow(getLayoutInflater());

        rootView = findViewById(R.id.newIdentityDoneActivityView);

        final TextView txtNewIdentityDoneMessage = findViewById(R.id.txtNewIdentityDoneMessage);
        txtNewIdentityDoneMessage.setMovementMethod(LinkMovementMethod.getInstance());

        final Button btnNewIdentityDoneExport = findViewById(R.id.btnNewIdentityDoneExport);
        btnNewIdentityDoneExport.setOnClickListener(
                v -> startActivity(new Intent(this, ExportOptionsActivity.class))
        );

        final Button btnNewIdentityDone = findViewById(R.id.btnNewIdentityDone);
        btnNewIdentityDone.setOnClickListener(
                v -> {
                    this.finish();
                    startActivity(new Intent(this, MainActivity.class));
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onBackPressed() {
        NewIdentityDoneActivity.this.finishAffinity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NewIdentityDoneActivity.this.finishAndRemoveTask();
        }
    }
}
