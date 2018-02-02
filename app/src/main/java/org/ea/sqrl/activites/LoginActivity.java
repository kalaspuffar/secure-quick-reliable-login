package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Handler;
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

public class LoginActivity extends BaseActivity {
    private String serverData = null;
    private String queryLink = null;
    private Handler handler = new Handler();


    private void postQuery(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientQuery(), serverData);
        commHandler.postRequest(queryLink, postData);
        commHandler.printParams();
    }

    private void postCreateAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientCreateAccount(), serverData);
        commHandler.postRequest(queryLink, postData);
        commHandler.printParams();
    }

    private void postLogin(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientLogin(), serverData);
        commHandler.postRequest(queryLink, postData);
        commHandler.printParams();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final TextView txtSite = findViewById(R.id.txtSite);
        final CommunicationHandler commHandler = CommunicationHandler.getInstance();
        final Button btnScanNew = findViewById(R.id.btnScanNew);
        final Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        final Button btnLogin = findViewById(R.id.btnLogin);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            byte[] rawQRData = extras.getByteArray(ScanActivity.EXTRA_MESSAGE);
            String sqrlLink = EncryptionUtils.readSQRLQRCodeAsString(rawQRData);
            int indexOfQuery = sqrlLink.indexOf("/", sqrlLink.indexOf("://")+3);
            queryLink = sqrlLink.substring(indexOfQuery);
            final String domain = sqrlLink.split("/")[2];
            commHandler.setDomain(domain);

            txtSite.setText(domain);

            new Thread(() -> {
                try {
                    postQuery(commHandler);

                    handler.post(() -> {
                        if(
                            commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                            commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)
                        ) {
                            btnLogin.setVisibility(Button.VISIBLE);
                        } else if(commHandler.isTIFZero()){
                            btnCreateAccount.setVisibility(Button.VISIBLE);
                        } else {
                            btnLogin.setVisibility(Button.INVISIBLE);
                            btnCreateAccount.setVisibility(Button.INVISIBLE);
                        }
                    });
                } catch (Exception e) {
                    txtSite.setText(e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }).start();
        }

        btnScanNew.setOnClickListener(v -> new Thread(() -> {
            System.out.println("Scan thing.");
            Intent intent = new Intent(LoginActivity.this, ScanActivity.class);
            intent.putExtra(ScanActivity.SCAN_MODE_MESSAGE, ScanActivity.SCAN_MODE_LOGIN);
            startActivity(intent);
        }).start());

        btnCreateAccount.setOnClickListener(v -> new Thread(() -> {
            try {
                postCreateAccount(commHandler);
                btnLogin.setVisibility(Button.INVISIBLE);
                btnCreateAccount.setVisibility(Button.INVISIBLE);
                txtSite.setText("");
            } catch (Exception e) {
                txtSite.setText(e.getMessage());
                e.printStackTrace();
            }
        }).start());

        btnLogin.setOnClickListener(v -> new Thread(() -> {
            try {
                postLogin(commHandler);
                btnLogin.setVisibility(Button.INVISIBLE);
                btnCreateAccount.setVisibility(Button.INVISIBLE);
                txtSite.setText("");
            } catch (Exception e) {
                txtSite.setText(e.getMessage());
                e.printStackTrace();
            }
        }).start());
    }
}
