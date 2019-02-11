package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.activites.create.CreateIdentityActivity;
import org.ea.sqrl.activites.identity.ImportActivity;
import org.ea.sqrl.activites.identity.TextImportActivity;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;

/**
 * Start activity should be a base for the user so we bring them into the application and they know
 * how to use it when installed and identities are added. So where we add some text for to inform
 * as well as a link to import your first identity.
 *
 * @author Daniel Persson
 */
public class StartActivity extends BaseActivity {
    private static final String TAG = "StartActivity";
    private boolean createNewIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final TextView txtWelcomeMessage = findViewById(R.id.txtWelcomeMessage);
        txtWelcomeMessage.setMovementMethod(LinkMovementMethod.getInstance());

        setupProgressPopupWindow(getLayoutInflater());
        setupCameraAccessPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        final Button btnScanSecret = findViewById(R.id.btnScanSecret);
        btnScanSecret.setOnClickListener(v -> {
            createNewIdentity = false;
            this.showPhoneStatePermission();
        });

        final Button btnStartCreateIdentity = findViewById(R.id.btnStartCreateIdentity);
        btnStartCreateIdentity.setOnClickListener(v -> {
            createNewIdentity = true;
            this.showPhoneStatePermission();
        });

        final Button btnTextImport = findViewById(R.id.btnTextImport);
        btnTextImport.setOnClickListener(
            v -> startActivity(new Intent(this, TextImportActivity.class))
        );
    }

    @Override
    protected void permissionOkCallback() {
        if(createNewIdentity) {
            startActivity(new Intent(this, CreateIdentityActivity.class));
        } else {
            startActivity(new Intent(this, ImportActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        StartActivity.this.finishAffinity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            StartActivity.this.finishAndRemoveTask();
        }
    }
 }
