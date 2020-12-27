package org.ea.sqrl.activites;

import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.BioAuthenticationCallback;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.IdentitySelector;
import org.ea.sqrl.utils.RescueCodeInputHelper;
import org.ea.sqrl.utils.SqrlApplication;
import org.ea.sqrl.utils.Utils;

import java.security.KeyStore;
import java.util.regex.Matcher;

import javax.crypto.Cipher;

/**
 *
 * @author Daniel Persson
 */
public class LoginActivity extends LoginBaseActivity {
    private static final String TAG = "LoginActivity";
    public static final String EXTRA_USE_CPS = "use_cps";
    public static final String EXTRA_QUICK_SCAN = "quick_scan";
    public static final String ACTION_QUICKPASS_OPERATION = "org.ea.sqrl.activites.LOGON";

    private boolean useCps = true;
    private TextInputLayout pwdTextInputLayout;
    private EditText txtLoginPassword;
    private IdentitySelector mIdentitySelector = null;
    private Matcher mSqrlMatcher;
    private RescueCodeInputHelper mRescueCodeInputHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        rootView = findViewById(R.id.loginActivityView);
        pwdTextInputLayout = findViewById(R.id.txtLoginPasswordLayoutInternal);
        txtLoginPassword = findViewById(R.id.txtLoginPassword);
        communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

        final TextView txtSiteDomain = findViewById(R.id.txtSite);
        final LinearLayout rescueCodeLayout = findViewById(R.id.rescueCodeLayout);

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

            txtSiteDomain.setText(data.getHost());

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

        SQRLStorage storage = SQRLStorage.getInstance(LoginActivity.this.getApplicationContext());

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

        findViewById(R.id.btnLogin).setOnClickListener(v ->
                doLogin(false, useCps, true));

        if(storage.hasBiometric() && !ACTION_QUICKPASS_OPERATION.equals(intent.getAction())) {
            doLoginBiometric();
        }

