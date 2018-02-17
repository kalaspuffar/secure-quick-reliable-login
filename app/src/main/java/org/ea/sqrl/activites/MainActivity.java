package org.ea.sqrl.activites;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.storage.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;

import java.util.Map;

public class MainActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private Spinner cboxIdentity;
    private Map<Long, String> identities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cboxIdentity = findViewById(R.id.cboxIdentity);
        identities = mDbHelper.getIdentitys();

        ArrayAdapter adapter = new ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            identities.values().toArray(new String[identities.size()])
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setOnItemSelectedListener(this);

        final Button btnUnlockIdentity = findViewById(R.id.btnUnlockIdentity);
        btnUnlockIdentity.setOnClickListener(
                v -> new Thread(() -> {
                    Intent intent = new Intent(this, DecryptingActivity.class);
                    startActivity(intent);
                }).start()
        );

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(this.getString(R.string.button_scan_secret));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        final Button btnLoadIdentity = findViewById(R.id.btnImportIdentity);
        btnLoadIdentity.setOnClickListener(
                v -> new Thread(() -> integrator.initiateScan()).start()
        );


        final Button btnImportIdentity = findViewById(R.id.btnImportIdentity);
        btnImportIdentity.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnCreate = findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnRemove = findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnRename = findViewById(R.id.btnRename);
        btnRename.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnForgetQuickPass = findViewById(R.id.btnForgetQuickPass);
        btnForgetQuickPass.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnRekey = findViewById(R.id.btnRekey);
        btnRekey.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );

        final Button btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePassword.setOnClickListener(
                v -> new Thread(() -> showNotImplementedDialog()).start()
        );
    }

    public void showNotImplementedDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(MainActivity.this);
        }
        builder.setTitle(R.string.not_implemented_title)
                .setMessage(getString(R.string.not_implemented_text))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Long[] keyArray = identities.keySet().toArray(new Long[identities.size()]);

        SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                getString(R.string.preferences),
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(getString(R.string.current_id), keyArray[pos]);
        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                SQRLStorage storage = SQRLStorage.getInstance();
                try {
                    byte[] qrCodeData = EncryptionUtils.readSQRLQRCode(result.getRawBytes());
                    storage.read(qrCodeData, true);
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        getString(R.string.preferences),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(getString(R.string.current_id), newIdentityId);
                editor.commit();

                identities = mDbHelper.getIdentitys();

                ArrayAdapter adapter = new ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        identities.values().toArray(new String[identities.size()])
                );
                cboxIdentity.setAdapter(adapter);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
