package org.ea.sqrl.activites.identity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.PopupMenu;
import android.widget.Button;
import android.widget.ImageButton;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.StartActivity;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.activites.create.CreateIdentityActivity;
import org.ea.sqrl.activites.create.RekeyIdentityActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.utils.IdentitySelector;
import org.ea.sqrl.utils.SqrlApplication;

/**
 * This activity is the central hub for all identity management functionality.
 *
 * @author Daniel Persson
 */
public class IdentityManagementActivity extends BaseActivity {
    private static final String TAG = "IdentityManagementActivity";

    private IdentitySelector mIdentitySelector = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_management);

        setupErrorPopupWindow(getLayoutInflater());

        final ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> IdentityManagementActivity.this.finish());

        final Button btnCreate = findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(v -> startActivity(new Intent(this, CreateIdentityActivity.class)));

        final Button btnImport = findViewById(R.id.btnImport);
        btnImport.setOnClickListener(
                v -> startActivity(new Intent(this, ImportOptionsActivity.class))
        );

        mIdentitySelector = new IdentitySelector(this, true, true, false);
        mIdentitySelector.registerLayout(findViewById(R.id.identitySelector));
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        if(!mDbHelper.hasIdentities()) {
            IdentityManagementActivity.this.finish();
        } else {
            mIdentitySelector.update();
        }
    }
}