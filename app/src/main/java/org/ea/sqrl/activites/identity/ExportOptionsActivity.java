package org.ea.sqrl.activites.identity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.util.Log;
import android.widget.CheckBox;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.services.IdentityPrintDocumentAdapter;

import java.io.File;
import java.io.FileOutputStream;

public class ExportOptionsActivity extends BaseActivity {
    private static final String TAG = "ExportOptionsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_options);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());
        final CheckBox cbWithoutPassword = findViewById(R.id.cbWithoutPassword);

        findViewById(R.id.btnShowIdentity).setOnClickListener(v -> {
            Intent showIdentityIntent = new Intent(this, ShowIdentityActivity.class);
            showIdentityIntent.putExtra(EXPORT_WITHOUT_PASSWORD, cbWithoutPassword.isChecked());
            startActivity(showIdentityIntent);
        });

        findViewById(R.id.btnSaveIdentity).setOnClickListener(v -> {
            String uriString = "content://org.ea.sqrl.fileprovider/sqrltmp/";
            File directory = new File(getCacheDir(), "sqrltmp");
            if(!directory.exists()) directory.mkdir();

            if(!directory.exists()) {
                showErrorMessage(R.string.main_activity_could_not_create_dir);
                return;
            }

            SQRLStorage storage = SQRLStorage.getInstance(ExportOptionsActivity.this.getApplicationContext());

            try {
                File file = File.createTempFile("identity", ".sqrl", directory);

                byte[] saveData;
                if(cbWithoutPassword.isChecked()) {
                    saveData = storage.createSaveDataWithoutPassword();
                } else {
                    saveData = storage.createSaveData();
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

        findViewById(R.id.btnPrintIdentity).setOnClickListener(v -> {
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


    public void showPrintingNotAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ExportOptionsActivity.this);
        builder.setTitle(R.string.print_not_available_title)
                .setMessage(getString(R.string.print_not_available_text))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
}
