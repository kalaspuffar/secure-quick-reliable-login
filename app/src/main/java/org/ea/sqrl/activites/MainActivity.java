package org.ea.sqrl.activites;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.ProgressionUpdater;
import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;

import java.util.Map;

/**
 * This main activity is the hub of the application where the user lands for daily use. It should
 * make it easy to reach all the functions you use often and hide things that you don't need
 * regularly.
 *
 * @author Daniel Persson
 */
public class MainActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private Handler handler = new Handler();
    private Spinner cboxIdentity;
    private Map<Long, String> identities;
    private PopupWindow renamePopupWindow;
    private PopupWindow decryptPopupWindow;
    private Button btnUnlockIdentity;
    private boolean useIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cboxIdentity = findViewById(R.id.cboxIdentity);
        identities = mDbHelper.getIdentitys();

        ArrayAdapter adapter = new ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            identities.values().toArray(new String[identities.size()])
        );
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setOnItemSelectedListener(this);

        LayoutInflater layoutInflater = (LayoutInflater)getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);

        setupRenamePopupWindow(layoutInflater);
        setupImportPopupWindow(layoutInflater);

        final IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(this.getString(R.string.button_scan_secret));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        Intent intent = getIntent();
        int startMode = intent.getIntExtra(START_USER_MODE, 0);
        if(startMode == START_USER_MODE_NEW_USER) {
            integrator.initiateScan();
        }

        btnUnlockIdentity = findViewById(R.id.btnUnlockIdentity);
        btnUnlockIdentity.setOnClickListener(
                v -> {
                    useIdentity = true;
                    final TextView txtRecoveryKey = decryptPopupWindow.getContentView().findViewById(R.id.txtRecoveryKey);
                    txtRecoveryKey.setText(SQRLStorage.getInstance().getVerifyingRecoveryBlock());
                    decryptPopupWindow.showAtLocation(decryptPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                }
        );

        final Button btnImportIdentity = findViewById(R.id.btnImportIdentity);
        btnImportIdentity.setOnClickListener(
                v -> {
                    useIdentity = false;
                    integrator.initiateScan();
                }
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
                v -> renamePopupWindow.showAtLocation(renamePopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(
                v -> new Thread(() -> {
                    startActivity(new Intent(this, ShowIdentityActivity.class));
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

    public void setupRenamePopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_rename, null);

        renamePopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        renamePopupWindow.setTouchable(true);
        renamePopupWindow.setFocusable(true);
        final EditText txtIdentityName = popupView.findViewById(R.id.txtIdentityName);

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
    }

    public void setupImportPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_decrypt, null);

        decryptPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        decryptPopupWindow.setTouchable(true);
        decryptPopupWindow.setFocusable(true);

        final ProgressBar pbDecrypting = popupView.findViewById(R.id.pbDecrypting);
        final EditText txtPassword = popupView.findViewById(R.id.txtPassword);
        final Button btnDecryptKey = popupView.findViewById(R.id.btnDecryptKey);
        final TextView progressText = popupView.findViewById(R.id.lblProgressText);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, pbDecrypting, progressText));

        btnDecryptKey.setOnClickListener(v -> new Thread(() -> {
            try {
                boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString());
                if(!decryptStatus) {
                    System.out.println("Could not decrypt identity");
                    return;
                }

                if(!useIdentity) {
                    boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        System.out.println("Could not encrypt identity");
                        return;
                    }
                    storage.clear();
                } else {
                    showClearNotification();
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
                e.printStackTrace();
            }
            long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());

            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    getString(R.string.preferences),
                    Context.MODE_PRIVATE
            );
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(getString(R.string.current_id), newIdentityId);
            editor.commit();

            handler.post(() -> {
                updateSpinnerData(newIdentityId);

                txtPassword.setText("");
                decryptPopupWindow.dismiss();
            });

            if(useIdentity) {
                startActivity(new Intent(this, LoginActivity.class));
            }
        }).start());
    }

    public void showClearNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            String CHANNEL_ID = "my_channel_01";

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.setDescription("Channel description");
                notificationManager.createNotificationChannel(notificationChannel);
            }

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setDefaults(Notification.DEFAULT_LIGHTS)
                            .setSmallIcon(R.drawable.ic_stat_sqrl_logo_vector_outline)
                            .setContentTitle(getString(R.string.notification_identity_unlocked))
                            .setContentText(getString(R.string.notification_identity_unlocked_desc));

            Intent resultIntent = new Intent(this, ClearIdentityActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(ClearIdentityActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            mBuilder.setAutoCancel(true);

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(NOTIFICATION_IDENTITY_UNLOCKED, mBuilder.build());
        } else {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    this,
                    NotificationChannel.DEFAULT_CHANNEL_ID
            )
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setSmallIcon(R.drawable.ic_stat_sqrl_logo_vector_outline)
                    .setContentTitle(getString(R.string.notification_identity_unlocked))
                    .setContentText(getString(R.string.notification_identity_unlocked_desc));

            Intent resultIntent = new Intent(this, ClearIdentityActivity.class);
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            mBuilder.setAutoCancel(true);

            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(NOTIFICATION_IDENTITY_UNLOCKED, mBuilder.build());
        }
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

        SQRLStorage storage = SQRLStorage.getInstance();
        try {
            byte[] identityData = mDbHelper.getIdentityData(keyArray[pos]);
            storage.read(identityData, true);
            btnUnlockIdentity.setEnabled(storage.hasIdentityBlock());

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                R.layout.simple_spinner_item,
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
                byte[] qrCodeData = EncryptionUtils.readSQRLQRCode(result.getRawBytes());
                try {
                    storage.read(qrCodeData, true);
                    final TextView txtRecoveryKey = decryptPopupWindow.getContentView().findViewById(R.id.txtRecoveryKey);
                    txtRecoveryKey.setText(storage.getVerifyingRecoveryBlock());
                    decryptPopupWindow.showAtLocation(decryptPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
