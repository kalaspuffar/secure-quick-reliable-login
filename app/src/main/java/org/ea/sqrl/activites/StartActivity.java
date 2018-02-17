package org.ea.sqrl.activites;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.ProgressionUpdater;
import org.ea.sqrl.R;
import org.ea.sqrl.database.IdentityContract;
import org.ea.sqrl.storage.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;

import java.util.Map;

/**
 *
 * @author Daniel Persson
 */
public class StartActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        if (mDbHelper.hasIdentities()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

        final TextView txtWelcomeMessage = findViewById(R.id.txtWelcomeMessage);
        txtWelcomeMessage.setMovementMethod(new ScrollingMovementMethod());

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(this.getString(R.string.button_scan_secret));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        final Button btnScanSecret = findViewById(R.id.btnScanSecret);
        btnScanSecret.setOnClickListener(
                v -> new Thread(() -> integrator.initiateScan()).start()
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                SQRLStorage storage = SQRLStorage.getInstance();
                try {
                    byte[] qrCodeData = EncryptionUtils.readSQRLQRCode(result.getRawBytes());
                    storage.read(qrCodeData, true);
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        getString(R.string.preferences),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(getString(R.string.current_id), newIdentityId);
                editor.commit();

                Intent intent = new Intent(this, DecryptingActivity.class);
                startActivity(intent);
            }
        }
    }
}
