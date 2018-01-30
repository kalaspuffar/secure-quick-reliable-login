package org.ea.sqrl.activites;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.ProgressionUpdater;
import org.ea.sqrl.R;
import org.ea.sqrl.storage.CommunicationHandler;
import org.ea.sqrl.storage.EncryptionUtils;
import org.ea.sqrl.storage.SQRLStorage;

import java.io.File;
import java.io.FileInputStream;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            byte[] rawQRData = extras.getByteArray(ScanActivity.EXTRA_MESSAGE);
            String hexdata = EncryptionUtils.byte2hex(rawQRData);
            System.out.println(hexdata);
            int end = hexdata.indexOf("0ec11");
            byte[] qrCodeData = EncryptionUtils.hex2Byte(hexdata.substring(3, end));
            System.out.println(EncryptionUtils.byte2hex(qrCodeData));

            String sqrlLink = new String(qrCodeData);
            String domain = sqrlLink.split("/")[2];

            final TextView txtSite = findViewById(R.id.txtSite);
            txtSite.setText(domain);

            final Button btnDecryptKey = findViewById(R.id.btnLogin);
            btnDecryptKey.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        public void run() {
                            SQRLStorage storage = SQRLStorage.getInstance();
                            CommunicationHandler commHandler = new CommunicationHandler();
                            String serverData = sqrlLink.substring(sqrlLink.indexOf("://")+3);

                            try {
                                byte[] privateKey = commHandler.getPrivateKey(storage.getMasterKey(), domain);
                                String postData = commHandler.createPostParams(commHandler.createClientQueryData(), sqrlLink, privateKey);
                                String response = commHandler.postRequest(serverData, postData);
                                System.out.println(CommunicationHandler.decodeUrlSafe(response));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            });
        }

    }
}
