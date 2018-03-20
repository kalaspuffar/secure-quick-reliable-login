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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.NotificationCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.R;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;
import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import java.util.Map;

/**
 * This main activity is the hub of the application where the user lands for daily use. It should
 * make it easy to reach all the functions you use often and hide things that you don't need
 * regularly.
 *
 * @author Daniel Persson
 */
public class MainActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";

    private Handler handler = new Handler();
    private Spinner cboxIdentity;
    private Map<Long, String> identities;
    private PopupWindow renamePopupWindow;
    private PopupWindow decryptPopupWindow;
    private PopupWindow loginPopupWindow;
    private PopupWindow changePasswordPopupWindow;
    private PopupWindow resetPasswordPopupWindow;

    private FrameLayout progressBarHolder;

    private Button btnUnlockIdentity;
    private EditText txtIdentityName;
    private ConstraintLayout mainView;
    private boolean useIdentity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBarHolder = findViewById(R.id.progressBarHolder);

        cboxIdentity = findViewById(R.id.cboxIdentity);
        identities = mDbHelper.getIdentitys();

        mainView = findViewById(R.id.main_view);

        ArrayAdapter adapter = new ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            identities.values().toArray(new String[identities.size()])
        );
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setOnItemSelectedListener(this);

        LayoutInflater inflater = (LayoutInflater)getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final Context contextThemeWrapper = new ContextThemeWrapper(this, R.style.AppTheme);
        LayoutInflater layoutInflater = inflater.cloneInContext(contextThemeWrapper);

        setupRenamePopupWindow(layoutInflater);
        setupLoginPopupWindow(layoutInflater);
        setupImportPopupWindow(layoutInflater);
        setupChangePasswordPopupWindow(layoutInflater);
        setupResetPasswordPopupWindow(layoutInflater);

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
                    integrator.initiateScan();
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
                v -> startActivity(new Intent(this, SettingsActivity.class))
        );

        final Button btnCreate = findViewById(R.id.btnCreate);
        btnCreate.setOnClickListener(
                v -> {
                    showNotImplementedDialog();

                    Sodium sodium = NaCl.sodium();
                    String Password = "hunter2";
                    byte[] key = new byte[Sodium.crypto_box_seedbytes()];
                    byte[] passwd = Password.getBytes();
                    byte[] salt = new byte[]{ 88, (byte)240, (byte)185, 66, (byte)195, 101, (byte)160, (byte)138, (byte)137, 78, 1, 2, 3, 4, 5, 6};

                    Sodium.crypto_pwhash(
                            key,
                            key.length,
                            passwd,
                            passwd.length,
                            salt,
                            Sodium.crypto_pwhash_opslimit_interactive(),
                            Sodium.crypto_pwhash_memlimit_interactive(),
                            Sodium.crypto_pwhash_alg_default()
                    );

                    byte[] dst_public_Key = new byte[32];
                    byte[] dst_private_key = new byte[32];
                    byte[] src_seed = new byte[32];
                    entropyHarvester.fetchRandom(src_seed);
                    Sodium.crypto_sign_seed_keypair(dst_public_Key, dst_private_key, src_seed);
                }
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
                v -> {
                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                    );
                    long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                    if(currentId != 0) {
                        txtIdentityName.setText(mDbHelper.getIdentityName(currentId));
                    }
                    renamePopupWindow.showAtLocation(renamePopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                }
        );

        final Button btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePassword.setOnClickListener(
                v -> changePasswordPopupWindow.showAtLocation(changePasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(
                v -> new Thread(() -> {
                    startActivity(new Intent(this, ShowIdentityActivity.class));
                }).start()
        );

        final Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(
                v -> resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );

        final Button btnForgetQuickPass = findViewById(R.id.btnForgetQuickPass);
        btnForgetQuickPass.setOnClickListener(
                v -> showNotImplementedDialog()
        );

        final Button btnRekey = findViewById(R.id.btnRekey);
        btnRekey.setOnClickListener(
                v -> showNotImplementedDialog()
        );
    }

    public void setupRenamePopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_rename, null);

        renamePopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        renamePopupWindow.setTouchable(true);
        txtIdentityName = popupView.findViewById(R.id.txtIdentityName);

        popupView.findViewById(R.id.btnCloseRename).setOnClickListener(v -> renamePopupWindow.dismiss());
        popupView.findViewById(R.id.btnRename).setOnClickListener(v -> {

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

    private void showProgressBar() {
        handler.post(() -> {
            mainView.setEnabled(false);
            AlphaAnimation inAnimation = new AlphaAnimation(0f, 1f);
            inAnimation.setDuration(200);
            progressBarHolder.setAnimation(inAnimation);
            progressBarHolder.setVisibility(View.VISIBLE);
        });
    }

    private void hideProgressBar() {
        handler.post(() -> {
            mainView.setEnabled(true);
            AlphaAnimation outAnimation = new AlphaAnimation(1f, 0f);
            outAnimation.setDuration(200);
            progressBarHolder.setAnimation(outAnimation);
            progressBarHolder.setVisibility(View.GONE);
        });
    }

    private final CommunicationHandler commHandler = CommunicationHandler.getInstance();
    private TextView txtErrorMessage;
    private String serverData = null;
    private String queryLink = null;

    private void postQuery(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientQuery(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        handler.post(() -> txtErrorMessage.setText(commHandler.getErrorMessage(this)));
        commHandler.printParams();
    }

    private void postCreateAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(
                commHandler.createClientCreateAccount(entropyHarvester),
                serverData
        );
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        handler.post(() -> txtErrorMessage.setText(commHandler.getErrorMessage(this)));
        commHandler.printParams();
    }

    private void postLogin(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientLogin(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        handler.post(() -> txtErrorMessage.setText(commHandler.getErrorMessage(this)));
        commHandler.printParams();
    }


    public void setupResetPasswordPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_reset_password, null);

        resetPasswordPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        resetPasswordPopupWindow.setTouchable(true);

        popupView.findViewById(R.id.btnCloseResetPassword).setOnClickListener(v -> resetPasswordPopupWindow.dismiss());
        popupView.findViewById(R.id.btnResetPassword).setOnClickListener(v -> {

            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    getString(R.string.preferences),
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(getString(R.string.current_id), 0);

            if(currentId != 0) {
            }

            resetPasswordPopupWindow.dismiss();
        });
    }

    public void setupLoginPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_login, null);

        loginPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        loginPopupWindow.setTouchable(true);
        final EditText txtLoginPassword = popupView.findViewById(R.id.txtLoginPassword);
        txtErrorMessage = popupView.findViewById(R.id.txtErrorMessage);

        popupView.findViewById(R.id.btnCloseLogin).setOnClickListener(v -> loginPopupWindow.dismiss());
        popupView.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    getString(R.string.preferences),
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(getString(R.string.current_id), 0);

            if(currentId != 0) {
                loginPopupWindow.dismiss();
                showProgressBar();

                new Thread(() -> {
                    boolean decryptionOk = SQRLStorage.getInstance().decryptIdentityKey(txtLoginPassword.getText().toString());
                    if(decryptionOk) {
                        showClearNotification();
                    } else {
                        txtErrorMessage.setText(getString(R.string.decrypt_identity_fail));
                        return;
                    }

                    try {
                        postQuery(commHandler);

                        if(
                            commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                            commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)
                        ) {
                            postLogin(commHandler);
                        } else if(
                            !commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) &&
                            !commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)
                        ){
                            postCreateAccount(commHandler);
                        } else {
                            handler.post(() -> {
                                txtLoginPassword.setText("");
                                txtErrorMessage.setText(commHandler.getErrorMessage(this));
                            });
                        }
                    } catch (Exception e) {
                        handler.post(() -> txtErrorMessage.setText(e.getMessage()));
                        Log.e(TAG, e.getMessage(), e);
                        return;
                    } finally {
                        hideProgressBar();
                    }

                    handler.post(() -> {
                        txtErrorMessage.setText("");
                        txtLoginPassword.setText("");
                    });
                }).start();
            }
        });
    }

    public void setupImportPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_decrypt, null);

        decryptPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        decryptPopupWindow.setTouchable(true);

        final ProgressBar pbDecrypting = popupView.findViewById(R.id.pbDecrypting);
        final EditText txtPassword = popupView.findViewById(R.id.txtPassword);
        final Button btnDecryptKey = popupView.findViewById(R.id.btnDecryptKey);
        final TextView progressText = popupView.findViewById(R.id.lblProgressText);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, pbDecrypting, progressText));

        popupView.findViewById(R.id.btnCloseImportIdentity).setOnClickListener(v -> decryptPopupWindow.dismiss());
        btnDecryptKey.setOnClickListener(v -> new Thread(() -> {
            try {
                boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString());
                if(!decryptStatus) {
                    handler.post(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.decrypt_identity_fail), Toast.LENGTH_LONG).show();
                        txtPassword.setText("");
                    });
                    return;
                }

                boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
                if (!encryptStatus) {
                    handler.post(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.encrypt_identity_fail), Toast.LENGTH_LONG).show();
                        txtPassword.setText("");
                    });
                    return;
                }
                storage.clear();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
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

                if(newIdentityId != 0) {
                    txtIdentityName.setText(mDbHelper.getIdentityName(newIdentityId));
                    renamePopupWindow.showAtLocation(renamePopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                }
            });
        }).start());
    }

    public void setupChangePasswordPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_change_password, null);

        changePasswordPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        changePasswordPopupWindow.setTouchable(true);


        final EditText txtCurrentPassword = popupView.findViewById(R.id.txtCurrentPassword);
        final EditText txtNewPassword = popupView.findViewById(R.id.txtNewPassword);
        final EditText txtRetypePassword = popupView.findViewById(R.id.txtRetypePassword);

        SQRLStorage storage = SQRLStorage.getInstance();


        popupView.findViewById(R.id.btnCloseChangePassword).setOnClickListener(v -> changePasswordPopupWindow.dismiss());
        final Button btnChangePassword = popupView.findViewById(R.id.btnDoChangePassword);
        btnChangePassword.setOnClickListener(v -> {
            if(!txtNewPassword.getText().toString().equals(txtRetypePassword.getText().toString())) {
                Toast.makeText(MainActivity.this, getString(R.string.change_password_retyped_password_do_not_match), Toast.LENGTH_LONG).show();
                txtCurrentPassword.setText("");
                txtNewPassword.setText("");
                txtRetypePassword.setText("");
                return;
            }

            handler.post(() -> changePasswordPopupWindow.dismiss());
            showProgressBar();

            new Thread(() -> {
                try {
                    boolean decryptStatus = storage.decryptIdentityKey(txtCurrentPassword.getText().toString());
                    if (!decryptStatus) {
                        handler.post(() -> {
                            Toast.makeText(MainActivity.this, getString(R.string.decrypt_identity_fail), Toast.LENGTH_LONG).show();
                            txtCurrentPassword.setText("");
                            txtNewPassword.setText("");
                            txtRetypePassword.setText("");
                        });
                        return;
                    }

                    boolean encryptStatus = storage.encryptIdentityKey(txtNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() -> {
                            Toast.makeText(MainActivity.this, getString(R.string.encrypt_identity_fail), Toast.LENGTH_LONG).show();
                            txtCurrentPassword.setText("");
                            txtNewPassword.setText("");
                            txtRetypePassword.setText("");
                        });
                        return;
                    }
                } finally {
                    storage.clear();
                    hideProgressBar();
                }

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        getString(R.string.preferences),
                        Context.MODE_PRIVATE
                );
                long currentId = sharedPref.getLong(getString(R.string.current_id), 0);
                mDbHelper.updateIdentityData(currentId, storage.createSaveData());

                handler.post(() -> {
                    txtCurrentPassword.setText("");
                    txtNewPassword.setText("");
                    txtRetypePassword.setText("");
                });
            }).start();
        });
    }


    public void showClearNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            String CHANNEL_ID = "sqrl_notify_01";

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "SQRL Notification Channel", NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.enableVibration(false);
                notificationChannel.enableLights(false);
                notificationChannel.setSound(null, null);

                notificationManager.createNotificationChannel(notificationChannel);
            }

            long[] v = {};
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_stat_sqrl_logo_vector_outline)
                            .setContentTitle(getString(R.string.notification_identity_unlocked))
                            .setContentText(getString(R.string.notification_identity_unlocked_title))
                            .setAutoCancel(true)
                            .setVibrate(v)
                            .setSound(null)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(getString(R.string.notification_identity_unlocked_desc)));



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
            mBuilder.setSound(Uri.parse("android.resource://"+getPackageName()+"/raw/silence"));

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
            storage.read(identityData);
            btnUnlockIdentity.setEnabled(storage.hasIdentityBlock());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
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
    public void onBackPressed() {
        if (renamePopupWindow != null && renamePopupWindow.isShowing()) {
            renamePopupWindow.dismiss();
        } else if (decryptPopupWindow != null && decryptPopupWindow.isShowing()) {
            decryptPopupWindow.dismiss();
        } else if (loginPopupWindow != null && loginPopupWindow.isShowing()) {
            loginPopupWindow.dismiss();
        } else if (changePasswordPopupWindow != null && changePasswordPopupWindow.isShowing()) {
            changePasswordPopupWindow.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                if(useIdentity) {
                    serverData = EncryptionUtils.readSQRLQRCodeAsString(result.getRawBytes());
                    int indexOfQuery = serverData.indexOf("/", serverData.indexOf("://") + 3);
                    queryLink = serverData.substring(indexOfQuery);
                    final String domain = serverData.split("/")[2];
                    commHandler.setDomain(domain);

                    handler.postDelayed(() -> {
                        final TextView txtSite = loginPopupWindow.getContentView().findViewById(R.id.txtSite);
                        txtSite.setText(domain);
                        loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    }, 100);
                } else {
                    SQRLStorage storage = SQRLStorage.getInstance();
                    byte[] qrCodeData = EncryptionUtils.readSQRLQRCode(result.getRawBytes());
                    try {
                        storage.read(qrCodeData);

                        if(!storage.hasEncryptedKeys()) {
                            Toast.makeText(MainActivity.this, R.string.identity_compressed_format_not_supported, Toast.LENGTH_LONG);
                            if(!mDbHelper.hasIdentities()) {
                                startActivity(new Intent(this, StartActivity.class));
                            }
                            return;
                        }

                        handler.postDelayed(() -> {
                            final TextView txtRecoveryKey = decryptPopupWindow.getContentView().findViewById(R.id.txtRecoveryKey);
                            txtRecoveryKey.setText(storage.getVerifyingRecoveryBlock());
                            txtRecoveryKey.setMovementMethod(LinkMovementMethod.getInstance());

                            decryptPopupWindow.showAtLocation(decryptPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                        }, 100);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
