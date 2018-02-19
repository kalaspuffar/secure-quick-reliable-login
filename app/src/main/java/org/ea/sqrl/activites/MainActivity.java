package org.ea.sqrl.activites;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
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
    private PopupWindow renamePopupWindow;

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

        LayoutInflater layoutInflater = (LayoutInflater)getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);

        View popupView = layoutInflater.inflate(R.layout.fragment_rename, null);

        renamePopupWindow =new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        renamePopupWindow.setTouchable(true);
        renamePopupWindow.setFocusable(true);
        EditText txtIdentityName = popupView.findViewById(R.id.txtIdentityName);

        ((Button) popupView.findViewById(R.id.btnRename))
                .setOnClickListener(v -> {

                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                    );
                    long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                    if(currentId != 0) {
                        mDbHelper.updateIdentityName(currentId, txtIdentityName.getText().toString());
                        updateSpinnerData(currentId);
                    }
                    txtIdentityName.setText("");
                    renamePopupWindow.dismiss();
                });

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(this.getString(R.string.button_scan_secret));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        final Button btnImportIdentity = findViewById(R.id.btnImportIdentity);
        btnImportIdentity.setOnClickListener(
                v -> new Thread(() -> integrator.initiateScan()).start()
        );

        final Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(
                v -> showNotImplementedDialog()
        );

        final Button btnCreate = findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(
                v -> showNotImplementedDialog()
        );

        final Button btnRemove = findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(
                v -> {
                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                    );
                    long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                    if(currentId != 0) {
                        mDbHelper.deleteIdentity(currentId);
                        updateSpinnerData(currentId);
                    }
                }
        );

        final Button btnRename = findViewById(R.id.btnRename);
        btnRename.setOnClickListener(
                v -> renamePopupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
        );

        final Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(
                v -> new Thread(() -> {
                    Intent intent = new Intent(this, ShowIdentityActivity.class);
                    startActivity(intent);
                }).start()
        );

        final Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(
                v -> showNotImplementedDialog()
        );

        final Button btnForgetQuickPass = findViewById(R.id.btnForgetQuickPass);
        btnForgetQuickPass.setOnClickListener(
                v -> showNotImplementedDialog()
        );

        final Button btnRekey = findViewById(R.id.btnRekey);
        btnRekey.setOnClickListener(
                v -> showNotImplementedDialog()
        );

        final Button btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePassword.setOnClickListener(
                v -> showNotImplementedDialog()
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

    private int getPosition(long currentId) {
        int i = 0;
        for(Long l : identities.keySet()) {
            if (l == currentId) return i;
            i++;
        }
        return 0;
    }

    private void updateSpinnerData(long currentId) {
        identities = mDbHelper.getIdentitys();

        ArrayAdapter adapter = new ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                identities.values().toArray(new String[identities.size()])
        );
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setSelection(getPosition(currentId));
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

                updateSpinnerData(newIdentityId);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
