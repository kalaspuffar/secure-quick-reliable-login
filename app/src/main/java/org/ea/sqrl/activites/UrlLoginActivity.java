package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;

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

        progressBarHolder = findViewById(R.id.progressBarHolder);
        cboxIdentity = findViewById(R.id.cboxIdentity);

        rootView = findViewById(R.id.loginView);

        final TextView txtUrlLogin = findViewById(R.id.txtSite);
        Intent intent = getIntent();
        Uri data = intent.getData();
        if(data == null) {
            handler.post(() -> Snackbar.make(rootView, getString(R.string.url_login_missing_url), Snackbar.LENGTH_LONG).show());
            return;
        }
        txtUrlLogin.setText(data.getHost());

        this.serverData = data.toString();
        commHandler.setUseSSL(serverData.startsWith("sqrl://"));

        int indexOfQuery = serverData.indexOf("/", serverData.indexOf("://") + 3);
        queryLink = serverData.substring(indexOfQuery);
        final String domain = serverData.split("/")[2];
        try {
            commHandler.setDomain(domain);
        } catch (Exception e) {
            handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        setupLoginOptionsPopupWindow(getLayoutInflater(), false);
        setupBasePopups(getLayoutInflater(), false);

        SQRLStorage storage = SQRLStorage.getInstance();

        final EditText txtLoginPassword = findViewById(R.id.txtLoginPassword);
        txtLoginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence password, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence password, int start, int before, int count) {
                if (!storage.hasQuickPass()) return;
                if ((start + count) >= storage.getHintLength()) {
                    showProgressBar();

                    new Thread(() -> {
                        boolean decryptionOk = storage.decryptIdentityKeyQuickPass(password.toString());
                        if(!decryptionOk) {
                            Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            hideProgressBar();
                            storage.clear();
                            storage.clearQuickPass(UrlLoginActivity.this);
                            return;
                        }

                        try {
                            postQuery(commHandler, false);
                            if(
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                                            commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)
                                    ) {
                                postLogin(commHandler);
                            } else {
                                handler.post(() -> {
                                    txtLoginPassword.setText("");
                                });
                                toastErrorMessage(true);
                                storage.clear();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                            storage.clear();
                            handler.post(() -> {
                                Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                        } finally {
                            commHandler.clearLastResponse();
                            hideProgressBar();
                            handler.post(() -> {
                                txtLoginPassword.setText("");
                            });
                            closeActivity();
                        }
                    }).start();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnLoginOptions).setOnClickListener(v -> {
            loginOptionsPopupWindow.showAtLocation(loginOptionsPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    getString(R.string.preferences),
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(getString(R.string.current_id), 0);

            if(currentId != 0) {
                showProgressBar();

                new Thread(() -> {
                    boolean decryptionOk = storage.decryptIdentityKey(txtLoginPassword.getText().toString());
                    if(decryptionOk) {
                        storage.clearQuickPass(this);
                        boolean quickPassEncryptOk = storage.encryptIdentityKeyQuickPass(txtLoginPassword.getText().toString(), entropyHarvester);
                        if(quickPassEncryptOk) {
                            showClearNotification();
                        }
                    } else {
                        Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                        hideProgressBar();
                        handler.post(() -> {
                            txtLoginPassword.setText("");
                        });
                        return;
                    }

                    try {
                        postQuery(commHandler, false);

                        if(
                            (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                            commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                            !commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                        ) {
                            postLogin(commHandler);
                        } else if(
                            !commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) &&
                            !commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)
                        ){
                            postCreateAccount(commHandler);
                        } else {
                            handler.post(() -> {
                                txtLoginPassword.setText("");
                            });
                            toastErrorMessage(true);
                        }
                    } catch (Exception e) {
                        handler.post(() -> {
                            Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
                        Log.e(TAG, e.getMessage(), e);
                    } finally {
                        commHandler.clearLastResponse();
                        hideProgressBar();
                        handler.post(() -> {
                            txtLoginPassword.setText("");
                        });
                        closeActivity();
                    }
                }).start();
            }
        });
    }

    @Override
    protected void closeActivity() {
        super.closeActivity();
        handler.postDelayed(() -> UrlLoginActivity.this.finish(), 5000);
    }
}
