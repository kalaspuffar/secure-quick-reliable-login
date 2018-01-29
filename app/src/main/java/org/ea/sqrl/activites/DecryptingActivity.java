package org.ea.sqrl.activites;

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
import org.ea.sqrl.storage.EncryptionUtils;
import org.ea.sqrl.storage.SQRLStorage;

public class DecryptingActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    private byte[] qrCodeData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypting);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            byte[] rawQRData = extras.getByteArray(ScanActivity.EXTRA_MESSAGE);
            String hexdata = EncryptionUtils.byte2hex(rawQRData);
            int end = hexdata.indexOf("0ec11ec11");
            qrCodeData = EncryptionUtils.hex2Byte(hexdata.substring(3, end));
            System.out.println(EncryptionUtils.byte2hex(qrCodeData));
        }

        final ProgressBar pbDecrypting = findViewById(R.id.pbDecrypting);
        final EditText txtPassword = findViewById(R.id.txtPassword);
        final Button btnDecryptKey = findViewById(R.id.btnDecryptKey);
        final TextView progressText = findViewById(R.id.lblProgressText);
        btnDecryptKey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            SQRLStorage storage = SQRLStorage.getInstance();
                            storage.read(qrCodeData, true);
                            storage.setProgressionUpdater(new ProgressionUpdater(handler, pbDecrypting, progressText));
                            storage.decryptIdentityKey(txtPassword.getText().toString());
                        } catch (Exception e) {
                            System.out.println("ERROR: " + e.getMessage());
                            e.printStackTrace();
                        }

                    }
                }).start();
            }
        });
    }
}
