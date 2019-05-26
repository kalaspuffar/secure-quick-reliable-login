package org.ea.sqrl.activites.identity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.SettingsActivity;
import org.ea.sqrl.activites.StartActivity;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.activites.create.CreateIdentityActivity;
import org.ea.sqrl.activites.create.RekeyIdentityActivity;
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
public class IdentityManagementActivity extends LoginBaseActivity {
    private static final String TAG = "IdentityManagementActivity";

    private boolean importIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_management);

        cboxIdentity = findViewById(R.id.cboxIdentity);
        txtOneIdentity = findViewById(R.id.txtOneIdentity);
        rootView = findViewById(R.id.identityManagementActivityView);
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

        setupLoginPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());
        setupBasePopups(getLayoutInflater(), false);

        final ImageButton btnCloseMain = findViewById(R.id.btnCloseMain);
        btnCloseMain.setOnClickListener(v -> IdentityManagementActivity.this.finish());

        final Button btnImport = findViewById(R.id.btnImport);
        btnImport.setOnClickListener(
                v -> startActivity(new Intent(this, ImportOptionsActivity.class))
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
                                SharedPreferences sharedPref = IdentityManagementActivity.this.getApplication().getSharedPreferences(
                                        APPS_PREFERENCES,
                                        Context.MODE_PRIVATE
                                );
                                long currentId = sharedPref.getLong(CURRENT_ID, 0);
                                if(currentId != 0) {
                                    mDbHelper.deleteIdentity(currentId);
                                    updateSpinnerData(-1);
                                    Snackbar.make(rootView, getString(R.string.main_identity_removed), Snackbar.LENGTH_LONG).show();

                                    if(!mDbHelper.hasIdentities()) {
                                        startActivity(new Intent(IdentityManagementActivity.this, StartActivity.class));
                                    }
                                }
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(IdentityManagementActivity.this);
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
            IdentityManagementActivity.this.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest || importIdentity) return;

        if(!mDbHelper.hasIdentities()) {
            IdentityManagementActivity.this.finish();
        } else {
            setupBasePopups(getLayoutInflater(), false);

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