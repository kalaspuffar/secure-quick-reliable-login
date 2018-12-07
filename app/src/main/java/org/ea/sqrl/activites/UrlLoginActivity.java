package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.account.AccountOptionsActivity;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.BioAuthenticationCallback;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;

import java.security.KeyStore;
import java.util.regex.Matcher;

import javax.crypto.Cipher;

/**
 *
 * @author Daniel Persson
 */
public class UrlLoginActivity extends LoginBaseActivity {
    private static final String TAG = "UrlLoginActivity";
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_login);

        cboxIdentity = findViewById(R.id.cboxIdentity);

        rootView = findViewById(R.id.urlLoginActivityView);
        communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

        final TextView txtUrlLogin = findViewById(R.id.txtSite);
        Intent intent = getIntent();

        boolean testing = intent.getBooleanExtra("RUNNING_TEST", false);
        if(testing) {
            return;
        }

        Uri data = intent.getData();
        if(data == null) {
            showErrorMessage(R.string.url_login_missing_url);
            return;
        }
        txtUrlLogin.setText(data.getHost());

        final String serverData = data.toString();
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

        setupBasePopups(getLayoutInflater(), true);
        setupErrorPopupWindow(getLayoutInflater());

        SQRLStorage storage = SQRLStorage.getInstance();

        final EditText txtLoginPassword = findViewById(R.id.txtLoginPassword);
        if(storage.hasQuickPass()) {
            txtLoginPassword.setHint(getString(R.string.login_identity_quickpass, "" + storage.getHintLength()));
        } else {
            txtLoginPassword.setHint(R.string.login_identity_password);
        }

        txtLoginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence password, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence password, int start, int before, int count) {
                if (!storage.hasQuickPass()) return;
                if ((start + count) >= storage.getHintLength()) {
                    progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    closeKeyboard();

                    new Thread(() -> {
                        boolean decryptionOk = storage.decryptIdentityKey(password.toString(), entropyHarvester, true);
                        if(!decryptionOk) {
                            showErrorMessage(R.string.decrypt_identity_fail);
                            handler.post(() -> {
                                txtLoginPassword.setHint(R.string.login_identity_password);
                                txtLoginPassword.setText("");
                                progressPopupWindow.dismiss();
                            });
                            storage.clear();
                            storage.clearQuickPass(UrlLoginActivity.this);
                            return;
                        }

                        handler.post(() -> txtLoginPassword.setText(""));

                        communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK);
                        communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN_CPS);

                        communicationFlowHandler.setDoneAction(() -> {
                            storage.clear();
                            handler.post(() -> {
                                progressPopupWindow.dismiss();
                                closeActivity();
                            });
                        });

                        communicationFlowHandler.setErrorAction(() -> {
                            storage.clear();
                            storage.clearQuickPass(UrlLoginActivity.this);
                            handler.post(() -> progressPopupWindow.dismiss());
                        });

                        communicationFlowHandler.handleNextAction();

                    }).start();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnLoginOptions).setOnClickListener(v ->
            startActivity(new Intent(this, AccountOptionsActivity.class))
        );

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    APPS_PREFERENCES,
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(CURRENT_ID, 0);

            if(currentId != 0) {
                progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                closeKeyboard();

                new Thread(() -> {
                    boolean decryptionOk = storage.decryptIdentityKey(txtLoginPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptionOk) {
                        showErrorMessage(R.string.decrypt_identity_fail);
                        handler.post(() -> {
                            txtLoginPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        storage.clear();
                        storage.clearQuickPass(this);
                        return;
                    }
                    showClearNotification();

                    handler.post(() -> txtLoginPassword.setText(""));

                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN_CPS);

                    communicationFlowHandler.setDoneAction(() -> {
                        storage.clear();
                        handler.post(() -> {
                            progressPopupWindow.dismiss();
                            closeActivity();
                        });
                    });

                    communicationFlowHandler.setErrorAction(() -> {
                        storage.clear();
                        storage.clearQuickPass(UrlLoginActivity.this);
                        handler.post(() -> progressPopupWindow.dismiss());
                    });

                    communicationFlowHandler.handleNextAction();

                }).start();
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && storage.hasBiometric()) {
            BioAuthenticationCallback biometricCallback =
                    new BioAuthenticationCallback(handler, loginPopupWindow, () -> {
                        handler.post(() -> {
                            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                        });
                        communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK);
                        communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN_CPS);

                        communicationFlowHandler.setDoneAction(() -> {
                            storage.clear();
                            handler.post(() -> {
                                progressPopupWindow.dismiss();
                                closeActivity();
                            });
                        });

                        communicationFlowHandler.setErrorAction(() -> {
                            storage.clear();
                            storage.clearQuickPass(UrlLoginActivity.this);
                            handler.post(() -> progressPopupWindow.dismiss());
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
    }

    @Override
    protected void closeActivity() {
        UrlLoginActivity.this.finishAffinity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UrlLoginActivity.this.finishAndRemoveTask();
        }
    }

    @Override
    public void onBackPressed() {
        this.closeActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        if(!mDbHelper.hasIdentities()) {
            startActivity(new Intent(this, StartActivity.class));
        } else {
            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    APPS_PREFERENCES,
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(CURRENT_ID, 0);
            if(currentId != 0) {
                updateSpinnerData(currentId);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(progressPopupWindow.isShowing()) {
            progressPopupWindow.dismiss();
        }
    }
}
