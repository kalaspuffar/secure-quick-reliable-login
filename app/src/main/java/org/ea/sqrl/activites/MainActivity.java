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
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.services.IdentityPrintDocumentAdapter;
import org.ea.sqrl.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.regex.Matcher;

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
    private PopupWindow exportOptionsPopupWindow;

    private EditText txtIdentityName;
    private boolean importIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cboxIdentity = findViewById(R.id.cboxIdentity);
        rootView = findViewById(R.id.mainActivityView);
        communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

        setupRenamePopupWindow(getLayoutInflater());
        setupLoginPopupWindow(getLayoutInflater());
        setupImportPopupWindow(getLayoutInflater());
        setupExportOptionsPopupWindow(getLayoutInflater());
        setupLoginOptionsPopupWindow(getLayoutInflater(), true);
        setupErrorPopupWindow(getLayoutInflater());

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
                v -> startActivity(new Intent(this, ChangePasswordActivity.class))
        );

        final Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(
                v -> exportOptionsPopupWindow.showAtLocation(exportOptionsPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(
                v -> startActivity(new Intent(this, ResetPasswordActivity.class))
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
                            showErrorMessage(R.string.decrypt_identity_fail);
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
                        showErrorMessage(R.string.decrypt_identity_fail);
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
                showErrorMessage(R.string.main_activity_could_not_create_dir);
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
                showErrorMessage(e.getMessage());
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
                    startActivity(new Intent(this, ResetPasswordActivity.class));
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
                            showErrorMessage(R.string.decrypt_identity_fail);
                            txtPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        return;
                    }
                    storage.clearQuickPass(this);

                    boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() -> {
                            showErrorMessage(R.string.encrypt_identity_fail);
                            txtPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        return;
                    }
                    storage.clear();
                } catch (Exception e) {
                    showErrorMessage(e.getMessage());
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
                    String serverData = Utils.readSQRLQRCodeAsString(data);
                    communicationFlowHandler.setServerData(serverData);
                    communicationFlowHandler.setUseSSL(serverData.startsWith("sqrl://"));

                    Matcher sqrlMatcher = CommunicationHandler.sqrlPattern.matcher(serverData);
                    if(!sqrlMatcher.matches()) {
                        showErrorMessage(R.string.scan_incorrect);
                        return;
                    }

                    final String domain = sqrlMatcher.group(1);
                    final String queryLink = sqrlMatcher.group(2);
                    try {
                        communicationFlowHandler.setQueryLink(queryLink);
                        communicationFlowHandler.setDomain(domain);
                    } catch (Exception e) {
                        showErrorMessage(e.getMessage());
                        Log.e(TAG, e.getMessage(), e);
                        return;
                    }

                    handler.postDelayed(() -> {
                        final TextView txtSite = loginPopupWindow.getContentView().findViewById(R.id.txtSite);
                        txtSite.setText(domain);

                        SQRLStorage storage = SQRLStorage.getInstance();
                        final TextView txtLoginPassword = loginPopupWindow.getContentView().findViewById(R.id.txtLoginPassword);
                        if(storage.hasQuickPass()) {
                            txtLoginPassword.setHint(getString(R.string.login_identity_quickpass, "" + storage.getHintLength()));
                        } else {
                            txtLoginPassword.setHint(R.string.login_identity_password);
                        }

                        loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    }, 100);
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

}
