package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.activites.create.CreateIdentityActivity;
import org.ea.sqrl.activites.identity.ImportActivity;
import org.ea.sqrl.activites.identity.ImportOptionsActivity;
import org.ea.sqrl.activites.identity.TextImportActivity;
import org.ea.sqrl.utils.Utils;

import java.util.Objects;

/**
 * Start activity should be a base for the user so we bring them into the application and they know
 * how to use it when installed and identities are added. So where we add some text for to inform
 * as well as a link to import your first identity.
 *
 * @author Daniel Persson
 */
public class StartActivity extends BaseActivity {
    private static final String TAG = "StartActivity";
    private boolean createNewIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.startup_wizard_page_4);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setupProgressPopupWindow(getLayoutInflater());
        setupCameraAccessPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        TextView wizardText = findViewById(R.id.wizard_text);
        String s = (String)wizardText.getText();
        Spannable textSpan = Utils.getSpanWithHighlight(s);
        wizardText.setText(textSpan);

        final Button btnStartCreateIdentity = findViewById(R.id.btnStartCreateIdentity);
        btnStartCreateIdentity.setOnClickListener(v -> {
            createNewIdentity = true;
            this.showPhoneStatePermission();
        });

        final Button btnImportIdentity = findViewById(R.id.btnImportIdentity);
        btnImportIdentity.setOnClickListener(
            v -> startActivity(new Intent(this, ImportOptionsActivity.class))
        );
    }

    @Override
    protected void permissionOkCallback() {
        if(createNewIdentity) {
            startActivity(new Intent(this, CreateIdentityActivity.class));
        } else {
            Intent importIdentityIntent = new Intent(this, ImportActivity.class);
            importIdentityIntent.putExtra(ImportActivity.EXTRA_IMPORT_METHOD,
                    ImportActivity.IMPORT_METHOD_QR_CODE);
            startActivity(importIdentityIntent);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        StartActivity.this.finishAffinity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            StartActivity.this.finishAndRemoveTask();
        }
    }
 }
