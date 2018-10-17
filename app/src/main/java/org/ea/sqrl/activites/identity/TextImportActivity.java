package org.ea.sqrl.activites.identity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;
import org.ea.sqrl.utils.Utils;

public class TextImportActivity extends BaseActivity {
    private static final String TAG = "TextImportActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_import);

        setupProgressPopupWindow(getLayoutInflater());
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

                    handler.post(() -> startActivity(new Intent(this, ResetPasswordActivity.class)));
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
}
