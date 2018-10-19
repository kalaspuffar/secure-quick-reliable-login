package org.ea.sqrl.activites.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ea.sqrl.BuildConfig;
import org.ea.sqrl.R;
import org.ea.sqrl.activites.LanguageActivity;
import org.ea.sqrl.activites.identity.ClearIdentityActivity;
import org.ea.sqrl.activites.IntroductionActivity;
import org.ea.sqrl.database.IdentityDBHelper;
import org.ea.sqrl.processors.EntropyHarvester;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.services.ClearIdentityReceiver;
import org.ea.sqrl.services.ClearIdentityService;
import org.ea.sqrl.utils.Utils;

/**
 * This base activity is inherited by all other activities. We place logic used for menus,
 * background processes and other things that are untied to the current context of the application.
 *
 * @author Daniel Persson
 */
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected static final String CURRENT_ID = "current_id";
    protected static final String APPS_PREFERENCES = "org.ea.sqrl.preferences";
    protected static final String EXPORT_WITHOUT_PASSWORD = "export_without_password";

    private final int REQUEST_PERMISSION_CAMERA = 1;

    protected Handler handler = new Handler();

    private PopupWindow cameraAccessPopupWindow;
    private PopupWindow errorPopupWindow;
    protected PopupWindow progressPopupWindow;

    private TextView txtErrorMessage;

    public static final int NOTIFICATION_IDENTITY_UNLOCKED = 1;

    protected final IdentityDBHelper mDbHelper;
    protected EntropyHarvester entropyHarvester;

    private boolean openedLanguageDialog = false;

    public BaseActivity() {
        mDbHelper = new IdentityDBHelper(this);
        try {
            entropyHarvester = EntropyHarvester.getInstance();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setLanguage(this);
        if(openedLanguageDialog) {
            this.recreate();
            openedLanguageDialog = false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("progressWindowOpen", progressPopupWindow.isShowing());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        boolean progressWindowOpen = savedInstanceState.getBoolean("progressWindowOpen", false);
        savedInstanceState.putBoolean("progressWindowOpen", false);
        if(progressWindowOpen) {
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(BaseActivity.this, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(BaseActivity.this);
            }
            builder.setTitle(R.string.progress_interrupted_title)
                    .setMessage(R.string.progress_interrupted_text)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_default, menu);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            menu.findItem(R.id.action_language).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                startActivity(new Intent(this, IntroductionActivity.class));
                return true;
            case R.id.action_language:
                openedLanguageDialog = true;
                startActivity(new Intent(this, LanguageActivity.class));
                return true;
            case R.id.action_about:
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(BaseActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(BaseActivity.this);
                }
                builder.setTitle(R.string.about_message_title)
                        .setMessage(getString(R.string.about_message_text, BuildConfig.VERSION_NAME))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();

                return true;
        }
        return super.onOptionsItemSelected(item);
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

    protected void setupCameraAccessPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_camera_permission, null);

        cameraAccessPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);

        final Button btnCameraPermissionOk = popupView.findViewById(R.id.btnCameraPermissionOk);
        btnCameraPermissionOk.setOnClickListener(v -> {
            requestPermission();
            cameraAccessPopupWindow.dismiss();
        });
    }

    protected void permissionOkCallback() {

    }

    protected void showPhoneStatePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                cameraAccessPopupWindow.showAtLocation(cameraAccessPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
            } else {
                requestPermission();
            }
        } else {
            permissionOkCallback();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionOkCallback();
                }
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                BaseActivity.this,
                new String[] {Manifest.permission.CAMERA},
                REQUEST_PERMISSION_CAMERA
        );
    }

    public void setupErrorPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_error_dialog, null);

        errorPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);


        txtErrorMessage = popupView.findViewById(R.id.txtErrorMessage);
        final Button btnErrorOk = popupView.findViewById(R.id.btnErrorOk);

        btnErrorOk.setOnClickListener(v -> {
            errorPopupWindow.dismiss();
        });
    }

    public void showErrorMessage(int id) {
        showErrorMessageInternal(getString(id));
    }

    public void showErrorMessage(String message) {
        showErrorMessageInternal(message);
    }

    public void showErrorMessageInternal(String message) {
        if(errorPopupWindow.getContentView() != null) {
            txtErrorMessage.setText(message);
            handler.post(() ->
                    errorPopupWindow.showAtLocation(errorPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
            );
            return;
        }

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(BaseActivity.this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(BaseActivity.this);
        }
        builder.setTitle(R.string.error_dialog_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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
}
