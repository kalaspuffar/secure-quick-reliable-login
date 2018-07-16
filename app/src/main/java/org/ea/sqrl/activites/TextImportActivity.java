package org.ea.sqrl.activites;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.text.Editable;
import android.text.TextWatcher;
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

import org.ea.sqrl.R;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;
import org.ea.sqrl.utils.Utils;

public class TextImportActivity extends BaseActivity {
    private static final String TAG = "TextImportActivity";

    private Handler handler = new Handler();

    private PopupWindow progressPopupWindow;
    private PopupWindow resetPasswordPopupWindow;

    private ConstraintLayout rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_import);

        rootView = findViewById(R.id.textImportActivityView);

        setupProgressPopupWindow(getLayoutInflater());
        setupResetPasswordPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        final EditText txtTextIdentityInput = findViewById(R.id.txtTextIdentityInput);
        txtTextIdentityInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence textIdentity, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence textIdentity, int start, int before, int count) {
                String cleanTextIdentity = textIdentity.toString().replaceAll("[^2-9a-zA-Z]+", "");
                if(cleanTextIdentity.length() % 20 == 0) {
                    if(EncryptionUtils.validateBase56(cleanTextIdentity)) {
                        txtTextIdentityInput.setError(null);
                    } else {
                        txtTextIdentityInput.setError(getString(R.string.text_input_incorrect));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final Button btnImportIdentityDo = findViewById(R.id.btnImportIdentityDo);

        findViewById(R.id.btnTextImportClose).setOnClickListener(v -> this.finish());

        btnImportIdentityDo.setOnClickListener(v -> {
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                SQRLStorage storage = SQRLStorage.getInstance();
                String textIdentity = txtTextIdentityInput.getText().toString();

                textIdentity = textIdentity.replaceAll("[^2-9a-zA-Z]", "");

                try {
                    byte[] identityData = EncryptionUtils.decodeBase56(textIdentity);
                    identityData = EncryptionUtils.combine(SQRLStorage.STORAGE_HEADER.getBytes(), identityData);
                    storage.read(identityData);

                    handler.post(() -> {
                        resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        int line = Utils.getInteger(e.getMessage());
                        if(line > 0) {
                            txtTextIdentityInput.setError(getString(R.string.text_input_incorrect_on_line) + line);
                        } else {
                            showErrorMessage(e.getMessage());
                        }
                        progressPopupWindow.dismiss();
                    });
                    Log.e(TAG, e.getMessage(), e);
                }
            }).start();
        });
    }

    public void setupResetPasswordPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_reset_password, null);

        resetPasswordPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        resetPasswordPopupWindow.setTouchable(true);

        final EditText txtRecoverCode1 = popupView.findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = popupView.findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = popupView.findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = popupView.findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = popupView.findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = popupView.findViewById(R.id.txtRecoverCode6);
        final EditText txtResetPasswordNewPassword = popupView.findViewById(R.id.txtResetPasswordNewPassword);

        popupView.findViewById(R.id.btnCloseResetPassword).setOnClickListener(v -> resetPasswordPopupWindow.dismiss());
        popupView.findViewById(R.id.btnResetPassword).setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance();

            if(!checkRescueCode(txtRecoverCode1)) return;
            if(!checkRescueCode(txtRecoverCode2)) return;
            if(!checkRescueCode(txtRecoverCode3)) return;
            if(!checkRescueCode(txtRecoverCode4)) return;
            if(!checkRescueCode(txtRecoverCode5)) return;
            if(!checkRescueCode(txtRecoverCode6)) return;

            resetPasswordPopupWindow.dismiss();
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                try {
                    String rescueCode = txtRecoverCode1.getText().toString();
                    rescueCode += txtRecoverCode2.getText().toString();
                    rescueCode += txtRecoverCode3.getText().toString();
                    rescueCode += txtRecoverCode4.getText().toString();
                    rescueCode += txtRecoverCode5.getText().toString();
                    rescueCode += txtRecoverCode6.getText().toString();

                    boolean decryptionOk = storage.decryptUnlockKey(rescueCode);
                    if (!decryptionOk) {
                        showErrorMessage(R.string.decrypt_identity_fail);
                        return;
                    }

                    storage.reInitializeMasterKeyIdentity();

                    boolean encryptStatus = storage.encryptIdentityKey(txtResetPasswordNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        showErrorMessage(R.string.encrypt_identity_fail);
                        return;
                    }
                    storage.clear();

                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            APPS_PREFERENCES,
                            Context.MODE_PRIVATE
                    );

                    long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putLong(CURRENT_ID, newIdentityId);
                    editor.apply();

                    handler.post(() -> {
                        this.finish();
                    });
                } finally {
                    storage.clear();
                    handler.post(() -> {
                        progressPopupWindow.dismiss();
                        txtResetPasswordNewPassword.setText("");
                        txtRecoverCode1.setText("");
                        txtRecoverCode2.setText("");
                        txtRecoverCode3.setText("");
                        txtRecoverCode4.setText("");
                        txtRecoverCode5.setText("");
                        txtRecoverCode6.setText("");
                    });
                }
            }).start();
        });
    }

    protected boolean checkRescueCode(EditText code) {
        if(code.length() != 4) {
            showErrorMessage(R.string.rescue_code_incorrect_input);
            code.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(code.getText().toString());
        } catch (NumberFormatException nfe) {
            showErrorMessage(R.string.rescue_code_incorrect_input);
            code.requestFocus();
            return false;
        }
        return true;
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
}
