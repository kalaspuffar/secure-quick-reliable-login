package org.ea.sqrl.activites;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.services.IdentityPrintDocumentAdapter;
import org.ea.sqrl.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;

/**
 * This main activity is the hub of the application where the user lands for daily use. It should
 * make it easy to reach all the functions you use often and hide things that you don't need
 * regularly.
 *
 * @author Daniel Persson
 */
public class MainActivity extends LoginBaseActivity {
    private static final String TAG = "MainActivity";

    private PopupWindow renamePopupWindow;
    private PopupWindow importPopupWindow;
    private PopupWindow resetPasswordPopupWindow;
    private PopupWindow changePasswordPopupWindow;
    private PopupWindow exportOptionsPopupWindow;

    private EditText txtIdentityName;
    private int startMode = 0;
    private boolean importIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cboxIdentity = findViewById(R.id.cboxIdentity);
        rootView = findViewById(R.id.mainActivityView);

        setupRenamePopupWindow(getLayoutInflater());
        setupLoginPopupWindow(getLayoutInflater());
        setupImportPopupWindow(getLayoutInflater());
        setupResetPasswordPopupWindow(getLayoutInflater());
        setupChangePasswordPopupWindow(getLayoutInflater());
        setupExportOptionsPopupWindow(getLayoutInflater());
        setupLoginOptionsPopupWindow(getLayoutInflater(), true);

        setupBasePopups(getLayoutInflater(), false);

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        final ImageButton btnCloseMain = findViewById(R.id.btnCloseMain);
        btnCloseMain.setOnClickListener(v -> MainActivity.this.finish());
        
        btnUseIdentity = findViewById(R.id.btnUseIdentity);
        btnUseIdentity.setOnClickListener(
                v -> {
                    importIdentity = false;
                    integrator.setPrompt(this.getString(R.string.scan_site_code));
                    integrator.initiateScan();
                }
        );

        final Button btnImportIdentity = findViewById(R.id.btnImportIdentity);
        btnImportIdentity.setOnClickListener(
                v -> {
                    importIdentity = true;
                    integrator.setPrompt(this.getString(R.string.scan_identity));
                    integrator.initiateScan();
                }
        );

