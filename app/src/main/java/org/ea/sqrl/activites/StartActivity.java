package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.Utils;

/**
 * Start activity should be a base for the user so we bring them into the application and they know
 * how to use it when installed and identities are added. So where we add some text for to inform
 * as well as a link to import your first identity.
 *
 * @author Daniel Persson
 */
public class StartActivity extends BaseActivity {
    private static final String TAG = "StartActivity";
    private IntentIntegrator integrator;
    private boolean createNewIdentity = false;
    private ConstraintLayout rootView;
    private Handler handler = new Handler();
    private PopupWindow importPopupWindow;
    private PopupWindow progressPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final TextView txtWelcomeMessage = findViewById(R.id.txtWelcomeMessage);
        txtWelcomeMessage.setMovementMethod(LinkMovementMethod.getInstance());

        rootView = findViewById(R.id.startActivityView);

        integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(this.getString(R.string.button_scan_secret));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        setupProgressPopupWindow(getLayoutInflater());
        setupImportPopupWindow(getLayoutInflater());
        setupProgressPopupWindow(getLayoutInflater());
        setupCameraAccessPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        final Button btnScanSecret = findViewById(R.id.btnScanSecret);
        btnScanSecret.setOnClickListener(v -> {
            createNewIdentity = false;
            this.showPhoneStatePermission();
        });

        final Button btnStartCreateIdentity = findViewById(R.id.btnStartCreateIdentity);
        btnStartCreateIdentity.setOnClickListener(v -> {
            createNewIdentity = true;
            this.showPhoneStatePermission();
        });

        final Button btnTextImport = findViewById(R.id.btnTextImport);
        btnTextImport.setOnClickListener(
            v -> startActivity(new Intent(this, TextImportActivity.class))
        );
    }

    protected void setupProgressPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_progress, null);

        progressPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);


        final ProgressBar progressBar = popupView.findViewById(R.id.pbEntropy);
        final TextView lblProgressTitle = popupView.findViewById(R.id.lblProgressTitle);
        final TextView lblProgressText = popupView.findViewById(R.id.lblProgressText);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, lblProgressTitle, progressBar, lblProgressText));
    }

    public void setupImportPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_import, null);

        importPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        importPopupWindow.setTouchable(true);

        final EditText txtPassword = popupView.findViewById(R.id.txtPassword);
        final Button btnImportIdentityDo = popupView.findViewById(R.id.btnImportIdentityDo);

        popupView.findViewById(R.id.btnCloseImportIdentity).setOnClickListener(v -> importPopupWindow.dismiss());
        popupView.findViewById(R.id.btnForgotPassword).setOnClickListener(v -> {
            importPopupWindow.dismiss();
            startActivity(new Intent(this, ResetPasswordActivity.class));
        });

        btnImportIdentityDo.setOnClickListener(v -> {
            importPopupWindow.dismiss();
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                SQRLStorage storage = SQRLStorage.getInstance();
                try {
                    boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptStatus) {
                        showErrorMessage(R.string.decrypt_identity_fail);
                        handler.post(() -> {
                            txtPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        return;
                    }
                    storage.clearQuickPass(this);

                    boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        showErrorMessage(R.string.encrypt_identity_fail);
                        handler.post(() -> {
                            txtPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        return;
                    }
                } catch (Exception e) {
                    showErrorMessage(e.getMessage());
                    Log.e(TAG, e.getMessage(), e);
                }

                storage.clear();

                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                mDbHelper.updateIdentityName(newIdentityId, "Default");

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        APPS_PREFERENCES,
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(CURRENT_ID, newIdentityId);
                editor.apply();

                handler.post(() -> {
                    txtPassword.setText("");
                    progressPopupWindow.dismiss();
                    startActivity(new Intent(this, SimplifiedActivity.class));
                });
            }).start();
        });
    }

    @Override
    protected void permissionOkCallback() {
        if(createNewIdentity) {
            startActivity(new Intent(this, CreateIdentityActivity.class));
        } else {
            integrator.initiateScan();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        StartActivity.this.finishAffinity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            StartActivity.this.finishAndRemoveTask();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d(TAG, "Cancelled scan");
                Snackbar.make(rootView, R.string.scan_cancel, Snackbar.LENGTH_LONG).show();
            } else {
                SQRLStorage storage = SQRLStorage.getInstance();

                try {
                    byte[] qrCodeData = Utils.readSQRLQRCode(data);
                    if(qrCodeData.length == 0) {
                        showErrorMessage(R.string.scan_incorrect);
                        return;
                    }

                    storage.read(qrCodeData);

                    if(!storage.hasEncryptedKeys()) {
                        handler.postDelayed(() -> startActivity(new Intent(this, ResetPasswordActivity.class)), 100);
                        return;
                    }

                    String recoveryBlock = storage.getVerifyingRecoveryBlock();

                    handler.postDelayed(() -> {
                        final TextView txtRecoveryKey = importPopupWindow.getContentView().findViewById(R.id.txtRecoveryKey);
                        txtRecoveryKey.setText(recoveryBlock);
                        txtRecoveryKey.setMovementMethod(LinkMovementMethod.getInstance());

                        importPopupWindow.showAtLocation(importPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    }, 100);
                } catch (Exception e) {
                    showErrorMessage(e.getMessage());
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
    }

 }
