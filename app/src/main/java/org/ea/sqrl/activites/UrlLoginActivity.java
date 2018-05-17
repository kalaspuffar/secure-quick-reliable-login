package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
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
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 *
 * @author Daniel Persson
 */
public class UrlLoginActivity extends LoginBaseActivity {
    private static final String TAG = "UrlLoginActivity";
    private ServerSocket server;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_login);

        cboxIdentity = findViewById(R.id.cboxIdentity);

        rootView = findViewById(R.id.urlLoginActivityView);

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
                    progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

                    new Thread(() -> {
                        boolean decryptionOk = storage.decryptIdentityKey(password.toString(), entropyHarvester, true);
                        if(!decryptionOk) {
                            Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            handler.post(() -> {
                                txtLoginPassword.setText("");
                                progressPopupWindow.dismiss();
                            });
                            storage.clear();
                            storage.clearQuickPass(UrlLoginActivity.this);
                            return;
                        }

                        try {
                            postQuery(commHandler, false, false);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                            storage.clear();
                            storage.clearQuickPass(UrlLoginActivity.this);
                            handler.post(() -> {
                                Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                            handler.postDelayed(() -> closeActivity(), 5000);
                            return;
                        } finally {
                            handler.post(() -> {
                                txtLoginPassword.setText("");
                                progressPopupWindow.dismiss();
                            });
                        }
                        commHandler.setAskAction(() -> {
                            handler.post(() -> {
                                progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                            });
                            try {
                                if(commHandler.isIdentityKnown(false)) {
                                    postLogin(commHandler);
                                    if(!commHandler.hasErrorMessage()) {
                                        handler.post(() -> closeActivity());
                                    }
                                } else if(!commHandler.isIdentityKnown(false)) {
                                    postCreateAccount(commHandler);
                                    if (!commHandler.hasErrorMessage()) {
                                        handler.post(() -> closeActivity());
                                    }
                                } else {
                                    handler.post(() -> {
                                        txtLoginPassword.setText("");
                                    });
                                    toastErrorMessage(true);
                                    storage.clear();
                                    handler.postDelayed(() -> closeActivity(), 5000);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage(), e);
                                handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                                handler.postDelayed(() -> closeActivity(), 5000);
                            } finally {
                                commHandler.clearLastResponse();
                                storage.clear();
                                handler.post(() -> {
                                    txtLoginPassword.setText("");
                                    progressPopupWindow.dismiss();
                                });
                            }
                        });
                        commHandler.showAskDialog();
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
                    APPS_PREFERENCES,
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(CURRENT_ID, 0);

            if(currentId != 0) {
                progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

                new Thread(() -> {
                    boolean decryptionOk = storage.decryptIdentityKey(txtLoginPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptionOk) {
                        Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                        handler.post(() -> {
                            txtLoginPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        storage.clear();
                        storage.clearQuickPass(this);
                        return;
                    }
                    showClearNotification();

                    try {
                        postQuery(commHandler, false, false);
                    } catch (Exception e) {
                        handler.post(() -> {
                            Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
                        storage.clear();
                        storage.clearQuickPass(this);
                        Log.e(TAG, e.getMessage(), e);
                        handler.postDelayed(() -> closeActivity(), 5000);
                    } finally {
                        handler.post(() -> {
                            txtLoginPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                    }
                    commHandler.setAskAction(() -> {
                        try {
                            handler.post(() -> {
                                progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                            });

                            if(commHandler.isIdentityKnown(false)) {
                                postLogin(commHandler);
                                if (!commHandler.hasErrorMessage()) {
                                    handler.post(() -> closeActivity());
                                }
                            } else if(!commHandler.isIdentityKnown(false)) {
                                postCreateAccount(commHandler);
                                if (!commHandler.hasErrorMessage()) {
                                    handler.post(() -> closeActivity());
                                }
                            } else {
                                handler.post(() -> {
                                    txtLoginPassword.setText("");
                                });
                                toastErrorMessage(true);
                                storage.clear();
                                storage.clearQuickPass(this);
                                handler.postDelayed(() -> closeActivity(), 5000);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                            handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                            handler.postDelayed(() -> closeActivity(), 5000);
                            storage.clear();
                            storage.clearQuickPass(this);
                        } finally {
                            commHandler.clearLastResponse();
                            storage.clear();
                            handler.post(() -> {
                                txtLoginPassword.setText("");
                                progressPopupWindow.dismiss();
                            });
                        }
                    });
                    commHandler.showAskDialog();
                }).start();
            }
        });

        startCPSServer();
    }

    public void startCPSServer() {
        new Thread(() -> {
            try {
                server = new ServerSocket(25519);

                Log.i(TAG, "Started CPS server");

                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    OutputStream os = socket.getOutputStream();

                    StringBuilder sb = new StringBuilder();
                    String line;
                    while((line = in.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }

                    Log.i(TAG, sb.toString());

                    byte[] content = EncryptionUtils.decodeUrlSafe(
                        "R0lGODlhAQABAAAAACw="
                    );
                    if(sb.toString().contains("gif HTTP/1.1")) {
                        Log.i(TAG, "Respond");


                        StringBuilder out = new StringBuilder();
                        out.append("HTTP/1.0 200 OK\r\n");
                        out.append("Content-Type: image/gif\r\n");
                        out.append("Content-Length: " + content.length + "\r\n\r\n");

                        Log.i(TAG, out.toString());

                        os.write(EncryptionUtils.combine(out.toString().getBytes("UTF-8"), content));
                        os.flush();
                    }

                    in.close();
                    socket.close();
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (server != null) {
            try {
                server.close();
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
    protected void onResume() {
        super.onResume();

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        if(!mDbHelper.hasIdentities()) {
            UrlLoginActivity.this.finish();
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
}