        final Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class))
        );

        final Button btnRemove = findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(
                v -> {
                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            APPS_PREFERENCES,
                            Context.MODE_PRIVATE
                    );
                    long currentId = sharedPref.getLong(CURRENT_ID, 0);
                    if(currentId != 0) {
                        mDbHelper.deleteIdentity(currentId);
                        updateSpinnerData(currentId);
                        Snackbar.make(rootView, getString(R.string.main_identity_removed), Snackbar.LENGTH_LONG).show();

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
                            APPS_PREFERENCES,
                            Context.MODE_PRIVATE
                    );
                    long currentId = sharedPref.getLong(CURRENT_ID, 0);
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
                v -> exportOptionsPopupWindow.showAtLocation(exportOptionsPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(
                v -> resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnTextImport = findViewById(R.id.btnTextImport);
        btnTextImport.setOnClickListener(
                v -> startActivity(new Intent(this, TextImportActivity.class))
        );


        final Button btnCreate = findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(v -> startActivity(new Intent(this, CreateIdentityActivity.class)));

        final Button btnRekey = findViewById(R.id.btnRekey);
        btnRekey.setOnClickListener(v -> startActivity(new Intent(this, RekeyIdentityActivity.class)));
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
                    APPS_PREFERENCES,
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(CURRENT_ID, 0);
            if(currentId != 0) {
                mDbHelper.updateIdentityName(currentId, txtIdentityName.getText().toString());
                updateSpinnerData(currentId);
            }
            txtIdentityName.setText("");
            renamePopupWindow.dismiss();
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
                        handler.post(() ->
                            Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show()
                        );
                        return;
                    }

                    storage.reInitializeMasterKeyIdentity();

                    boolean encryptStatus = storage.encryptIdentityKey(txtResetPasswordNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() ->
                            Snackbar.make(rootView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show()
                        );
                        return;
                    }
                    storage.clear();

                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            APPS_PREFERENCES,
                            Context.MODE_PRIVATE
                    );

                    if(importIdentity) {
                        long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putLong(CURRENT_ID, newIdentityId);
                        editor.apply();

                        handler.post(() -> {
                            updateSpinnerData(newIdentityId);
                            importPopupWindow.dismiss();

                            if(newIdentityId != 0) {
                                txtIdentityName.setText(mDbHelper.getIdentityName(newIdentityId));
                                renamePopupWindow.showAtLocation(renamePopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                            }
                        });
                    } else {
                        long currentId = sharedPref.getLong(CURRENT_ID, 0);
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

        final SQRLStorage storage = SQRLStorage.getInstance();

        loginPopupWindow.setTouchable(true);
        final EditText txtLoginPassword = popupView.findViewById(R.id.txtLoginPassword);

        txtLoginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence password, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence password, int start, int before, int count) {
                if (!storage.hasQuickPass()) return;
                if ((start + count) >= storage.getHintLength()) {
                    loginPopupWindow.dismiss();
                    progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    closeKeyboard();

                    new Thread(() -> {
                        boolean decryptionOk = storage.decryptIdentityKey(password.toString(), entropyHarvester, true);
                        if(!decryptionOk) {
                            Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            handler.post(() -> {
                                progressPopupWindow.dismiss();
                                loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                            });
                            storage.clear();
                            storage.clearQuickPass(MainActivity.this);
                            return;
                        }

                        handler.post(() -> txtLoginPassword.setText(""));

                        communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                        communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);

                        communicationFlowHandler.setDoneAction(() -> {
                            storage.clear();
                            handler.post(() -> {
                                progressPopupWindow.dismiss();
                                closeActivity();
                            });
                        });

                        communicationFlowHandler.setErrorAction(() -> {
                            storage.clear();
                            storage.clearQuickPass(MainActivity.this);
                            handler.post(() -> progressPopupWindow.dismiss());
                        });

                        communicationFlowHandler.handleNextAction();

                    }).start();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        popupView.findViewById(R.id.btnCloseLogin).setOnClickListener(v -> loginPopupWindow.dismiss());
        popupView.findViewById(R.id.btnLoginOptions).setOnClickListener(v -> {
            loginPopupWindow.dismiss();
            loginOptionsPopupWindow.showAtLocation(loginOptionsPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });

        popupView.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    APPS_PREFERENCES,
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(CURRENT_ID, 0);

            if(currentId != 0) {
                loginPopupWindow.dismiss();
                progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                closeKeyboard();

                new Thread(() -> {
                    boolean decryptionOk = storage.decryptIdentityKey(txtLoginPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptionOk) {
                        Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                        handler.post(() -> {
                            txtLoginPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        storage.clear();
                        return;
                    }
                    showClearNotification();

                    handler.post(() -> txtLoginPassword.setText(""));

                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);

                    communicationFlowHandler.setDoneAction(() -> {
                        storage.clear();
                        handler.post(() -> {
                            progressPopupWindow.dismiss();
                            closeActivity();
                        });
                    });

                    communicationFlowHandler.setErrorAction(() -> {
                        storage.clear();
                        storage.clearQuickPass(MainActivity.this);
                        handler.post(() -> progressPopupWindow.dismiss());
                    });

                    communicationFlowHandler.handleNextAction();

                }).start();
            }
        });
    }

    public void setupExportOptionsPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_export_options, null);

        exportOptionsPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);
        exportOptionsPopupWindow.setTouchable(true);

        final CheckBox cbWithoutPassword = popupView.findViewById(R.id.cbWithoutPassword);

        popupView.findViewById(R.id.btnCloseExportOptions).setOnClickListener(v -> {
            exportOptionsPopupWindow.dismiss();
        });

        popupView.findViewById(R.id.btnShowIdentity).setOnClickListener(v -> {
            exportOptionsPopupWindow.dismiss();
            Intent showIdentityIntent = new Intent(this, ShowIdentityActivity.class);
            showIdentityIntent.putExtra(EXPORT_WITHOUT_PASSWORD, cbWithoutPassword.isChecked());
            startActivity(showIdentityIntent);
        });

        popupView.findViewById(R.id.btnSaveIdentity).setOnClickListener(v -> {
            exportOptionsPopupWindow.dismiss();
            String uriString = "content://org.ea.sqrl.fileprovider/sqrltmp/";
            File directory = new File(getCacheDir(), "sqrltmp");
            if(!directory.mkdir()) {
                handler.post(() -> Snackbar.make(rootView, getString(R.string.main_activity_could_not_create_dir), Snackbar.LENGTH_LONG).show());
            }

            try {
                File file = File.createTempFile("identity", ".sqrl", directory);

                byte[] saveData;
                if(cbWithoutPassword.isChecked()) {
                    saveData = SQRLStorage.getInstance().createSaveDataWithoutPassword();
                } else {
                    saveData = SQRLStorage.getInstance().createSaveData();
                }

                FileOutputStream FileOutputStream = new FileOutputStream(file);
                FileOutputStream.write(saveData);
                FileOutputStream.close();

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(uriString + file.getName()));
                shareIntent.setType("application/octet-stream");
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.save_identity_to)));
            } catch (Exception e) {
                handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                Log.e(TAG, e.getMessage(), e);
            }
        });

        popupView.findViewById(R.id.btnPrintIdentity).setOnClickListener(v -> {
            exportOptionsPopupWindow.dismiss();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                String jobName = getString(R.string.app_name) + " Document";

                PrintAttributes printAttributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .build();

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        APPS_PREFERENCES,
                        Context.MODE_PRIVATE
                );
                long currentId = sharedPref.getLong(CURRENT_ID, 0);
                String identityName = mDbHelper.getIdentityName(currentId);

                printManager.print(jobName, new IdentityPrintDocumentAdapter(this, identityName, cbWithoutPassword.isChecked()), printAttributes);
            } else {
                showPrintingNotAvailableDialog();
            }
        });
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
        popupView.findViewById(R.id.btnForgotPassword).setOnClickListener(
                v -> {
                    importPopupWindow.dismiss();
                    resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                }
        );

        btnImportIdentityDo.setOnClickListener(v -> {
            importPopupWindow.dismiss();
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                SQRLStorage storage = SQRLStorage.getInstance();
                try {
                    boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        return;
                    }
                    showClearNotification();

                    boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(rootView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        return;
                    }
                    storage.clear();
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
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
                Snackbar.make(rootView, getString(R.string.change_password_retyped_password_do_not_match), Snackbar.LENGTH_LONG).show();
                txtCurrentPassword.setText("");
                txtNewPassword.setText("");
                txtRetypePassword.setText("");
                return;
            }

            changePasswordPopupWindow.dismiss();
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                try {
                    boolean decryptStatus = storage.decryptIdentityKey(txtCurrentPassword.getText().toString(), entropyHarvester, false);
                    if (!decryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtCurrentPassword.setText("");
                            txtNewPassword.setText("");
                            txtRetypePassword.setText("");
                        });
                        return;
                    }
                    showClearNotification();

                    boolean encryptStatus = storage.encryptIdentityKey(txtNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(rootView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show();
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
                        APPS_PREFERENCES,
                        Context.MODE_PRIVATE
                );
                long currentId = sharedPref.getLong(CURRENT_ID, 0);
                mDbHelper.updateIdentityData(currentId, storage.createSaveData());

                handler.post(() -> {
                    txtCurrentPassword.setText("");
                    txtNewPassword.setText("");
                    txtRetypePassword.setText("");
                });
            }).start();
        });
    }

    public void showPrintingNotAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.print_not_available_title)
                .setMessage(getString(R.string.print_not_available_text))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (renamePopupWindow != null && renamePopupWindow.isShowing()) {
            renamePopupWindow.dismiss();
        } else if (importPopupWindow != null && importPopupWindow.isShowing()) {
            importPopupWindow.dismiss();
        } else if (loginPopupWindow != null && loginPopupWindow.isShowing()) {
            loginPopupWindow.dismiss();
        } else if (changePasswordPopupWindow != null && changePasswordPopupWindow.isShowing()) {
            changePasswordPopupWindow.dismiss();
        } else {
            MainActivity.this.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest || importIdentity) return;

        if(!mDbHelper.hasIdentities()) {
            MainActivity.this.finish();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Snackbar.make(rootView, R.string.scan_cancel, Snackbar.LENGTH_LONG).show();
                if(!mDbHelper.hasIdentities()) {
                    MainActivity.this.finish();
                }
            } else {
                if(!importIdentity) {
                    String serverData = Utils.readSQRLQRCodeAsString(result.getRawBytes(), result.getErrorCorrectionLevel());
                    communicationFlowHandler.setServerData(serverData);
                    communicationFlowHandler.setUseSSL(serverData.startsWith("sqrl://"));

                    if(serverData.indexOf("://") == -1) {
                        handler.post(() -> Snackbar.make(rootView, R.string.scan_incorrect, Snackbar.LENGTH_LONG).show());
                        return;
                    }

                    int indexOfQuery = serverData.indexOf("/", serverData.indexOf("://") + 3);
                    String queryLink = serverData.substring(indexOfQuery);
                    final String domain = serverData.split("/")[2];
                    try {
                        communicationFlowHandler.setQueryLink(queryLink);
                        communicationFlowHandler.setDomain(domain);
                    } catch (Exception e) {
                        handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                        Log.e(TAG, e.getMessage(), e);
                        return;
                    }

                    handler.postDelayed(() -> {
                        final TextView txtSite = loginPopupWindow.getContentView().findViewById(R.id.txtSite);
                        txtSite.setText(domain);
                        loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    }, 100);
                } else {
                    SQRLStorage storage = SQRLStorage.getInstance();
                    try {
                        byte[] qrCodeData = Utils.readSQRLQRCode(result.getRawBytes(), result.getErrorCorrectionLevel());
                        if(qrCodeData.length == 0) {
                            handler.post(() -> Snackbar.make(rootView, R.string.scan_incorrect, Snackbar.LENGTH_LONG).show());
                            return;
                        }

                        storage.read(qrCodeData);

                        if(!storage.hasEncryptedKeys()) {
                            handler.postDelayed(() -> {
                                resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                            }, 100);
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
                        handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }
        }
    }

}
