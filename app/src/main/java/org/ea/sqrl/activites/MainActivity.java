package org.ea.sqrl.activites;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.account.AccountOptionsActivity;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.activites.create.CreateIdentityActivity;
import org.ea.sqrl.activites.create.RekeyIdentityActivity;
import org.ea.sqrl.activites.identity.ChangePasswordActivity;
import org.ea.sqrl.activites.identity.ExportOptionsActivity;
import org.ea.sqrl.activites.identity.ImportActivity;
import org.ea.sqrl.activites.identity.RenameActivity;
import org.ea.sqrl.activites.identity.ResetPasswordActivity;
import org.ea.sqrl.activites.identity.TextImportActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.Utils;

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

    private boolean importIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cboxIdentity = findViewById(R.id.cboxIdentity);
        rootView = findViewById(R.id.mainActivityView);
        communicationFlowHandler = CommunicationFlowHandler.getInstance(this, handler);

        ImageView moreIndicator = findViewById(R.id.more_indicator);
        ScrollView scrollView = findViewById(R.id.main_scroll);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if(!scrollView.canScrollVertically(1)) {
                moreIndicator.setVisibility(View.INVISIBLE);
            } else {
                moreIndicator.setVisibility(View.VISIBLE);
            }
        });

        setupLoginPopupWindow(getLayoutInflater(), MainActivity.this);
        setupErrorPopupWindow(getLayoutInflater());

        setupBasePopups(getLayoutInflater(), false);

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(false);
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
                v -> startActivity(new Intent(this, ImportActivity.class))
        );

        final Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class))
        );

        final Button btnRemove = findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(
                v -> {
                    DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                SharedPreferences sharedPref = MainActivity.this.getApplication().getSharedPreferences(
                                        APPS_PREFERENCES,
                                        Context.MODE_PRIVATE
                                );
                                long currentId = sharedPref.getLong(CURRENT_ID, 0);
                                if(currentId != 0) {
                                    mDbHelper.deleteIdentity(currentId);
                                    updateSpinnerData(currentId);
                                    Snackbar.make(rootView, getString(R.string.main_identity_removed), Snackbar.LENGTH_LONG).show();

                                    if(!mDbHelper.hasIdentities()) {
                                        startActivity(new Intent(MainActivity.this, StartActivity.class));
                                    }
                                }
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.remove_identity_confirmation)
                            .setNegativeButton(R.string.remove_identity_confirmation_negative, dialogClickListener)
                            .setPositiveButton(R.string.remove_identity_confirmation_positive, dialogClickListener)
                            .show();
                }
        );

        final Button btnRename = findViewById(R.id.btnRename);
        btnRename.setOnClickListener(
                v -> startActivity(new Intent(this, RenameActivity.class))
        );

        final Button btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePassword.setOnClickListener(
                v -> startActivity(new Intent(this, ChangePasswordActivity.class))
        );

        final Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(
                v -> startActivity(new Intent(this, ExportOptionsActivity.class))
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

    @Override
    public void onBackPressed() {
        if (loginPopupWindow != null && loginPopupWindow.isShowing()) {
            hideLoginPopup();
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
            setupBasePopups(getLayoutInflater(), false);
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
                        communicationFlowHandler.setDomain(domain, queryLink);
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

                        showLoginPopup();
                    }, 100);
                }
            }
        }
    }

}