        rescueCodeLayout.setVisibility(View.GONE);
        configureIdentitySelector(storage);
        setupAdvancedFunctions();
        setupHelp();
    }

    @Override
    protected void closeActivity() {
        boolean quickScan = getIntent().getBooleanExtra(EXTRA_QUICK_SCAN, false);
        if (useCps || quickScan) {
            LoginActivity.this.finishAffinity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LoginActivity.this.finishAndRemoveTask();
            }
        } else {
            LoginActivity.this.finish();
        }
    }

    @Override
    public void onBackPressed() {
        LoginActivity.this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        if(!mDbHelper.hasIdentities()) {
            startActivity(new Intent(this, WizardPage1Activity.class));
        } else {
            useCps = getIntent().getBooleanExtra(EXTRA_USE_CPS, true);
            setupBasePopups(getLayoutInflater());
            SQRLStorage storage = SQRLStorage.getInstance(LoginActivity.this.getApplicationContext());
            configureIdentitySelector(storage).update();
            setupAdvancedFunctions();
            setupHelp();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(progressPopupWindow.isShowing()) {
            hideProgressPopup();
        }
    }

    private void setupHelp() {
        SQRLStorage storage = SQRLStorage.getInstance(LoginActivity.this.getApplicationContext());

        findViewById(R.id.imgLoginPasswordHelp).setOnClickListener(v -> {
            if (storage.hasQuickPass()) {
                showInfoMessage(R.string.quickpass, R.string.helptext_quickpass_login);
            } else {
                showInfoMessage(R.string.introduction_password_title, R.string.helptext_password_login);
            }
        });

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
        final Button btnLogin = findViewById(R.id.btnLogin);

        mRescueCodeInputHelper = new RescueCodeInputHelper(LoginActivity.this,
                findViewById(R.id.loginActivityView), btnLogin, false);
        mRescueCodeInputHelper.setStatusChangedListener(successfullyCompleted ->
            btnLogin.setEnabled(successfullyCompleted)
        );

        radgrpAccountOptions.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radDisableAccount:
                    showPasswordLayout();
                    btnLogin.setEnabled(true);
                    btnLogin.setText(R.string.button_lock_account);
                    break;

                case R.id.radEnableAccount:
                    showRescueCodeLayout();
                    mRescueCodeInputHelper.clearForm();
                    btnLogin.setEnabled(false);
                    btnLogin.setText(R.string.button_unlock_account);
                    break;

                case R.id.radRemoveAccount:
                    showRescueCodeLayout();
                    mRescueCodeInputHelper.clearForm();
                    btnLogin.setEnabled(false);
                    btnLogin.setText(R.string.button_remove_account);
                    break;

                case R.id.radStandardLogin:
                default:
                    showPasswordLayout();
                    btnLogin.setEnabled(true);
                    btnLogin.setText(R.string.button_login);
                    break;
            }
        });

        imgAdvancedFunctionsToggle.setOnClickListener(toggleAdvancedFunctionsListener);
        txtAdvancedFunctions.setOnClickListener(toggleAdvancedFunctionsListener);

        advancedFunctionsLayout.setVisibility(View.GONE);
    }

    private void showPasswordLayout() {
        final LinearLayout rescueCodeLayout = findViewById(R.id.rescueCodeLayout);
        final ConstraintLayout passwordInputLayout = findViewById(R.id.txtLoginPasswordLayout);

        rescueCodeLayout.setVisibility(View.GONE);
        passwordInputLayout.setVisibility(View.VISIBLE);
    }

    private void showRescueCodeLayout() {
        final LinearLayout rescueCodeLayout = findViewById(R.id.rescueCodeLayout);
        final ConstraintLayout passwordInputLayout = findViewById(R.id.txtLoginPasswordLayout);

        rescueCodeLayout.setVisibility(View.VISIBLE);
        passwordInputLayout.setVisibility(View.GONE);

    }

    private View.OnClickListener toggleAdvancedFunctionsListener = (v) -> {
        final ConstraintLayout advancedFunctionsLayout = findViewById(R.id.advancedFunctionsLayout);
        final ImageView imgAdvancedFunctionsToggle = findViewById(R.id.imgAdvancedFunctionsToggle);
        final TextView txtLoginDescription = findViewById(R.id.txtLoginDescription);

        txtLoginDescription.setVisibility(View.GONE);

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
                new BioAuthenticationCallback(LoginActivity.this.getApplicationContext(), () ->
                        handler.post(() -> doLogin(false, useCps, false))
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
        final RadioGroup radgrpAccountOptions = findViewById(R.id.radgrpAccountOptions);
        SQRLStorage storage = SQRLStorage.getInstance(this.getApplicationContext());

        long currentId = SqrlApplication.getCurrentId(this.getApplication());
        if(currentId <= 0) return;

        Utils.reMaskPassword(pwdTextInputLayout);

        String alternateId = ((TextView)findViewById(R.id.txtAlternateId)).getText().toString();
        if (!alternateId.equals("")) {
            communicationFlowHandler.setAlternativeId(alternateId);
        }

        communicationFlowHandler.setUrlBasedLogin(useCps);

        showProgressPopup();
        closeKeyboard();

        new Thread(() -> {
            if (needsDecryption) {
                if (!decryptIdentityInternal(storage, useQuickpass)) {
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
                    finish();
                });
                return;
            }

            int checkedId = radgrpAccountOptions.getCheckedRadioButtonId();

            switch (checkedId) {
                case R.id.radDisableAccount:
                    configureCommFlowHandlerDisableAccount(storage);
                    break;
                case R.id.radEnableAccount:
                    configureCommFlowHandlerEnableAccount(storage);
                    break;
                case R.id.radRemoveAccount:
                    configureCommFlowHandlerRemoveAccount(storage);
                    break;
                case R.id.radStandardLogin:
                default:
                    configureCommFlowHandlerStandardLogin(storage);
                    break;
            }

            communicationFlowHandler.setErrorAction(() -> {
                storage.clear();
                handler.post(() -> hideProgressPopup());
            });

            communicationFlowHandler.handleNextAction();
        }).start();
    }

    private boolean decryptIdentityInternal(SQRLStorage storage, boolean useQuickPass) {
        final RadioGroup radgrpAccountOptions = findViewById(R.id.radgrpAccountOptions);

        if (radgrpAccountOptions.getCheckedRadioButtonId() == R.id.radEnableAccount ||
                radgrpAccountOptions.getCheckedRadioButtonId() == R.id.radRemoveAccount) {

            try {
                if (!storage.decryptUnlockKey(mRescueCodeInputHelper.getRescueCodeInput())) {
                    showErrorMessage(R.string.decrypt_identity_fail);
                    handler.post(() -> hideProgressPopup());
                    return false;
                }
                storage.reInitializeMasterKeyIdentity();
                return true;
            } catch (Exception e) {
                showErrorMessage(e.getMessage());
                Log.e(TAG, e.getMessage(), e);
                this.closeActivity();
                storage.clear();
                storage.clearQuickPass();
                return false;
            } finally {
                handler.post(() -> mRescueCodeInputHelper.clearForm());
            }
        } else {
            if(!storage.decryptIdentityKey(txtLoginPassword.getText().toString(), entropyHarvester, useQuickPass)) {
                showErrorMessage(R.string.decrypt_identity_fail);
                handler.post(() -> {
                    txtLoginPassword.setHint(R.string.login_identity_password);
                    txtLoginPassword.setText("");
                    hideProgressPopup();
                });
                storage.clear();
                storage.clearQuickPass();
                return false;
            }
            return true;
        }
    }

    private void configureCommFlowHandlerStandardLogin(SQRLStorage storage) {
        if (communicationFlowHandler.isUrlBasedLogin()) {
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
    }

    private void configureCommFlowHandlerDisableAccount(SQRLStorage storage) {
        if(communicationFlowHandler.isUrlBasedLogin()) {
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK);
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOCK_ACCOUNT_CPS);
        } else {
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOCK_ACCOUNT);
        }

        communicationFlowHandler.setDoneAction(() -> {
            storage.clear();
            storage.clearQuickPass();
            handler.post(() -> {
                hideProgressPopup();
                showInfoMessage(
                        R.string.disable_account_title,
                        R.string.disable_account_successful,
                        () -> closeActivity()
                );
            });
        });
    }

    private void configureCommFlowHandlerEnableAccount(SQRLStorage storage) {
        if(communicationFlowHandler.isUrlBasedLogin()) {
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITH_SUK);
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.UNLOCK_ACCOUNT_CPS);
        } else {
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITH_SUK_QRCODE);
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.UNLOCK_ACCOUNT);
        }

        communicationFlowHandler.setDoneAction(() -> {
            storage.clear();
            handler.post(() -> {
                hideProgressPopup();
                showInfoMessage(
                        R.string.enable_account_title,
                        R.string.enable_account_successful,
                        () -> closeActivity()
                );
            });
        });
    }

    private void configureCommFlowHandlerRemoveAccount(SQRLStorage storage) {
        if(communicationFlowHandler.isUrlBasedLogin()) {
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITH_SUK);
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOCK_ACCOUNT_CPS);
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.REMOVE_ACCOUNT_CPS);
        } else {
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITH_SUK_QRCODE);
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOCK_ACCOUNT);
            communicationFlowHandler.addAction(CommunicationFlowHandler.Action.REMOVE_ACCOUNT);
        }

        communicationFlowHandler.setDoneAction(() -> {
            storage.clear();
            handler.post(() -> {
                hideProgressPopup();
                showInfoMessage(
                        R.string.remove_account_title,
                        R.string.remove_account_successful,
                        () -> closeActivity()
                );
            });
        });
    }
}
