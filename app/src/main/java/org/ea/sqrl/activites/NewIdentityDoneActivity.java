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
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.services.IdentityPrintDocumentAdapter;

import java.io.File;
import java.io.FileOutputStream;

/**
 *
 * @author Daniel Persson
 */
public class NewIdentityDoneActivity extends LoginBaseActivity {
    private static final String TAG = "RekeyIdentityActivity";

    private PopupWindow exportOptionsPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_identity_done);

        SQRLStorage.getInstance().clear();

        setupExportOptionsPopupWindow(getLayoutInflater());

        rootView = findViewById(R.id.newIdentityDoneActivityView);

        final TextView txtNewIdentityDoneMessage = findViewById(R.id.txtNewIdentityDoneMessage);
        txtNewIdentityDoneMessage.setMovementMethod(LinkMovementMethod.getInstance());

        final Button btnNewIdentityDoneExport = findViewById(R.id.btnNewIdentityDoneExport);
        btnNewIdentityDoneExport.setOnClickListener(
                v -> exportOptionsPopupWindow.showAtLocation(exportOptionsPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnNewIdentityDone = findViewById(R.id.btnNewIdentityDone);
        btnNewIdentityDone.setOnClickListener(
                v -> {
                    this.finish();
                    startActivity(new Intent(this, MainActivity.class));
                }
        );
    }

    public void setupExportOptionsPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_export_options, null);

        exportOptionsPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);
        exportOptionsPopupWindow.setTouchable(true);

        popupView.findViewById(R.id.btnCloseExportOptions).setOnClickListener(v -> {
            exportOptionsPopupWindow.dismiss();
        });

        popupView.findViewById(R.id.btnShowIdentity).setOnClickListener(v -> {
            exportOptionsPopupWindow.dismiss();
            startActivity(new Intent(this, ShowIdentityActivity.class));
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

                FileOutputStream FileOutputStream = new FileOutputStream(file);
                FileOutputStream.write(SQRLStorage.getInstance().createSaveData());
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

                try {
                    SQRLStorage.getInstance().createVerifyRecoveryBlock();

                    printManager.print(jobName, new IdentityPrintDocumentAdapter(this, identityName), printAttributes);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            } else {
                showPrintingNotAvailableDialog();
            }
        });
    }

    public void showPrintingNotAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(NewIdentityDoneActivity.this);
        builder.setTitle(R.string.print_not_available_title)
                .setMessage(getString(R.string.print_not_available_text))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    public void showNotImplementedDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(NewIdentityDoneActivity.this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(NewIdentityDoneActivity.this);
        }
        builder.setTitle(R.string.not_implemented_title)
                .setMessage(getString(R.string.not_implemented_text))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
}
