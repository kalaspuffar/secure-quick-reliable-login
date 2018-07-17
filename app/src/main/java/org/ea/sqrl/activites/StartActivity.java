package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.Utils;

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
    private ConstraintLayout rootView;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final TextView txtWelcomeMessage = findViewById(R.id.txtWelcomeMessage);
        txtWelcomeMessage.setMovementMethod(LinkMovementMethod.getInstance());

        rootView = findViewById(R.id.startActivityView);

        setupProgressPopupWindow(getLayoutInflater());
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

    protected void setupProgressPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_progress, null);

        progressPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);


        final ProgressBar progressBar = popupView.findViewById(R.id.pbEntropy);
        final TextView lblProgressTitle = popupView.findViewById(R.id.lblProgressTitle);
        final TextView lblProgressText = popupView.findViewById(R.id.lblProgressText);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, lblProgressTitle, progressBar, lblProgressText));
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
