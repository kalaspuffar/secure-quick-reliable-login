package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ea.sqrl.ProgressionUpdater;
import org.ea.sqrl.R;
import org.ea.sqrl.database.IdentityContract.IdentityEntry;
import org.ea.sqrl.utils.EncryptionUtils;
import org.ea.sqrl.storage.SQRLStorage;

/**
 *
 * @author Daniel Persson
 */
public class DecryptingActivity extends BaseActivity {

    private Handler handler = new Handler();
    private byte[] qrCodeData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypting);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        //int defaultValue = getResources().getInteger(R.integer.saved_high_score_default);
        long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
        byte[] qrCodeData = mDbHelper.getIdentityData(currentId);

        if (qrCodeData.length == 0) {
            Intent intent = new Intent(DecryptingActivity.this, StartActivity.class);
            startActivity(intent);
        }

        final ProgressBar pbDecrypting = findViewById(R.id.pbDecrypting);
        final EditText txtPassword = findViewById(R.id.txtPassword);
        final Button btnDecryptKey = findViewById(R.id.btnDecryptKey);
        final TextView progressText = findViewById(R.id.lblProgressText);
        final TextView txtRecoveryKey = findViewById(R.id.txtRecoveryKey);

        SQRLStorage storage = SQRLStorage.getInstance();
        try {
            storage.read(qrCodeData, true);
            storage.setProgressionUpdater(new ProgressionUpdater(handler, pbDecrypting, progressText));
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        txtRecoveryKey.setText(storage.getVerifyingRecoveryBlock());

        btnDecryptKey.setOnClickListener(v -> new Thread(() -> {
            try {
                boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString());

                if(decryptStatus) {
                    Intent intent = new Intent(DecryptingActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    progressText.setTextColor(Color.RED);
                    progressText.setText(R.string.error_incorrect_password);
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
                e.printStackTrace();
            }

        }).start());
    }
}
