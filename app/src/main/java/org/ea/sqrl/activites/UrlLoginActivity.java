package org.ea.sqrl.activites;

import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
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
    public static final String EXTRA_QUICK_SCAN = "quick_scan";
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
        txtLoginPassword = findViewById(R.id.txtLoginPassword);
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

        setupBasePopups(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        SQRLStorage storage = SQRLStorage.getInstance(UrlLoginActivity.this.getApplicationContext());

        if(storage.hasQuickPass()) {
            txtLoginPassword.setHint(getString(R.string.login_identity_quickpass, "" + storage.getHintLength()));
        } else {
            txtLoginPassword.setHint(R.string.login_identity_password);
        }

        txtLoginPassword.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_DONE){
                doLogin(false, useCps, true);
                return true;
            }
            return false;
        });

        txtLoginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence password, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence password, int start, int before, int count) {
                if (!storage.hasQuickPass()) return;
                if ((start + count) >= storage.getHintLength()) {
                    doLogin(true, useCps, true);
                }
            }
        });

        findViewById(R.id.btnLoginOptions).setOnClickListener(v -> {
            UrlLoginActivity.this.finish();
            startActivity(new Intent(this, AccountOptionsActivity.class));
        });

        findViewById(R.id.btnLogin).setOnClickListener(v ->
                doLogin(false, useCps, true));

        if(storage.hasBiometric() && !ACTION_QUICKPASS_OPERATION.equals(intent.getAction())) {
            doLoginBiometric();
        }

        configureIdentitySelector(storage);
        setupAdvancedFunctions();
        setupHelp();
    }

    private void setupHelp() {
        findViewById(R.id.imgAlternateIdHelp).setOnClickListener(v ->
                showInfoMessage(R.string.button_alternative_identity, R.string.helptext_alterate_id));
        findViewById(R.id.imgDisableAccountHelp).setOnClickListener(v ->
                showInfoMessage(R.string.button_lock_account, R.string.helptext_disable_account));
        findViewById(R.id.imgEnableAccountHelp).setOnClickListener(v ->
                showInfoMessage(R.string.button_unlock_account, R.string.helptext_enable_account));
        findViewById(R.id.imgRemoveAccountHelp).setOnClickListener(v ->
                showInfoMessage(R.string.button_remove_account, R.string.helptext_remove_account));
    }

    private void setupAdvancedFunctions() {
        final ConstraintLayout advancedFunctionsLayout = findViewById(R.id.advancedFunctionsLayout);
        final ImageView imgAdvancedFunctionsToggle = findViewById(R.id.imgAdvancedFunctionsToggle);
        final TextView txtAdvancedFunctions = findViewById(R.id.txtAdvancedFunctions);
        final RadioGroup radgrpAccountOptions = findViewById(R.id.radgrpAccountOptions);

        radgrpAccountOptions.setOnCheckedChangeListener((group, checkedId) -> {
            Button btnLogin = findViewById(R.id.btnLogin);

            switch (checkedId) {
                case R.id.radDisableAccount:
                    btnLogin.setText(R.string.button_lock_account);
                    break;
                case R.id.radEnableAccount:
                    btnLogin.setText(R.string.button_unlock_account);
                    break;
                case R.id.radRemoveAccount:
                    btnLogin.setText(R.string.button_remove_account);
                    break;
                case R.id.radStandardLogin:
                default:
                    btnLogin.setText(R.string.button_login);
                    break;
            }
        });

        imgAdvancedFunctionsToggle.setOnClickListener(toggleAdvancedFunctionsListener);
        txtAdvancedFunctions.setOnClickListener(toggleAdvancedFunctionsListener);

        advancedFunctionsLayout.setVisibility(View.GONE);
    }

    private View.OnClickListener toggleAdvancedFunctionsListener = (v) -> {
        final ConstraintLayout advancedFunctionsLayout = findViewById(R.id.advancedFunctionsLayout);
        final ImageView imgAdvancedFunctionsToggle = findViewById(R.id.imgAdvancedFunctionsToggle);

        if (advancedFunctionsLayout.getVisibility() == View.GONE) {
            advancedFunctionsLayout.setVisibility(View.VISIBLE);
            imgAdvancedFunctionsToggle.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_keyboard_arrow_up_gray_24dp));
        } else {
            advancedFunctionsLayout.setVisibility(View.GONE);
            imgAdvancedFunctionsToggle.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_keyboard_arrow_down_gray_24dp));
        }
    };

    @Override
    protected void closeActivity() {
        boolean quickScan = getIntent().getBooleanExtra(EXTRA_QUICK_SCAN, false);
        if (useCps || quickScan) {
            UrlLoginActivity.this.finishAffinity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                UrlLoginActivity.this.finishAndRemoveTask();
            }
        } else {
            UrlLoginActivity.this.finish();
        }
        return;
    }

    @Override
    public void onBackPressed() {
        UrlLoginActivity.this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        if(!mDbHelper.hasIdentities()) {
            startActivity(new Intent(this, StartActivity.class));
        } else {
            setupBasePopups(getLayoutInflater());
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

    private void doLoginBiometric() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;

        BioAuthenticationCallback biometricCallback =
                new BioAuthenticationCallback(UrlLoginActivity.this.getApplicationContext(), () ->
                        doLogin(false, useCps, false)
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

    public void doLogin(boolean useQuickpass, boolean useCps, boolean needsDecryption) {

        handler.post(() -> {
            SQRLStorage storage = SQRLStorage.getInstance(this.getApplicationContext());

            long currentId = SqrlApplication.getCurrentId(this.getApplication());
            if(currentId <= 0) return;

            String alternateId = ((TextView)findViewById(R.id.txtAlternateId)).getText().toString();
            if (!alternateId.equals("")) {
                communicationFlowHandler.setAlternativeId(alternateId);
            }

            communicationFlowHandler.setUrlBasedLogin(useCps);

            showProgressPopup();
            closeKeyboard();

            new Thread(() -> {
                if (needsDecryption) {
                    boolean decryptionOk = storage.decryptIdentityKey(txtLoginPassword.getText().toString(), entropyHarvester, useQuickpass);
                    if(!decryptionOk) {
                        showErrorMessage(R.string.decrypt_identity_fail);
                        handler.post(() -> {
                            txtLoginPassword.setHint(R.string.login_identity_password);
                            txtLoginPassword.setText("");
                            hideProgressPopup();
                        });
                        storage.clear();
                        storage.clearQuickPass();
                        return;
                    }
                }

                clearQuickPassAfterTimeout();

                handler.post(() -> txtLoginPassword.setText(""));

                if (this instanceof EnableQuickPassActivity) {
                    storage.clear();
                    handler.post(() -> {
                        hideProgressPopup();
                        closeActivity();
                        EnableQuickPassActivity enableQuickPassActivity = (EnableQuickPassActivity)this;
                        enableQuickPassActivity.finish();
                    });
                    return;
                }

                if (useCps) {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN_CPS);
                } else {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);
                }

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
            }).start();
        }) ;
    }
}
