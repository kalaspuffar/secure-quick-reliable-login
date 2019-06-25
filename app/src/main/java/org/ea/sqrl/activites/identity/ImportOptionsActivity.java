package org.ea.sqrl.activites.identity;

import android.content.Intent;
import android.os.Bundle;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;

public class ImportOptionsActivity extends BaseActivity {
    private static final String TAG = "ImportOptionsActivity";
    public static final String EXTRA_IMPORT_METHOD = "ImportMethod";
    public static final String IMPORT_METHOD_QRCODE = "QR_CODE";
    public static final String IMPORT_METHOD_FILE = "FILE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_options);

        setupProgressPopupWindow(getLayoutInflater());
        setupErrorPopupWindow(getLayoutInflater());

        findViewById(R.id.btnScanQRCode).setOnClickListener(v -> {
            Intent importIdentityIntent = new Intent(this, ImportActivity.class);
            importIdentityIntent.putExtra(EXTRA_IMPORT_METHOD, IMPORT_METHOD_QRCODE);
            startActivity(importIdentityIntent);
        });

        findViewById(R.id.btnPickIdentityFile).setOnClickListener(v -> {
            Intent importIdentityIntent = new Intent(this, ImportActivity.class);
            importIdentityIntent.putExtra(EXTRA_IMPORT_METHOD, IMPORT_METHOD_FILE);
            startActivity(importIdentityIntent);
        });

        findViewById(R.id.btnTextImport).setOnClickListener(v -> {
            Intent textImportIntent = new Intent(this, TextImportActivity.class);
            startActivity(textImportIntent);
        });
    }
}