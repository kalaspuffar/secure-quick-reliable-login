package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;

import org.ea.sqrl.R;

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

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);

        if (!runningTest && mDbHelper.hasIdentities()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(START_USER_MODE, START_USER_MODE_RETURNING_USER);
            startActivity(intent);
        }

        final TextView txtWelcomeMessage = findViewById(R.id.txtWelcomeMessage);
        txtWelcomeMessage.setMovementMethod(LinkMovementMethod.getInstance());

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(this.getString(R.string.button_scan_secret));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        setupProgressPopupWindow(getLayoutInflater());

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
    }

    @Override
    protected void permissionOkCallback() {
        if(createNewIdentity) {
            startActivity(new Intent(this, CreateIdentityActivity.class));
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(START_USER_MODE, START_USER_MODE_NEW_USER);
            startActivity(intent);
        }
    }
 }
