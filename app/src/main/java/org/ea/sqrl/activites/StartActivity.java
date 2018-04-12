package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;

/**
 * Start activity should be a base for the user so we bring them into the application and they know
 * how to use it when installed and identities are added. So where we add some text for to inform
 * as well as a link to import your first identity.
 *
 * @author Daniel Persson
 */
public class StartActivity extends BaseActivity {
    private static final String TAG = "StartActivity";
    private View root;
    private boolean createNewIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        root = findViewById(R.id.startActivityView);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d(TAG, "Cancelled scan");
                Snackbar.make(root, "Cancelled", Snackbar.LENGTH_LONG).show();
            } else {
                SQRLStorage storage = SQRLStorage.getInstance();
                try {
                    byte[] qrCodeData = EncryptionUtils.readSQRLQRCode(result.getRawBytes());
                    storage.read(qrCodeData);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    return;
                }
                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        getString(R.string.preferences),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(getString(R.string.current_id), newIdentityId);
                editor.apply();

                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
    }
}
