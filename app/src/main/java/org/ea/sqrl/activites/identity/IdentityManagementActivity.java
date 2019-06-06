package org.ea.sqrl.activites.identity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.SettingsActivity;
import org.ea.sqrl.activites.StartActivity;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.activites.create.CreateIdentityActivity;
import org.ea.sqrl.activites.create.RekeyIdentityActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.utils.SqrlApplication;

/**
 * This activity is the central hub for all identity management functionality.
 *
 * @author Daniel Persson
 */
public class IdentityManagementActivity extends LoginBaseActivity {
    private static final String TAG = "IdentityManagementActivity";

    private ImageButton btnIdentityOptions = null;
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

        final Button btnCreate = findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(v -> startActivity(new Intent(this, CreateIdentityActivity.class)));

        final Button btnImport = findViewById(R.id.btnImport);
        btnImport.setOnClickListener(
                v -> startActivity(new Intent(this, ImportOptionsActivity.class))
        );

        btnIdentityOptions = findViewById(R.id.btnIdentityOptions);
        btnIdentityOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(IdentityManagementActivity.this, btnIdentityOptions);
            popup.getMenuInflater()
                    .inflate(R.menu.menu_id_management_options, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {

                    case R.id.action_idm_settings:
                        startActivity(new Intent(this, SettingsActivity.class));
                        break;
                    case R.id.action_idm_rename:
                        startActivity(new Intent(this, RenameActivity.class));
                        break;
                    case R.id.action_idm_remove:
                        removeIdentity();
                        break;
                    case R.id.action_idm_export:
                        startActivity(new Intent(this, ExportOptionsActivity.class));
                        break;
                    case R.id.action_idm_password_options:
                        showPasswordOptionsMenu();
                        break;
                    default:
                        break;
                }
                return true;
            });

            popup.show();
        });
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

            long currentId = SqrlApplication.getCurrentId(this.getApplication());
            if(currentId != 0) {
                updateSpinnerData(currentId);
            }
        }
    }

    private void showPasswordOptionsMenu() {
        PopupMenu popup = new PopupMenu(IdentityManagementActivity.this, btnIdentityOptions);
        popup.getMenuInflater()
                .inflate(R.menu.menu_id_password_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {

                case R.id.action_idm_pw_change_password:
                    startActivity(new Intent(this, ChangePasswordActivity.class));
                    break;
                case R.id.action_idm_pw_reset_password:
                    startActivity(new Intent(this, ResetPasswordActivity.class));
                    break;
                case R.id.action_idm_pw_rekey:
                    startActivity(new Intent(this, RekeyIdentityActivity.class));
                    break;
                default:
                    break;
            }
            return true;
        });

        popup.show();
    }

    private void removeIdentity() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    long currentId = SqrlApplication.getCurrentId(IdentityManagementActivity.this.getApplication());

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
}