package org.ea.sqrl.activites.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.account.AccountOptionsActivity;
import org.ea.sqrl.activites.EnableQuickPassActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.SqrlApplication;

import java.util.Map;

/**
 *
 * @author Daniel Persson
 */
@SuppressLint("Registered")
public class LoginBaseActivity extends BaseActivity {
    private static final String TAG = "LoginBaseActivity";
    protected ConstraintLayout rootView;
    protected Map<Long, String> identities;

    protected PopupWindow loginPopupWindow;
    protected CommunicationFlowHandler communicationFlowHandler = null;


    protected void setupBasePopups(LayoutInflater layoutInflater, boolean urlBasedLogin) {
        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        communicationFlowHandler.setupAskPopupWindow(layoutInflater, handler);
        communicationFlowHandler.setupErrorPopupWindow(layoutInflater);
        communicationFlowHandler.setUrlBasedLogin(urlBasedLogin);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupProgressPopupWindow(getLayoutInflater());
    }

    protected void closeActivity() {}

    @Override
    protected void onPause() {
        super.onPause();
        if(communicationFlowHandler != null) {
            communicationFlowHandler.closeServer();
        }
    }

    protected void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void showLoginPopup() {
        loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        lockRotation();
    }

    public void hideLoginPopup() {
        if (loginPopupWindow != null) loginPopupWindow.dismiss();
        unlockRotation();
    }

    public void setupLoginPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_login, null);

        loginPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        final SQRLStorage storage = SQRLStorage.getInstance(LoginBaseActivity.this.getApplicationContext());

        loginPopupWindow.setTouchable(true);
        final EditText txtLoginPassword = popupView.findViewById(R.id.txtLoginPassword);
        txtLoginPassword.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_DONE){
                if(LoginBaseActivity.this instanceof EnableQuickPassActivity) {
                    ((EnableQuickPassActivity)LoginBaseActivity.this).doingLogin();
                }

                doLogin(storage, txtLoginPassword, false, false, true, null, LoginBaseActivity.this);
                return true;
            }
            return false;
        });

        txtLoginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence password, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence password, int start, int before, int count) {
                if (!storage.hasQuickPass()) return;
                if ((start + count) >= storage.getHintLength()) {
                    if(LoginBaseActivity.this instanceof EnableQuickPassActivity) {
                        ((EnableQuickPassActivity)LoginBaseActivity.this).doingLogin();
                    }

                    doLogin(storage, txtLoginPassword, true, false, true, null, LoginBaseActivity.this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        popupView.findViewById(R.id.btnCloseLogin).setOnClickListener(v -> hideLoginPopup());
        popupView.findViewById(R.id.btnLoginOptions).setOnClickListener(v -> {
            hideLoginPopup();
            startActivity(new Intent(this, AccountOptionsActivity.class));
        });

        popupView.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            long currentId = SqrlApplication.getCurrentId(this.getApplication());

            if(currentId != 0) {
                if(LoginBaseActivity.this instanceof EnableQuickPassActivity) {
                    ((EnableQuickPassActivity)LoginBaseActivity.this).doingLogin();
                }
                doLogin(storage, txtLoginPassword, false, false, true, null, this);
            }
        });
    }

    public void doLogin(SQRLStorage storage, EditText txtLoginPassword, boolean usedQuickpass,
                        boolean usedCps, boolean needsDecryption, Activity activityToFinish, Context context) {
        handler.post(() -> {
            if (!usedCps) hideLoginPopup();
            showProgressPopup();
            closeKeyboard();

            new Thread(() -> {
                if (needsDecryption) {
                    boolean decryptionOk = storage.decryptIdentityKey(txtLoginPassword.getText().toString(), entropyHarvester, usedQuickpass);
                    if(!decryptionOk) {
                        showErrorMessage(R.string.decrypt_identity_fail, () -> {
                            if (!usedCps) {
                                showLoginPopup();
                            }
                        });
                        handler.post(() -> {
                            txtLoginPassword.setHint(R.string.login_identity_password);
                            txtLoginPassword.setText("");
                            hideProgressPopup();
                        });
                        storage.clear();
                        storage.clearQuickPass();
                        if(LoginBaseActivity.this instanceof EnableQuickPassActivity) {
                            ((EnableQuickPassActivity)LoginBaseActivity.this).failedLogin();
                        }
                        return;
                    }
                }

                clearQuickPassDelayed();

                handler.post(() -> txtLoginPassword.setText(""));

                if (context instanceof EnableQuickPassActivity) {
                    storage.clear();
                    handler.post(() -> {
                        hideProgressPopup();
                        closeActivity();
                        EnableQuickPassActivity enableQuickPassActivity = (EnableQuickPassActivity)context;
                        enableQuickPassActivity.finish();
                    });
                    return;
                }

                if (usedCps) {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN_CPS);
                } else {
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);
                }

                communicationFlowHandler.setDoneAction(() -> {
                    storage.clear();
                    handler.post(() -> {
                        hideProgressPopup();
                        closeActivity();
                    });
                    if (activityToFinish != null) activityToFinish.finish();
                });

                communicationFlowHandler.setErrorAction(() -> {
                    storage.clear();
                    handler.post(() -> hideProgressPopup());
                });

                communicationFlowHandler.handleNextAction();
            }).start();
        }) ;
    }
}
