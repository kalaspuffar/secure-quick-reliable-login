package org.ea.sqrl.activites;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.services.ClearIdentityReceiver;
import org.ea.sqrl.services.ClearIdentityService;
import org.ea.sqrl.utils.EncryptionUtils;

/**
 * Start activity should be a base for the user so we bring them into the application and they know
 * how to use it when installed and identities are added. So where we add some text for to inform
 * as well as a link to import your first identity.
 *
 * @author Daniel Persson
 */
public class StartActivity extends BaseActivity {
    private static final String TAG = "StartActivity";
    private IntentIntegrator integrator;
    private boolean createNewIdentity = false;
    private ConstraintLayout rootView;
    private Handler handler = new Handler();
    private PopupWindow importPopupWindow;
    private PopupWindow resetPasswordPopupWindow;
    private PopupWindow progressPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final TextView txtWelcomeMessage = findViewById(R.id.txtWelcomeMessage);
        txtWelcomeMessage.setMovementMethod(LinkMovementMethod.getInstance());

        rootView = findViewById(R.id.startActivityView);

        integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(this.getString(R.string.button_scan_secret));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setBarcodeImageEnabled(false);

        setupProgressPopupWindow(getLayoutInflater());
        setupImportPopupWindow(getLayoutInflater());
        setupResetPasswordPopupWindow(getLayoutInflater());
        setupProgressPopupWindow(getLayoutInflater());

        final Button btnScanSecret = findViewById(R.id.btnScanSecret);
        btnScanSecret.setOnClickListener(v -> {
            createNewIdentity = false;
            this.showPhoneStatePermission();
        });

