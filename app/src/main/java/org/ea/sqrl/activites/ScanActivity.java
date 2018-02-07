package org.ea.sqrl.activites;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;

/**
 *
 * @author Daniel Persson
 */
public class ScanActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "org.ea.sqrl.QRCODE";
    public static final String SCAN_MODE_MESSAGE = "org.ea.sqrl.SCANMODE";

    public static final int SCAN_MODE_SECRET = 1;
    public static final int SCAN_MODE_LOGIN = 2;
    private int scanMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan_secret);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            scanMode = extras.getInt(ScanActivity.SCAN_MODE_MESSAGE);
        } else {
            scanMode = SCAN_MODE_SECRET;
        }

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = null;
                switch (scanMode) {
                    case SCAN_MODE_SECRET:
                        intent = new Intent(this, DecryptingActivity.class);
                        break;
                    case SCAN_MODE_LOGIN:
                        intent = new Intent(this, LoginActivity.class);
                        break;
                    default:
                        intent = new Intent(this, IntroductionActivity.class);
                }
                try {
                    intent.putExtra(EXTRA_MESSAGE, result.getRawBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                startActivity(intent);
            }
        }
    }
}
