package org.ea.sqrl.activites.identity;

import android.content.Intent;
import android.os.Bundle;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;

public class ImportOptionsActivity extends BaseActivity {
    private static final String TAG = "ImportOptionsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_options);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        findViewById(R.id.btnScanQRCode).setOnClickListener(v -> {
            Intent importIdentityIntent = new Intent(this, ImportActivity.class);
            importIdentityIntent.putExtra(ImportActivity.EXTRA_IMPORT_METHOD, ImportActivity.IMPORT_METHOD_QR_CODE);
            startActivity(importIdentityIntent);
        });

        findViewById(R.id.btnPickIdentityFile).setOnClickListener(v -> {
            Intent importIdentityIntent = new Intent(this, ImportActivity.class);
            importIdentityIntent.putExtra(ImportActivity.EXTRA_IMPORT_METHOD, ImportActivity.IMPORT_METHOD_FILE);
            startActivity(importIdentityIntent);
        });

        findViewById(R.id.btnTextImport).setOnClickListener(v -> {
            Intent textImportIntent = new Intent(this, TextImportActivity.class);
            startActivity(textImportIntent);
        });
    }
}