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

            final CommunicationHandler commHandler = CommunicationHandler.getInstance();
            final String domain = sqrlLink.split("/")[2];
            commHandler.setDomain(domain);

            final TextView txtSite = findViewById(R.id.txtSite);
            txtSite.setText(domain);

            final Button btnScanNew = findViewById(R.id.btnScanNew);
            btnScanNew.setOnClickListener(v -> new Thread(() -> {
                Intent intent = new Intent(LoginActivity.this, ScanActivity.class);
                intent.putExtra(ScanActivity.SCAN_MODE_MESSAGE, ScanActivity.SCAN_MODE_LOGIN);
                startActivity(intent);
            }).start());

            final Button btnLogin = findViewById(R.id.btnLogin);
            btnLogin.setOnClickListener(v -> new Thread(() -> {
                int indexOfQuery = sqrlLink.indexOf("/", sqrlLink.indexOf("://")+3);
                String queryLink = sqrlLink.substring(indexOfQuery);
                try {
                    String postData = commHandler.createPostParams(commHandler.createClientQueryData(), sqrlLink);
                    String response = commHandler.postRequest(queryLink, postData);
                    System.out.println("Response: " + response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start());
        }

    }
}
