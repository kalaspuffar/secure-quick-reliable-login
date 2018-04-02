package org.ea.sqrl.activites;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;

import java.util.Map;

public class LoginBaseActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "LoginBaseActivity";
    protected ConstraintLayout rootView;

    protected Handler handler = new Handler();
    protected Spinner cboxIdentity;
    protected Map<Long, String> identities;
    protected Button btnUseIdentity;

    protected FrameLayout progressBarHolder;
    protected PopupWindow loginOptionsPopupWindow;
    private PopupWindow disableAccountPopupWindow;
    private PopupWindow enableAccountPopupWindow;
    private PopupWindow removeAccountPopupWindow;
    protected PopupWindow progressPopupWindow;
    protected PopupWindow loginPopupWindow;

    protected final CommunicationHandler commHandler = CommunicationHandler.getInstance();
    protected String serverData = null;
    protected String queryLink = null;

    protected void setupBasePopups(LayoutInflater layoutInflater) {
        identities = mDbHelper.getIdentitys();

        ArrayAdapter adapter = new ArrayAdapter(
                this,
                R.layout.simple_spinner_item,
                identities.values().toArray(new String[identities.size()])
        );
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setOnItemSelectedListener(this);


        setupProgressPopupWindow(layoutInflater);
        setupEnableAccountPopupWindow(layoutInflater);
        setupDisableAccountPopupWindow(layoutInflater);
        setupRemoveAccountPopupWindow(layoutInflater);
    }

    protected void postQuery(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientQuery(true), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    protected void postCreateAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(
                commHandler.createClientCreateAccount(entropyHarvester),
                serverData
        );
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    protected void postLogin(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientLogin(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    protected void postDisableAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientDisable(), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    protected void postEnableAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientEnable(), serverData, true);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    protected void postRemoveAccount(CommunicationHandler commHandler) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientRemove(), serverData, true);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        toastErrorMessage();
        commHandler.printParams();
    }

    protected void toastErrorMessage() {
        if(commHandler.hasErrorMessage()) {
            handler.post(() ->
                    Snackbar.make(rootView, commHandler.getErrorMessage(this), Snackbar.LENGTH_LONG).show()
            );
        }
    }

    protected void showProgressBar() {
        handler.post(() -> {
            rootView.setEnabled(false);
            AlphaAnimation inAnimation = new AlphaAnimation(0f, 1f);
            inAnimation.setDuration(200);
            progressBarHolder.setAnimation(inAnimation);
            progressBarHolder.setVisibility(View.VISIBLE);
        });
    }

    protected void hideProgressBar() {
        handler.post(() -> {
            rootView.setEnabled(true);
            AlphaAnimation outAnimation = new AlphaAnimation(1f, 0f);
            outAnimation.setDuration(200);
            progressBarHolder.setAnimation(outAnimation);
            progressBarHolder.setVisibility(View.GONE);
        });
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

    public void setupLoginOptionsPopupWindow(LayoutInflater layoutInflater, boolean popup) {
        View popupView = layoutInflater.inflate(R.layout.fragment_login_optional, null);

        loginOptionsPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);
        loginOptionsPopupWindow.setTouchable(true);

        popupView.findViewById(R.id.btnCloseLoginOptional).setOnClickListener(v -> {
            loginOptionsPopupWindow.dismiss();
            if(popup) {
                loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
            }
        });

        popupView.findViewById(R.id.btnRemoveAccount).setOnClickListener(v -> {
            loginOptionsPopupWindow.dismiss();
            removeAccountPopupWindow.showAtLocation(removeAccountPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });

        popupView.findViewById(R.id.btnLockAccount).setOnClickListener(v -> {
            loginOptionsPopupWindow.dismiss();
            disableAccountPopupWindow.showAtLocation(disableAccountPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });

        popupView.findViewById(R.id.btnUnlockAccount).setOnClickListener(v -> {
            loginOptionsPopupWindow.dismiss();
            enableAccountPopupWindow.showAtLocation(enableAccountPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });
    }

    public void setupProgressPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_progress, null);

        progressPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);

        final ProgressBar progressBar = popupView.findViewById(R.id.pbProgress);
        final TextView lblProgressText = popupView.findViewById(R.id.lblProgressText);

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater(handler, progressBar, lblProgressText));
    }

    public void setupDisableAccountPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_disable_account, null);

        disableAccountPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);
        disableAccountPopupWindow.setTouchable(true);

        popupView.findViewById(R.id.btnCloseLogin).setOnClickListener(v -> {
            disableAccountPopupWindow.dismiss();
            loginOptionsPopupWindow.showAtLocation(loginOptionsPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        });


        final EditText txtDisablePassword = popupView.findViewById(R.id.txtDisablePassword);
        popupView.findViewById(R.id.btnDisableAccount).setOnClickListener(v -> {
            disableAccountPopupWindow.dismiss();
            showProgressBar();

            new Thread(() -> {
                boolean decryptionOk = SQRLStorage.getInstance().decryptIdentityKey(txtDisablePassword.getText().toString());
                if(decryptionOk) {
                    showClearNotification();
                } else {
                    Snackbar.make(rootView, getString(R.string.decrypt_identity_fail), Snackbar.LENGTH_LONG).show();
                    return;
                }

                try {
                    postQuery(commHandler);

                    if(
                            (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                                    !commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                            ) {
                        postDisableAccount(commHandler);
                    } else {
                        handler.post(() -> {
                            txtDisablePassword.setText("");
                        });
                        toastErrorMessage();
                    }
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
                    return;
                } finally {
                    hideProgressBar();
                }

                handler.post(() -> {
                    txtDisablePassword.setText("");
                });
            }).start();
        });
    }

    public void setupEnableAccountPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_enable_account, null);

        enableAccountPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        enableAccountPopupWindow.setTouchable(true);

        final EditText txtRecoverCode1 = popupView.findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = popupView.findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = popupView.findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = popupView.findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = popupView.findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = popupView.findViewById(R.id.txtRecoverCode6);

        popupView.findViewById(R.id.btnCloseResetPassword).setOnClickListener(v -> enableAccountPopupWindow.dismiss());
        popupView.findViewById(R.id.btnEnableAccountEnable).setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance();

            if(!checkRescueCode(txtRecoverCode1)) return;
            if(!checkRescueCode(txtRecoverCode2)) return;
            if(!checkRescueCode(txtRecoverCode3)) return;
            if(!checkRescueCode(txtRecoverCode4)) return;
            if(!checkRescueCode(txtRecoverCode5)) return;
            if(!checkRescueCode(txtRecoverCode6)) return;

            handler.post(() -> {
                enableAccountPopupWindow.dismiss();
                progressPopupWindow.showAtLocation(progressPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
            });

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

                    postQuery(commHandler);
                    if(
                            (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                            ) {
                        postEnableAccount(commHandler);
                    } else {
                        toastErrorMessage();
                    }
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    storage.clear();
                    handler.post(() -> {
                        progressPopupWindow.dismiss();
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

    public void setupRemoveAccountPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_remove_account, null);

        removeAccountPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        removeAccountPopupWindow.setTouchable(true);

        final EditText txtRecoverCode1 = popupView.findViewById(R.id.txtRecoverCode1);
        final EditText txtRecoverCode2 = popupView.findViewById(R.id.txtRecoverCode2);
        final EditText txtRecoverCode3 = popupView.findViewById(R.id.txtRecoverCode3);
        final EditText txtRecoverCode4 = popupView.findViewById(R.id.txtRecoverCode4);
        final EditText txtRecoverCode5 = popupView.findViewById(R.id.txtRecoverCode5);
        final EditText txtRecoverCode6 = popupView.findViewById(R.id.txtRecoverCode6);

        popupView.findViewById(R.id.btnCloseResetPassword).setOnClickListener(v -> removeAccountPopupWindow.dismiss());
        popupView.findViewById(R.id.btnRemoveAccountRemove).setOnClickListener(v -> {

            SQRLStorage storage = SQRLStorage.getInstance();

            if(!checkRescueCode(txtRecoverCode1)) return;
            if(!checkRescueCode(txtRecoverCode2)) return;
            if(!checkRescueCode(txtRecoverCode3)) return;
            if(!checkRescueCode(txtRecoverCode4)) return;
            if(!checkRescueCode(txtRecoverCode5)) return;
            if(!checkRescueCode(txtRecoverCode6)) return;

            removeAccountPopupWindow.dismiss();
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

                    postQuery(commHandler);
                    if(
                            (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                            ) {
                        postRemoveAccount(commHandler);
                    } else if(
                            (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                                    commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                                    !commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                            ) {
                        postDisableAccount(commHandler);
                        postRemoveAccount(commHandler);
                    } else {
                        toastErrorMessage();
                    }
                } catch (Exception e) {
                    handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    storage.clear();
                    handler.post(() -> {
                        progressPopupWindow.dismiss();
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

    public void showClearNotification() {
        final String CHANNEL_ID = "sqrl_notify_01";

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
        storage.clearQuickPass(this);
        try {
            byte[] identityData = mDbHelper.getIdentityData(keyArray[pos]);
            storage.read(identityData);
            btnUseIdentity.setEnabled(storage.hasIdentityBlock());

        } catch (Exception e) {
            handler.post(() -> Snackbar.make(rootView, e.getMessage(), Snackbar.LENGTH_LONG).show());
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private int getPosition(long currentId) {
        int i = 0;
        for(Long l : identities.keySet()) {
            if (l == currentId) return i;
            i++;
        }
        return 0;
    }

    @Override
    public void onBackPressed() {
        if (loginOptionsPopupWindow != null && loginOptionsPopupWindow.isShowing()) {
            loginOptionsPopupWindow.dismiss();
        } else if (disableAccountPopupWindow != null && disableAccountPopupWindow.isShowing()) {
            disableAccountPopupWindow.dismiss();
        } else if (enableAccountPopupWindow != null && enableAccountPopupWindow.isShowing()) {
            enableAccountPopupWindow.dismiss();
        } else if (removeAccountPopupWindow != null && removeAccountPopupWindow.isShowing()) {
            removeAccountPopupWindow.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    protected void updateSpinnerData(long currentId) {
        identities = mDbHelper.getIdentitys();

        ArrayAdapter adapter = new ArrayAdapter(
                this,
                R.layout.simple_spinner_item,
                identities.values().toArray(new String[identities.size()])
        );
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setSelection(getPosition(currentId));
    }
}
