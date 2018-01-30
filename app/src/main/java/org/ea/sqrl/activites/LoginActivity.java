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
            String sqrlLink = EncryptionUtils.readSQRLQRCodeAsString(rawQRData);

            final String domain = sqrlLink.split("/")[2];

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
                                System.out.println("Response: " + response);

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
