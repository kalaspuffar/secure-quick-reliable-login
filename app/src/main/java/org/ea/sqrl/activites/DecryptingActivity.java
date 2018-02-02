package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ea.sqrl.ProgressionUpdater;
import org.ea.sqrl.R;
import org.ea.sqrl.storage.CommunicationHandler;
import org.ea.sqrl.storage.EncryptionUtils;
import org.ea.sqrl.storage.SQRLStorage;

public class DecryptingActivity extends BaseActivity {

    private Handler handler = new Handler();
    private byte[] qrCodeData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypting);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            byte[] rawQRData = extras.getByteArray(ScanActivity.EXTRA_MESSAGE);
            qrCodeData = EncryptionUtils.readSQRLQRCode(rawQRData);
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
                    progressText.setText(R.string.error_incorrect_password);
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
                e.printStackTrace();
            }

        }).start());
    }
}
