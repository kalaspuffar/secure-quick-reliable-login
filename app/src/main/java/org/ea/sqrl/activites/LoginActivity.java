package org.ea.sqrl.activites;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.ProgressionUpdater;
import org.ea.sqrl.R;
import org.ea.sqrl.storage.CommunicationHandler;
import org.ea.sqrl.storage.EncryptionUtils;
import org.ea.sqrl.storage.SQRLStorage;

import java.io.File;
import java.io.FileInputStream;

/**
 *
 * @author Daniel Persson
 */
public class LoginActivity extends BaseActivity {
    private final CommunicationHandler commHandler = CommunicationHandler.getInstance();
    private TextView txtSite;
    private Button btnCreateAccount;
    private Button btnLogin;
    private String serverData = null;
    private String queryLink = null;
    private Handler handler = new Handler();


    private void postQuery(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientQuery(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    private void postCreateAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientCreateAccount(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    private void postLogin(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientLogin(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtSite = findViewById(R.id.txtSite);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnLogin = findViewById(R.id.btnLogin);
        final Button btnScanNew = findViewById(R.id.btnScanNew);

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);

        btnScanNew.setOnClickListener(v -> new Thread(() -> {
            integrator.initiateScan();
        }).start());

        btnCreateAccount.setOnClickListener(v -> new Thread(() -> {
            try {
                postCreateAccount(commHandler);
                btnLogin.setVisibility(Button.INVISIBLE);
                btnCreateAccount.setVisibility(Button.INVISIBLE);
                txtSite.setText("");
            } catch (Exception e) {
                txtSite.setTextColor(Color.RED);
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
                txtSite.setTextColor(Color.RED);
                txtSite.setText(e.getMessage());
                e.printStackTrace();
            }
        }).start());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                serverData = EncryptionUtils.readSQRLQRCodeAsString(result.getRawBytes());
                int indexOfQuery = serverData.indexOf("/", serverData.indexOf("://")+3);
                queryLink = serverData.substring(indexOfQuery);
                final String domain = serverData.split("/")[2];
                commHandler.setDomain(domain);

                txtSite.setTextColor(Color.GRAY);
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
                        handler.post(() -> {
                            txtSite.setTextColor(Color.RED);
                            txtSite.setText(e.getMessage());
                        });
                        e.printStackTrace();
                        return;
                    }
                }).start();
            }
        }
    }
}
