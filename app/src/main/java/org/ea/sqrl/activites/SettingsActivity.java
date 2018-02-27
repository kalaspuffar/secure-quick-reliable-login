package org.ea.sqrl.activites;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;

public class SettingsActivity extends BaseActivity {
    private Handler handler = new Handler();
    private PopupWindow savePopupWindow;

    private EditText txtSettingsHintLength;
    private EditText txtSettingsPasswordVerify;
    private EditText txtSettingsIdleTimeout;
    private CheckBox cbSettingsSQRLOnly;
    private CheckBox cbSettingsNoBypass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        LayoutInflater layoutInflater = (LayoutInflater)getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        setupSavePopupWindow(layoutInflater);

        SQRLStorage storage = SQRLStorage.getInstance();

        txtSettingsHintLength = findViewById(R.id.txtSettingsHintLength);
        txtSettingsHintLength.setText("" + storage.getHintLength());
        txtSettingsPasswordVerify = findViewById(R.id.txtSettingsPasswordVerify);
        txtSettingsPasswordVerify.setText("" + storage.getPasswordVerify());
        txtSettingsIdleTimeout = findViewById(R.id.txtSettingsIdleTimeout);
        txtSettingsIdleTimeout.setText("" + storage.getIdleTimeout());
        cbSettingsSQRLOnly = findViewById(R.id.cbSettingsSQRLOnly);
        cbSettingsSQRLOnly.setChecked(storage.isSQRLOnly());
        cbSettingsNoBypass = findViewById(R.id.cbSettingsNoBypass);
        cbSettingsNoBypass.setChecked(storage.isNoByPass());

        final Button btnSettingsCancel = findViewById(R.id.btnSettingsCancel);
        btnSettingsCancel.setOnClickListener(v -> SettingsActivity.this.finish());

        final Button btnSettingsSave = findViewById(R.id.btnSettingsSave);
        btnSettingsSave.setOnClickListener(v ->
            savePopupWindow.showAtLocation(savePopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );
    }

    public int getIntValue(EditText txt, int errorMessage) {
        try {
            return Integer.parseInt(txt.getText().toString());
        } catch (NumberFormatException nfe) {
            handler.post(() -> {
                Toast.makeText(SettingsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            });
        }
        return -1;
    }

    public void setupSavePopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_save_settings, null);

        savePopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        savePopupWindow.setTouchable(true);
        savePopupWindow.setFocusable(true);

        final ProgressBar pbDecrypting = popupView.findViewById(R.id.pbDecrypting);
        final EditText txtPassword = popupView.findViewById(R.id.txtPassword);
        final TextView progressText = popupView.findViewById(R.id.lblProgressText);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, pbDecrypting, progressText));

        final Button btnSaveSettings = popupView.findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(v -> new Thread(() -> {
            boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString());
            if(!decryptStatus) {
                handler.post(() -> {
                    Toast.makeText(SettingsActivity.this, getString(R.string.decrypt_identity_fail), Toast.LENGTH_LONG).show();
                    txtPassword.setText("");
                });
                return;
            }

            int hintLength = getIntValue(txtSettingsHintLength, R.string.settings_hint_length_not_number);
            if(hintLength == -1) return;
            if(hintLength > 255) {
                handler.post(() -> {
                    Toast.makeText(SettingsActivity.this, getString(R.string.settings_hint_length_to_large), Toast.LENGTH_LONG).show();
                });
                return;
            }
            int passwordVerify = getIntValue(txtSettingsPasswordVerify, R.string.settings_password_verify_not_number);
            if(passwordVerify == -1) return;
            int idleTimeout = getIntValue(txtSettingsIdleTimeout, R.string.settings_idle_timeout_not_number);
            if(idleTimeout == -1) return;

            storage.setHintLength(hintLength);
            storage.setPasswordVerify(passwordVerify);
            storage.setIdleTimeout(idleTimeout);
            storage.setSQRLOnly(cbSettingsSQRLOnly.isChecked());
            storage.setNoByPass(cbSettingsNoBypass.isChecked());

            boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
            if (!encryptStatus) {
                handler.post(() -> {
                    Toast.makeText(SettingsActivity.this, getString(R.string.encrypt_identity_fail), Toast.LENGTH_LONG).show();
                    txtPassword.setText("");
                });
                return;
            }
            storage.clear();

            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    getString(R.string.preferences),
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
            mDbHelper.updateIdentityData(currentId, storage.createSaveData());

            handler.post(() -> {
                txtPassword.setText("");
                savePopupWindow.dismiss();
            });

        }).start());
    }
}
