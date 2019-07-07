package org.ea.sqrl.activites;

import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.account.AccountOptionsActivity;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.BioAuthenticationCallback;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.IdentitySelector;
import org.ea.sqrl.utils.SqrlApplication;

import java.security.KeyStore;
import java.util.regex.Matcher;

import javax.crypto.Cipher;

/**
 *
 * @author Daniel Persson
 */
public class UrlLoginActivity extends LoginBaseActivity {
    private static final String TAG = "UrlLoginActivity";
    public static final String EXTRA_USE_CPS = "use_cps";
    public static final String ACTION_QUICKPASS_OPERATION = "org.ea.sqrl.activites.LOGON";

    private boolean useCps = true;
    private EditText txtLoginPassword;
    private IdentitySelector mIdentitySelector = null;
    private Matcher mSqrlMatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_login);

        rootView = findViewById(R.id.urlLoginActivityView);
        communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

        final TextView txtUrlLogin = findViewById(R.id.txtSite);
        Intent intent = getIntent();

        boolean testing = intent.getBooleanExtra("RUNNING_TEST", false);
        if(testing) {
            return;
        }

        if (!ACTION_QUICKPASS_OPERATION.equals(intent.getAction())) {
            Uri data = intent.getData();
            if(data == null) {
                showErrorMessage(R.string.url_login_missing_url);
                return;
            }

            useCps = intent.getBooleanExtra(EXTRA_USE_CPS, true);

            txtUrlLogin.setText(data.getHost());

            final String serverData = data.toString();
            communicationFlowHandler.setServerData(serverData);
            communicationFlowHandler.setUseSSL(serverData.startsWith("sqrl://"));

            mSqrlMatcher = CommunicationHandler.sqrlPattern.matcher(serverData);
            if(!mSqrlMatcher.matches()) {
                showErrorMessage(R.string.scan_incorrect);
                return;
            }

            try {
                communicationFlowHandler.setQueryLink(mSqrlMatcher.group(2));
                communicationFlowHandler.setDomain(mSqrlMatcher.group(1), mSqrlMatcher.group(2));
            } catch (Exception e) {
                showErrorMessage(e.getMessage());
                Log.e(TAG, e.getMessage(), e);
                return;
            }

        }

        setupBasePopups(getLayoutInflater(), true);
        setupErrorPopupWindow(getLayoutInflater());

        SQRLStorage storage = SQRLStorage.getInstance(UrlLoginActivity.this.getApplicationContext());

        txtLoginPassword = findViewById(R.id.txtLoginPassword);
        if(storage.hasQuickPass()) {
            txtLoginPassword.setHint(getString(R.string.login_identity_quickpass, "" + storage.getHintLength()));
        } else {
            txtLoginPassword.setHint(R.string.login_identity_password);
        }

        txtLoginPassword.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_DONE){
                doLogin(storage, txtLoginPassword, false, useCps, true,null, UrlLoginActivity.this);
                return true;
            }
            return false;
        });

        txtLoginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence password, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence password, int start, int before, int count) {
                if (!storage.hasQuickPass()) return;
                if ((start + count) >= storage.getHintLength()) {
                    doLogin(storage, txtLoginPassword, true, useCps, true, null, UrlLoginActivity.this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnLoginOptions).setOnClickListener(v -> {
            UrlLoginActivity.this.finish();
            startActivity(new Intent(this, AccountOptionsActivity.class));
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            long currentId = SqrlApplication.getCurrentId(this.getApplication());

            if(currentId != 0) {
                doLogin(storage, txtLoginPassword, false, useCps, true, null,this);
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && storage.hasBiometric() && !ACTION_QUICKPASS_OPERATION.equals(intent.getAction())) {
            BioAuthenticationCallback biometricCallback =
                    new BioAuthenticationCallback(UrlLoginActivity.this.getApplicationContext(), () ->
                        doLogin(storage, txtLoginPassword, false, useCps, false, UrlLoginActivity.this, UrlLoginActivity.this)
                    );

            BiometricPrompt bioPrompt = new BiometricPrompt.Builder(this)
                    .setTitle(getString(R.string.login_title))
                    .setSubtitle(mSqrlMatcher.group(1))
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
        configureIdentitySelector(storage);
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
            setupBasePopups(getLayoutInflater(), true);
            SQRLStorage storage = SQRLStorage.getInstance(UrlLoginActivity.this.getApplicationContext());
            configureIdentitySelector(storage).update();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(progressPopupWindow.isShowing()) {
            hideProgressPopup();
        }
    }

    private IdentitySelector configureIdentitySelector(SQRLStorage storage) {
        if (mIdentitySelector == null) {
            mIdentitySelector = new IdentitySelector(this, true,false, true);
            mIdentitySelector.registerLayout(findViewById(R.id.identitySelector));
            mIdentitySelector.setIdentityChangedListener((identityIndex, identityName) -> {
                if (storage.hasQuickPass()) {
                    txtLoginPassword.setHint(getString(R.string.login_identity_quickpass, "" + storage.getHintLength()));
                } else {
                    txtLoginPassword.setHint(R.string.login_identity_password);
                }
            });
        }
        return mIdentitySelector;
    }
}
