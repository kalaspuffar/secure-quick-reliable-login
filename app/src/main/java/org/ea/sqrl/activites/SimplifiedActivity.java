package org.ea.sqrl.activites;

import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.BioAuthenticationCallback;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.IdentitySelector;
import org.ea.sqrl.utils.SqrlApplication;
import org.ea.sqrl.utils.Utils;

import java.security.KeyStore;
import java.util.regex.Matcher;

import javax.crypto.Cipher;

/**
 *
 * @author Daniel Persson
 */
public class SimplifiedActivity extends LoginBaseActivity {
    private static final String TAG = "SimplifiedActivity";

    public static final String ACTION_QUICK_SCAN = "org.ea.sqrl.activites.QUICK_SCAN";
    public static final String ACTION_LOGON = "org.ea.sqrl.activites.LOGON";

    private IdentitySelector mIdentitySelector = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simplified);

        rootView = findViewById(R.id.simplifiedActivityView);
        communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

        setupLoginPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());
        setupBasePopups(getLayoutInflater(), false);

        mIdentitySelector = new IdentitySelector(this, true,
                false, false, true, true);
        mIdentitySelector.registerLayout(findViewById(R.id.identitySelector));

        findViewById(R.id.btnUseIdentity).setOnClickListener(v -> initiateScan());
        findViewById(R.id.txtScanQrCode).setOnClickListener(v -> initiateScan());

        if (ACTION_QUICK_SCAN.equals(getIntent().getAction())) {
            handler.postDelayed(() -> initiateScan(), 100L);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        if(!mDbHelper.hasIdentities()) {
            startActivity(new Intent(this, StartActivity.class));
        } else {
            long currentId = SqrlApplication.getCurrentId(this.getApplication());
            if(currentId != 0) {
                SqrlApplication.setCurrentId(this, currentId);
                mIdentitySelector.update();
            }

            setupBasePopups(getLayoutInflater(), false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("SimplifiedActivity", "Cancelled scan");
                Snackbar.make(rootView, R.string.scan_cancel, Snackbar.LENGTH_LONG).show();
                if(!mDbHelper.hasIdentities()) {
                    startActivity(new Intent(this, StartActivity.class));
                }
            } else {
                final String serverData = Utils.readSQRLQRCodeAsString(data);
                communicationFlowHandler.setServerData(serverData);
                communicationFlowHandler.setUseSSL(serverData.startsWith("sqrl://"));

                Matcher sqrlMatcher = CommunicationHandler.sqrlPattern.matcher(serverData);
                if(!sqrlMatcher.matches()) {
                    showErrorMessage(R.string.scan_incorrect);
                    return;
                }

                final String domain = sqrlMatcher.group(1);
                final String queryLink = sqrlMatcher.group(2);

                try {
                    communicationFlowHandler.setQueryLink(queryLink);
                    communicationFlowHandler.setDomain(domain, queryLink);
                } catch (Exception e) {
                    showErrorMessage(e.getMessage());
                    Log.e(TAG, e.getMessage(), e);
                    return;
                }

                handler.postDelayed(() -> {
                    final TextView txtSite = loginPopupWindow.getContentView().findViewById(R.id.txtSite);
                    txtSite.setText(domain);

                    SQRLStorage storage = SQRLStorage.getInstance(SimplifiedActivity.this.getApplicationContext());
                    final TextView txtLoginPassword = loginPopupWindow.getContentView().findViewById(R.id.txtLoginPassword);
                    if(storage.hasQuickPass()) {
                        txtLoginPassword.setHint(getString(R.string.login_identity_quickpass, "" + storage.getHintLength()));
                    } else {
                        txtLoginPassword.setHint(R.string.login_identity_password);
                    }

                    showLoginPopup();

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && storage.hasBiometric()) {

                        BioAuthenticationCallback biometricCallback =
                                new BioAuthenticationCallback(SimplifiedActivity.this.getApplicationContext(), () -> {
                                    handler.post(() -> {
                                        hideLoginPopup();
                                        showProgressPopup();
                                    });
                                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);

                                    communicationFlowHandler.setDoneAction(() -> {
                                        storage.clear();
                                        handler.post(() -> {
                                            hideProgressPopup();
                                            closeActivity();
                                        });
                                    });

                                    communicationFlowHandler.setErrorAction(() -> {
                                        storage.clear();
                                        handler.post(() -> hideProgressPopup());
                                    });

                                    communicationFlowHandler.handleNextAction();
                                });

                        BiometricPrompt bioPrompt = new BiometricPrompt.Builder(this)
                                .setTitle(getString(R.string.login_title))
                                .setSubtitle(domain)
                                .setDescription(getString(R.string.login_verify_domain_text))
                                .setNegativeButton(
                                    getString(R.string.button_cps_cancel),
                                    this.getMainExecutor(),
                                    (dialogInterface, i) -> {}
                                ).build();

                        CancellationSignal cancelSign = new CancellationSignal();
                        cancelSign.setOnCancelListener(() -> {});

                        try {
                            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                            keyStore.load(null);
                            KeyStore.Entry entry = keyStore.getEntry("quickPass", null);
                            Cipher decCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING"); //or try with "RSA"
                            decCipher.init(Cipher.DECRYPT_MODE, ((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
                            bioPrompt.authenticate(new BiometricPrompt.CryptoObject(decCipher), cancelSign, this.getMainExecutor(), biometricCallback);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }, 100);
            }
        }
    }

    private void initiateScan() {
        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.setPrompt(this.getString(R.string.scan_site_code));
        integrator.initiateScan();
    }
}
