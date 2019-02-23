package org.ea.sqrl.activites.identity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.SimplifiedActivity;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.Utils;

public class ImportActivity extends BaseActivity {
    private static final String TAG = "ImportActivity";

    private boolean firstIdentity = false;
    private ConstraintLayout rootView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        rootView = findViewById(R.id.importActivityView);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        final EditText txtPassword = findViewById(R.id.txtPassword);
        final Button btnImportIdentityDo = findViewById(R.id.btnImportIdentityDo);

        findViewById(R.id.btnForgotPassword).setOnClickListener(
                v -> {
                    ImportActivity.this.finish();
                    Intent resetPasswordIntent = new Intent(this, ResetPasswordActivity.class);
                    resetPasswordIntent.putExtra(SQRLStorage.NEW_IDENTITY, true);
                    startActivity(resetPasswordIntent);
                }
        );

        btnImportIdentityDo.setOnClickListener(v -> {
            showProgressPopup();

            new Thread(() -> {
                SQRLStorage storage = SQRLStorage.getInstance();
                try {
                    boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptStatus) {
                        handler.post(() -> {
                            showErrorMessage(R.string.decrypt_identity_fail);
                            txtPassword.setText("");
                            hideProgressPopup();
                        });
                        storage.clearQuickPass(this);
                        storage.clear();
                        return;
                    }
                    storage.clearQuickPass(this);

                    boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() -> {
                            showErrorMessage(R.string.encrypt_identity_fail);
                            txtPassword.setText("");
                            hideProgressPopup();
                        });
                        return;
                    }
                    storage.clear();
                } catch (Exception e) {
                    showErrorMessage(e.getMessage());
                    Log.e(TAG, e.getMessage(), e);
                }

                if(!mDbHelper.hasIdentities()) {
                    firstIdentity = true;
                }

                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        APPS_PREFERENCES,
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(CURRENT_ID, newIdentityId);
                editor.apply();

                handler.post(() -> {
                    txtPassword.setText("");
                    hideProgressPopup();

                    if(newIdentityId != 0) {
                        if(firstIdentity) {
                            mDbHelper.updateIdentityName(newIdentityId, "Default");
                            startActivity(new Intent(this, SimplifiedActivity.class));
                        } else {
                            startActivity(new Intent(this, RenameActivity.class));
                        }
                        ImportActivity.this.finish();
                    }
                });
            }).start();
        });

        boolean testing = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(testing) {
            return;
        }

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.setBarcodeImageEnabled(false);

        integrator.setPrompt(this.getString(R.string.scan_identity));
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Snackbar.make(rootView, R.string.scan_cancel, Snackbar.LENGTH_LONG).show();
                ImportActivity.this.finish();
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
                        final TextView txtRecoveryKey = findViewById(R.id.txtRecoveryKey);
                        txtRecoveryKey.setText(recoveryBlock);
                        txtRecoveryKey.setMovementMethod(LinkMovementMethod.getInstance());
                    }, 100);
                } catch (Exception e) {
                    if(e.getMessage().equals("Incorrect header")) {
                        showErrorMessage(R.string.scan_incorrect);
                    } else {
                        showErrorMessage(e.getMessage());
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }
        }
    }
}