        final Button btnStartCreateIdentity = findViewById(R.id.btnStartCreateIdentity);
        btnStartCreateIdentity.setOnClickListener(v -> {
            createNewIdentity = true;
            this.showPhoneStatePermission();
        });
    }

    protected void setupProgressPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_progress, null);

        progressPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);


        final ProgressBar progressBar = popupView.findViewById(R.id.pbEntropy);
        final TextView lblProgressTitle = popupView.findViewById(R.id.lblProgressTitle);
        final TextView lblProgressText = popupView.findViewById(R.id.lblProgressText);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, lblProgressTitle, progressBar, lblProgressText));
    }

    public void setupImportPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_import, null);

        importPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        importPopupWindow.setTouchable(true);

        final EditText txtPassword = popupView.findViewById(R.id.txtPassword);
        final Button btnImportIdentityDo = popupView.findViewById(R.id.btnImportIdentityDo);

        popupView.findViewById(R.id.btnCloseImportIdentity).setOnClickListener(v -> importPopupWindow.dismiss());
        popupView.findViewById(R.id.btnForgotPassword).setOnClickListener(
                v -> {
                    importPopupWindow.dismiss();
                    resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                }
        );

        btnImportIdentityDo.setOnClickListener(v -> {
            importPopupWindow.dismiss();
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                SQRLStorage storage = SQRLStorage.getInstance();
                try {
                    boolean decryptStatus = storage.decryptIdentityKey(txtPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        return;
                    }
                    showClearNotification();

                    boolean encryptStatus = storage.encryptIdentityKey(txtPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() -> {
                            Snackbar.make(rootView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                            txtPassword.setText("");
                            progressPopupWindow.dismiss();
                        });
                        return;
                    }
                    storage.clear();
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
                }

                long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                mDbHelper.updateIdentityName(newIdentityId, "Default");

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                        APPS_PREFERENCES,
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(CURRENT_ID, newIdentityId);
                editor.apply();

                handler.post(() -> {
                    txtPassword.setText("");
                    progressPopupWindow.dismiss();
                });
            }).start();
        });
    }

    public void showClearNotification() {
        final String CHANNEL_ID = "sqrl_notify_01";

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "SQRL Notification Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableVibration(false);
            notificationChannel.enableLights(false);
            notificationChannel.setSound(null, null);

            if(notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
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

        if(mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION_IDENTITY_UNLOCKED, mBuilder.build());
        }

        long delayMillis = SQRLStorage.getInstance().getIdleTimeout() * 60000;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobInfo jobInfo = new JobInfo.Builder(ClearIdentityService.JOB_NUMBER, new ComponentName(this, ClearIdentityService.class))
                    .setMinimumLatency(delayMillis).build();

            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(jobScheduler != null) jobScheduler.schedule(jobInfo);
        } else {
            AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

            Intent intent = new Intent(getApplicationContext(), ClearIdentityReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

            int SDK_INT = Build.VERSION.SDK_INT;
            long timeInMillis = System.currentTimeMillis() + delayMillis;

            if(alarmManager == null) return;

            if (SDK_INT < Build.VERSION_CODES.KITKAT) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        }
    }

    protected boolean checkRescueCode(EditText code) {
        if(code.length() != 4) {
            Snackbar.make(rootView, getString(R.string.rescue_code_incorrect_input), Snackbar.LENGTH_LONG).show();
            code.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(code.getText().toString());
        } catch (NumberFormatException nfe) {
            Snackbar.make(rootView, getString(R.string.rescue_code_incorrect_input), Snackbar.LENGTH_LONG).show();
            code.requestFocus();
            return false;
        }
        return true;
    }

    public void setupResetPasswordPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_reset_password, null);

        resetPasswordPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        resetPasswordPopupWindow.setTouchable(true);

        final EditText txtRecoverCode1 = popupView.findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = popupView.findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = popupView.findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = popupView.findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = popupView.findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = popupView.findViewById(R.id.txtRecoverCode6);
        final EditText txtResetPasswordNewPassword = popupView.findViewById(R.id.txtResetPasswordNewPassword);

        popupView.findViewById(R.id.btnCloseResetPassword).setOnClickListener(v -> resetPasswordPopupWindow.dismiss());
        popupView.findViewById(R.id.btnResetPassword).setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance();

            if(!checkRescueCode(txtRecoverCode1)) return;
            if(!checkRescueCode(txtRecoverCode2)) return;
            if(!checkRescueCode(txtRecoverCode3)) return;
            if(!checkRescueCode(txtRecoverCode4)) return;
            if(!checkRescueCode(txtRecoverCode5)) return;
            if(!checkRescueCode(txtRecoverCode6)) return;

            resetPasswordPopupWindow.dismiss();
            progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);

            new Thread(() -> {
                try {
                    String rescueCode = txtRecoverCode1.getText().toString();
                    rescueCode += txtRecoverCode2.getText().toString();
                    rescueCode += txtRecoverCode3.getText().toString();
                    rescueCode += txtRecoverCode4.getText().toString();
                    rescueCode += txtRecoverCode5.getText().toString();
                    rescueCode += txtRecoverCode6.getText().toString();

                    boolean decryptionOk = storage.decryptUnlockKey(rescueCode);
                    if (!decryptionOk) {
                        handler.post(() ->
                                Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show()
                        );
                        return;
                    }

                    storage.reInitializeMasterKeyIdentity();

                    boolean encryptStatus = storage.encryptIdentityKey(txtResetPasswordNewPassword.getText().toString(), entropyHarvester);
                    if (!encryptStatus) {
                        handler.post(() ->
                                Snackbar.make(rootView, getString(R.string.encrypt_identity_fail), Snackbar.LENGTH_LONG).show()
                        );
                        return;
                    }
                    storage.clear();

                    SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                            APPS_PREFERENCES,
                            Context.MODE_PRIVATE
                    );

                    long newIdentityId = mDbHelper.newIdentity(storage.createSaveData());
                    mDbHelper.updateIdentityName(newIdentityId, "Default");

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putLong(CURRENT_ID, newIdentityId);
                    editor.apply();

                    handler.post(() -> {
                        importPopupWindow.dismiss();
                    });
                } finally {
                    storage.clear();
                    handler.post(() -> {
                        progressPopupWindow.dismiss();
                        txtResetPasswordNewPassword.setText("");
                        txtRecoverCode1.setText("");
                        txtRecoverCode2.setText("");
                        txtRecoverCode3.setText("");
                        txtRecoverCode4.setText("");
                        txtRecoverCode5.setText("");
                        txtRecoverCode6.setText("");
                    });
                }
            }).start();
        });
    }

    @Override
    protected void permissionOkCallback() {
        if(createNewIdentity) {
            startActivity(new Intent(this, CreateIdentityActivity.class));
        } else {
            integrator.initiateScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d(TAG, "Cancelled scan");
                Snackbar.make(rootView, R.string.scan_cancel, Snackbar.LENGTH_LONG).show();
            } else {
                SQRLStorage storage = SQRLStorage.getInstance();
                byte[] qrCodeData = EncryptionUtils.readSQRLQRCode(result.getRawBytes());
                if(qrCodeData.length == 0) {
                    handler.post(() -> Snackbar.make(rootView, R.string.scan_incorrect, Snackbar.LENGTH_LONG).show());
                    return;
                }
                try {
                    storage.read(qrCodeData);

                    if(!storage.hasEncryptedKeys()) {
                        handler.postDelayed(() -> {
                            resetPasswordPopupWindow.showAtLocation(resetPasswordPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                        }, 100);
                        return;
                    }

                    String recoveryBlock = storage.getVerifyingRecoveryBlock();

                    handler.postDelayed(() -> {
                        final TextView txtRecoveryKey = importPopupWindow.getContentView().findViewById(R.id.txtRecoveryKey);
                        txtRecoveryKey.setText(recoveryBlock);
                        txtRecoveryKey.setMovementMethod(LinkMovementMethod.getInstance());

                        importPopupWindow.showAtLocation(importPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
                    }, 100);
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
    }

 }
