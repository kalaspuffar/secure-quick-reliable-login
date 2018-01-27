package org.ea.sqrl;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.ea.sqrl.storage.EncryptionUtils;
import org.ea.sqrl.storage.SQRLStorage;

import java.util.Arrays;

public class DecryptingActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    private byte[] qrCodeData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypting);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            byte[] rawQRData = extras.getByteArray(ScanSecretActivity.EXTRA_MESSAGE);
            String hexdata = EncryptionUtils.byte2hex(rawQRData);
            int end = hexdata.indexOf("0ec11ec11");
            qrCodeData = EncryptionUtils.hex2Byte(hexdata.substring(3, end));
            System.out.println(EncryptionUtils.byte2hex(qrCodeData));
        }

        final ProgressBar pbDecrypting = findViewById(R.id.pbDecrypting);
        final EditText txtPassword = findViewById(R.id.txtPassword);
        final Button btnDecryptKey = findViewById(R.id.btnDecryptKey);
        btnDecryptKey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            //byte[] store = EncryptionUtils.hex2Byte("7371726c646174617d0001002d000f1deaebeb3e30e24695ef8032e90fe3fa1f61d564b6e30540e9dd1a09c6000000f30104050f00c4a90023c950a4d684687c337abbe63bf4a184b2dd3ae19af3a6d9991ff1018b1817633edfb14f153ae3ea47dcd34b6a60986dfa0915a62db2e550f1eab552f7305de5587e37504bea3c34a2013b8ee44900020079d1d5da2d4b046212f43da2f7b0a39709ca000000d5dd50893d516c8175291f7d905b5bf5636d26fee5d3f8801375f7824b09a2a824de7fc41451ca13e610d5591d568db6");//
                            SQRLStorage storage = new SQRLStorage(qrCodeData, true);
                            storage.setProgressBar(pbDecrypting);
                            storage.setHandler(handler);
                            storage.decryptData(txtPassword.getText().toString());
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
