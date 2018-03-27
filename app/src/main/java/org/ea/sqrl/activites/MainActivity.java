package org.ea.sqrl.activites;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.R;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;

import java.util.Map;

/**
 * This main activity is the hub of the application where the user lands for daily use. It should
 * make it easy to reach all the functions you use often and hide things that you don't need
 * regularly.
 *
 * @author Daniel Persson
 */
public class MainActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";

    private Handler handler = new Handler();
    private Spinner cboxIdentity;
    private Map<Long, String> identities;
    private PopupWindow renamePopupWindow;
    private PopupWindow decryptPopupWindow;
    private PopupWindow loginPopupWindow;
    private PopupWindow changePasswordPopupWindow;
    private PopupWindow resetPasswordPopupWindow;
    private PopupWindow progressPopupWindow;
    private PopupWindow loginOptionsPopupWindow;
    private PopupWindow disableAccountPopupWindow;
    private PopupWindow enableAccountPopupWindow;
    private PopupWindow removeAccountPopupWindow;

    private FrameLayout progressBarHolder;

    private Button btnUseIdentity;
    private EditText txtIdentityName;
    private ConstraintLayout mainView;
    private boolean importIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBarHolder = findViewById(R.id.progressBarHolder);

        cboxIdentity = findViewById(R.id.cboxIdentity);
        identities = mDbHelper.getIdentitys();

        mainView = findViewById(R.id.mainActivityView);

        ArrayAdapter adapter = new ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            identities.values().toArray(new String[identities.size()])
        );
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setOnItemSelectedListener(this);

        setupRenamePopupWindow(getLayoutInflater());
        setupLoginPopupWindow(getLayoutInflater());
        setupLoginOptionsPopupWindow(getLayoutInflater());
        setupImportPopupWindow(getLayoutInflater());
        setupChangePasswordPopupWindow(getLayoutInflater());
        setupResetPasswordPopupWindow(getLayoutInflater());
        setupProgressPopupWindow(getLayoutInflater());
        setupEnableAccountPopupWindow(getLayoutInflater());
        setupDisableAccountPopupWindow(getLayoutInflater());
        setupRemoveAccountPopupWindow(getLayoutInflater());

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(this.getString(R.string.button_scan_secret));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        Intent intent = getIntent();
        int startMode = intent.getIntExtra(START_USER_MODE, 0);
        if(startMode == START_USER_MODE_NEW_USER) {
            integrator.initiateScan();
            importIdentity = true;
        }

        btnUseIdentity = findViewById(R.id.btnUseIdentity);
        btnUseIdentity.setOnClickListener(
                v -> {
                    importIdentity = false;
                    integrator.initiateScan();
                }
        );

        final Button btnImportIdentity = findViewById(R.id.btnImportIdentity);
        btnImportIdentity.setOnClickListener(
                v -> {
                    importIdentity = true;
                    integrator.initiateScan();
                }
        );

        final Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class))
        );

        final Button btnCreate = findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(
                v -> {
                    showNotImplementedDialog();
                }
        );

        final Button btnRemove = findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(
                v -> {
                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                    );
                    long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                    if(currentId != 0) {
                        mDbHelper.deleteIdentity(currentId);
                        updateSpinnerData(currentId);
                        Snackbar.make(mainView, getString(R.string.main_identity_removed), Snackbar.LENGTH_LONG).show();

                        if(!mDbHelper.hasIdentities()) {
                            startActivity(new Intent(this, StartActivity.class));
                        }
                    }
                }
        );

        final Button btnRename = findViewById(R.id.btnRename);
        btnRename.setOnClickListener(
                v -> {
                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                    );
                    long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                    if(currentId != 0) {
                        txtIdentityName.setText(mDbHelper.getIdentityName(currentId));
                    }
                    renamePopupWindow.showAtLocation(renamePopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                }
        );

        final Button btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePassword.setOnClickListener(
                v -> changePasswordPopupWindow.showAtLocation(changePasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(
                v -> new Thread(() -> {
                    startActivity(new Intent(this, ShowIdentityActivity.class));
                }).start()
        );

        final Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(
                v -> resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnForgetQuickPass = findViewById(R.id.btnForgetQuickPass);
        btnForgetQuickPass.setOnClickListener(
                v -> showNotImplementedDialog()
        );

        final Button btnRekey = findViewById(R.id.btnRekey);
        btnRekey.setOnClickListener(
                v -> showNotImplementedDialog()
        );
    }

    public void setupRenamePopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_rename, null);

        renamePopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        renamePopupWindow.setTouchable(true);
        txtIdentityName = popupView.findViewById(R.id.txtIdentityName);

        popupView.findViewById(R.id.btnCloseRename).setOnClickListener(v -> renamePopupWindow.dismiss());
        popupView.findViewById(R.id.btnRename).setOnClickListener(v -> {

                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                    );
                    long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                    if(currentId != 0) {
                        mDbHelper.updateIdentityName(currentId, txtIdentityName.getText().toString());
                        updateSpinnerData(currentId);
                    }
                    txtIdentityName.setText("");
                    renamePopupWindow.dismiss();
                });
    }

    private void showProgressBar() {
        handler.post(() -> {
            mainView.setEnabled(false);
            AlphaAnimation inAnimation = new AlphaAnimation(0f, 1f);
            inAnimation.setDuration(200);
            progressBarHolder.setAnimation(inAnimation);
            progressBarHolder.setVisibility(View.VISIBLE);
        });
    }

    private void hideProgressBar() {
        handler.post(() -> {
            mainView.setEnabled(true);
            AlphaAnimation outAnimation = new AlphaAnimation(1f, 0f);
            outAnimation.setDuration(200);
            progressBarHolder.setAnimation(outAnimation);
            progressBarHolder.setVisibility(View.GONE);
        });
    }

    private final CommunicationHandler commHandler = CommunicationHandler.getInstance();
    private String serverData = null;
    private String queryLink = null;

    private void toastErrorMessage() {
        if(commHandler.hasErrorMessage()) {
            handler.post(() ->
                    Snackbar.make(mainView, commHandler.getErrorMessage(this), Snackbar.LENGTH_LONG).show()
            );
        }
    }

    private void postQuery(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientQuery(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    private void postCreateAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(
                commHandler.createClientCreateAccount(entropyHarvester),
                serverData
        );
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    private void postLogin(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientLogin(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    private void postDisableAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientDisable(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    private void postEnableAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientEnable(), serverData, true);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    private void postRemoveAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientRemove(), serverData, true);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    private boolean checkRescueCode(EditText code) {
        if(code.length() != 4) {
            Snackbar.make(mainView, getString(R.string.rescue_code_incorrect_input), Snackbar.LENGTH_LONG).show();
            code.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(code.getText().toString());
        } catch (NumberFormatException nfe) {
            Snackbar.make(mainView, getString(R.string.rescue_code_incorrect_input), Snackbar.LENGTH_LONG).show();
            code.requestFocus();
            return false;
        }
        return true;
    }

    public void setupProgressPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_progress, null);

        progressPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);

        final ProgressBar progressBar = popupView.findViewById(R.id.pbProgress);
        final TextView lblProgressText = popupView.findViewById(R.id.lblProgressText);
        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, progressBar, lblProgressText));
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
                        handler.post(() ->
                            Snackbar.make(mainView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show()
                        );
                        return;
                    }

                    storage.reInitializeMasterKeyIdentity();

                    boolean encryptStatus = storage.encryptIdentityKey(txtResetPasswordNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() ->
                            Snackbar.make(mainView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show()
                        );
                        return;
                    }
                    storage.clear();

                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                    );

                    if(importIdentity) {
                        long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putLong(getString(R.string.current_id), newIdentityId);
                        editor.commit();

                        handler.post(() -> {
                            updateSpinnerData(newIdentityId);
                            decryptPopupWindow.dismiss();

                            if(newIdentityId != 0) {
                                txtIdentityName.setText(mDbHelper.getIdentityName(newIdentityId));
                                renamePopupWindow.showAtLocation(renamePopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                            }
                        });
                    } else {
                        long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                        if(currentId != 0) {
                            mDbHelper.updateIdentityData(currentId, storage.createSaveData());
                        }
                    }
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

    public void setupLoginPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_login, null);

        loginPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        loginPopupWindow.setTouchable(true);
        final EditText txtLoginPassword = popupView.findViewById(R.id.txtDisablePassword);

        popupView.findViewById(R.id.btnCloseLogin).setOnClickListener(v -> loginPopupWindow.dismiss());
        popupView.findViewById(R.id.btnLoginOptions).setOnClickListener(v -> {
            loginPopupWindow.dismiss();
            loginOptionsPopupWindow.showAtLocation(loginOptionsPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });

        popupView.findViewById(R.id.btnDisableAccount).setOnClickListener(v -> {
            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    getString(R.string.preferences),
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(getString(R.string.current_id), 0);

            if(currentId != 0) {
                loginPopupWindow.dismiss();
                showProgressBar();

                new Thread(() -> {
                    boolean decryptionOk = SQRLStorage.getInstance().decryptIdentityKey(txtLoginPassword.getText().toString());
                    if(decryptionOk) {
                        showClearNotification();
                    } else {
                        Snackbar.make(mainView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                        hideProgressBar();
                        return;
                    }

                    try {
                        postQuery(commHandler);

                        if(
                            commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                            commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)
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
                            toastErrorMessage();
                        }
                    } catch (Exception e) {
                        handler.post(() -> Snackbar.make(mainView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                        Log.e(TAG, e.getMessage(), e);
                    } finally {
                        hideProgressBar();
                    }

                    handler.post(() -> {
                        txtLoginPassword.setText("");
                    });
                }).start();
            }
        });
    }

    public void setupLoginOptionsPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_login_optional, null);

        loginOptionsPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);
        loginOptionsPopupWindow.setTouchable(true);

        popupView.findViewById(R.id.btnCloseLoginOptional).setOnClickListener(v -> {
            loginOptionsPopupWindow.dismiss();
            loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });

        popupView.findViewById(R.id.btnRemoveAccount).setOnClickListener(v -> {
            loginOptionsPopupWindow.dismiss();
            removeAccountPopupWindow.showAtLocation(removeAccountPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });

        popupView.findViewById(R.id.btnLockAccount).setOnClickListener(v -> {
            loginOptionsPopupWindow.dismiss();
            disableAccountPopupWindow.showAtLocation(disableAccountPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });

        popupView.findViewById(R.id.btnUnlockAccount).setOnClickListener(v -> {
            loginOptionsPopupWindow.dismiss();
            enableAccountPopupWindow.showAtLocation(enableAccountPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });
    }

    public void setupDisableAccountPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_disable_account, null);

        disableAccountPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);
        disableAccountPopupWindow.setTouchable(true);

        popupView.findViewById(R.id.btnCloseLogin).setOnClickListener(v -> {
            disableAccountPopupWindow.dismiss();
            loginOptionsPopupWindow.showAtLocation(loginOptionsPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });


        final EditText txtDisablePassword = popupView.findViewById(R.id.txtDisablePassword);
        popupView.findViewById(R.id.btnDisableAccount).setOnClickListener(v -> {
            disableAccountPopupWindow.dismiss();
            showProgressBar();

            new Thread(() -> {
                boolean decryptionOk = SQRLStorage.getInstance().decryptIdentityKey(txtDisablePassword.getText().toString());
                if(decryptionOk) {
                    showClearNotification();
                } else {
                    Snackbar.make(mainView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                    return;
                }

                try {
                    postQuery(commHandler);

                    if(
                        (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                                commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                                !commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                        ) {
                        postDisableAccount(commHandler);
                    } else {
                        handler.post(() -> {
                            txtDisablePassword.setText("");
                        });
                        toastErrorMessage();
                    }
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(mainView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
                    return;
                } finally {
                    hideProgressBar();
                }

                handler.post(() -> {
                    txtDisablePassword.setText("");
                });
            }).start();
        });
    }

    public void setupEnableAccountPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_enable_account, null);

        enableAccountPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        enableAccountPopupWindow.setTouchable(true);

        final EditText txtRecoverCode1 = popupView.findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = popupView.findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = popupView.findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = popupView.findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = popupView.findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = popupView.findViewById(R.id.txtRecoverCode6);

        popupView.findViewById(R.id.btnCloseResetPassword).setOnClickListener(v -> enableAccountPopupWindow.dismiss());
        popupView.findViewById(R.id.btnEnableAccountEnable).setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance();

            if(!checkRescueCode(txtRecoverCode1)) return;
            if(!checkRescueCode(txtRecoverCode2)) return;
            if(!checkRescueCode(txtRecoverCode3)) return;
            if(!checkRescueCode(txtRecoverCode4)) return;
            if(!checkRescueCode(txtRecoverCode5)) return;
            if(!checkRescueCode(txtRecoverCode6)) return;

            handler.post(() -> {
                enableAccountPopupWindow.dismiss();
                progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
            });

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
                        handler.post(() ->
                                Snackbar.make(mainView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show()
                        );
                        return;
                    }
                    storage.reInitializeMasterKeyIdentity();

                    postQuery(commHandler);
                    if(
                            (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                            ) {
                        postEnableAccount(commHandler);
                    } else {
                        toastErrorMessage();
                    }
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(mainView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    storage.clear();
                    handler.post(() -> {
                        progressPopupWindow.dismiss();
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

    public void setupRemoveAccountPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_remove_account, null);

        removeAccountPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        removeAccountPopupWindow.setTouchable(true);

        final EditText txtRecoverCode1 = popupView.findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = popupView.findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = popupView.findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = popupView.findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = popupView.findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = popupView.findViewById(R.id.txtRecoverCode6);

        popupView.findViewById(R.id.btnCloseResetPassword).setOnClickListener(v -> removeAccountPopupWindow.dismiss());
        popupView.findViewById(R.id.btnRemoveAccountRemove).setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance();

            if(!checkRescueCode(txtRecoverCode1)) return;
            if(!checkRescueCode(txtRecoverCode2)) return;
            if(!checkRescueCode(txtRecoverCode3)) return;
            if(!checkRescueCode(txtRecoverCode4)) return;
            if(!checkRescueCode(txtRecoverCode5)) return;
            if(!checkRescueCode(txtRecoverCode6)) return;

            removeAccountPopupWindow.dismiss();
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
                        handler.post(() ->
                                Snackbar.make(mainView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show()
                        );
                        return;
                    }
                    storage.reInitializeMasterKeyIdentity();

                    postQuery(commHandler);
                    if(
                        (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                        commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                        commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                        ) {
                        postRemoveAccount(commHandler);
                    } else if(
                        (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                        commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                        !commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                        ) {
                        postDisableAccount(commHandler);
                        postRemoveAccount(commHandler);
                    } else {
                        toastErrorMessage();
                    }
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(mainView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    storage.clear();
                    handler.post(() -> {
                        progressPopupWindow.dismiss();
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


    public void setupImportPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_decrypt, null);

        decryptPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        decryptPopupWindow.setTouchable(true);

        final ProgressBar pbDecrypting = popupView.findViewById(R.id.pbDecrypting);
        final EditText txtPassword = popupView.findViewById(R.id.txtPassword);
        final Button btnDecryptKey = popupView.findViewById(R.id.btnDecryptKey);
        final TextView progressText = popupView.findViewById(R.id.lblProgressText);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, pbDecrypting, progressText));

        popupView.findViewById(R.id.btnCloseImportIdentity).setOnClickListener(v -> decryptPopupWindow.dismiss());
        btnDecryptKey.setOnClickListener(v -> {
            decryptPopupWindow.dismiss();
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                try {
                    boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString());
                    if(!decryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(mainView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtPassword.setText("");
                        });
                        return;
                    }

                    boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(mainView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtPassword.setText("");
                        });
                        return;
                    }
                    storage.clear();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }

                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        getString(R.string.preferences),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(getString(R.string.current_id), newIdentityId);
                editor.commit();

                handler.post(() -> {
                    updateSpinnerData(newIdentityId);
                    txtPassword.setText("");
                    progressPopupWindow.dismiss();

                    if(newIdentityId != 0) {
                        txtIdentityName.setText(mDbHelper.getIdentityName(newIdentityId));
                        renamePopupWindow.showAtLocation(renamePopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    }
                });
            }).start();
        });
    }

    public void setupChangePasswordPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_change_password, null);

        changePasswordPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        changePasswordPopupWindow.setTouchable(true);

        final EditText txtCurrentPassword = popupView.findViewById(R.id.txtCurrentPassword);
        final EditText txtNewPassword = popupView.findViewById(R.id.txtNewPassword);
        final EditText txtRetypePassword = popupView.findViewById(R.id.txtRetypePassword);

        SQRLStorage storage = SQRLStorage.getInstance();

        popupView.findViewById(R.id.btnCloseChangePassword).setOnClickListener(v -> changePasswordPopupWindow.dismiss());
        final Button btnChangePassword = popupView.findViewById(R.id.btnDoChangePassword);
        btnChangePassword.setOnClickListener(v -> {
            if(!txtNewPassword.getText().toString().equals(txtRetypePassword.getText().toString())) {
                Snackbar.make(mainView, getString(R.string.change_password_retyped_password_do_not_match), Snackbar.LENGTH_LONG).show();
                txtCurrentPassword.setText("");
                txtNewPassword.setText("");
                txtRetypePassword.setText("");
                return;
            }

            changePasswordPopupWindow.dismiss();
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                try {
                    boolean decryptStatus = storage.decryptIdentityKey(txtCurrentPassword.getText().toString());
                    if (!decryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(mainView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtCurrentPassword.setText("");
                            txtNewPassword.setText("");
                            txtRetypePassword.setText("");
                        });
                        return;
                    }

                    boolean encryptStatus = storage.encryptIdentityKey(txtNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(mainView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtCurrentPassword.setText("");
                            txtNewPassword.setText("");
                            txtRetypePassword.setText("");
                        });
                        return;
                    }
                } finally {
                    storage.clear();
                    handler.post(() -> progressPopupWindow.dismiss());
                }

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        getString(R.string.preferences),
                        Context.MODE_PRIVATE
                );
                long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                mDbHelper.updateIdentityData(currentId, storage.createSaveData());

                handler.post(() -> {
                    txtCurrentPassword.setText("");
                    txtNewPassword.setText("");
                    txtRetypePassword.setText("");
                });
            }).start();
        });
    }


    public void showClearNotification() {
        final String CHANNEL_ID = "sqrl_notify_01";

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "SQRL Notification Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableVibration(false);
            notificationChannel.enableLights(false);
            notificationChannel.setSound(null, null);

            notificationManager.createNotificationChannel(notificationChannel);
        }

        long[] v = {};
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_sqrl_logo_vector_outline)
                        .setContentTitle(getString(R.string.notification_identity_unlocked))
                        .setContentText(getString(R.string.notification_identity_unlocked_title))
                        .setAutoCancel(true)
                        .setVibrate(v)
                        .setSound(null)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(getString(R.string.notification_identity_unlocked_desc)));

        Intent resultIntent = new Intent(this, ClearIdentityActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ClearIdentityActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(NOTIFICATION_IDENTITY_UNLOCKED, mBuilder.build());
    }

    public void showNotImplementedDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(MainActivity.this);
        }
        builder.setTitle(R.string.not_implemented_title)
                .setMessage(getString(R.string.not_implemented_text))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Long[] keyArray = identities.keySet().toArray(new Long[identities.size()]);

        SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                getString(R.string.preferences),
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(getString(R.string.current_id), keyArray[pos]);
        editor.commit();

        SQRLStorage storage = SQRLStorage.getInstance();
        try {
            byte[] identityData = mDbHelper.getIdentityData(keyArray[pos]);
            storage.read(identityData);
            btnUseIdentity.setEnabled(storage.hasIdentityBlock());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private int getPosition(long currentId) {
        int i = 0;
        for(Long l : identities.keySet()) {
            if (l == currentId) return i;
            i++;
        }
        return 0;
    }

    private void updateSpinnerData(long currentId) {
        identities = mDbHelper.getIdentitys();

        ArrayAdapter adapter = new ArrayAdapter(
                this,
                R.layout.simple_spinner_item,
                identities.values().toArray(new String[identities.size()])
        );
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setSelection(getPosition(currentId));
    }

    @Override
    public void onBackPressed() {
        if (renamePopupWindow != null && renamePopupWindow.isShowing()) {
            renamePopupWindow.dismiss();
        } else if (decryptPopupWindow != null && decryptPopupWindow.isShowing()) {
            decryptPopupWindow.dismiss();
        } else if (loginPopupWindow != null && loginPopupWindow.isShowing()) {
            loginPopupWindow.dismiss();
        } else if (changePasswordPopupWindow != null && changePasswordPopupWindow.isShowing()) {
            changePasswordPopupWindow.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Snackbar.make(mainView, "Cancelled", Snackbar.LENGTH_LONG).show();
            } else {
                if(!importIdentity) {
                    serverData = EncryptionUtils.readSQRLQRCodeAsString(result.getRawBytes());
                    int indexOfQuery = serverData.indexOf("/", serverData.indexOf("://") + 3);
                    queryLink = serverData.substring(indexOfQuery);
                    final String domain = serverData.split("/")[2];
                    commHandler.setDomain(domain);

                    handler.postDelayed(() -> {
                        final TextView txtSite = loginPopupWindow.getContentView().findViewById(R.id.txtSite);
                        txtSite.setText(domain);
                        loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    }, 100);
                } else {
                    SQRLStorage storage = SQRLStorage.getInstance();
                    byte[] qrCodeData = EncryptionUtils.readSQRLQRCode(result.getRawBytes());
                    try {
                        storage.read(qrCodeData);

                        if(!storage.hasEncryptedKeys()) {
                            handler.postDelayed(() -> {
                                resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                            }, 100);
                            return;
                        }

                        handler.postDelayed(() -> {
                            final TextView txtRecoveryKey = decryptPopupWindow.getContentView().findViewById(R.id.txtRecoveryKey);
                            txtRecoveryKey.setText(storage.getVerifyingRecoveryBlock());
                            txtRecoveryKey.setMovementMethod(LinkMovementMethod.getInstance());

                            decryptPopupWindow.showAtLocation(decryptPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                        }, 100);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
