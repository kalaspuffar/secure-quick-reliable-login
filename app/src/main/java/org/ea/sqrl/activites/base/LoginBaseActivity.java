package org.ea.sqrl.activites.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.constraint.ConstraintLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.account.AccountOptionsActivity;
import org.ea.sqrl.adapter.IdentityAdapter;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;
import java.util.Map;

/**
 *
 * @author Daniel Persson
 */
@SuppressLint("Registered")
public class LoginBaseActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "LoginBaseActivity";
    protected ConstraintLayout rootView;

    protected Spinner cboxIdentity;
    protected TextView txtOneIdentity;
    protected Map<Long, String> identities;
    protected Button btnUseIdentity;

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
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Long[] keyArray = identities.keySet().toArray(new Long[identities.size()]);

        SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                APPS_PREFERENCES,
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(CURRENT_ID, keyArray[pos]);
        editor.apply();

        SQRLStorage storage = SQRLStorage.getInstance(LoginBaseActivity.this.getApplicationContext());

        byte[] identityData = mDbHelper.getIdentityData(keyArray[pos]);

        if(storage.needsReload(identityData)) {
            storage.clearQuickPass();
            try {
                storage.read(identityData);
            } catch (Exception e) {
                showErrorMessage(e.getMessage());
                Log.e(TAG, e.getMessage(), e);
            }
        }

        if(btnUseIdentity != null) {
            btnUseIdentity.setEnabled(storage.hasIdentityBlock());
        }

        this.selectionUpdated();
    }

    protected void selectionUpdated() {}

    @Override
    protected void onPause() {
        super.onPause();
        if(communicationFlowHandler != null) {
            communicationFlowHandler.closeServer();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private int getPosition(long currentId) {
        int i = 0;
        for(long l : identities.keySet()) {
            if (l == currentId) return i;
            i++;
        }
        return 0;
    }

    protected void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected void updateSpinnerData(long currentId) {
        identities = mDbHelper.getIdentities();
        if (identities.size() == 0) return;
        if(currentId == -1) {
            currentId = identities.keySet().iterator().next();
        }

        cboxIdentity.setOnItemSelectedListener(this);
        cboxIdentity.setAdapter(new IdentityAdapter(identities));
        cboxIdentity.setSelection(getPosition(currentId), false);

        String currentIdName = mDbHelper.getIdentityName(currentId);
        txtOneIdentity.setText(currentIdName);

        if (identities.size() > 1) {
            cboxIdentity.setVisibility(View.VISIBLE);
            txtOneIdentity.setVisibility(View.INVISIBLE);
        } else {
            cboxIdentity.setVisibility(View.INVISIBLE);
            txtOneIdentity.setVisibility(View.VISIBLE);
        }
    }

    public void showLoginPopup() {
        loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        lockRotation();
    }

    public void hideLoginPopup() {
        loginPopupWindow.dismiss();
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
                doLogin(storage, txtLoginPassword, false, false, null, LoginBaseActivity.this);
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
                    doLogin(storage, txtLoginPassword, true, false, null, LoginBaseActivity.this);
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
            SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                    APPS_PREFERENCES,
                    Context.MODE_PRIVATE
            );
            long currentId = sharedPref.getLong(CURRENT_ID, 0);

            if(currentId != 0) {
                doLogin(storage, txtLoginPassword, false, false, null, this);
            }
        });
    }

    public void doLogin(SQRLStorage storage, EditText txtLoginPassword, boolean usedQuickpass, boolean usedCps, Activity activityToFinish, Context context) {
        if (!usedCps) hideLoginPopup();
        showProgressPopup();
        closeKeyboard();

        new Thread(() -> {
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
                return;
            }
            clearQuickPassDelayed();

            handler.post(() -> txtLoginPassword.setText(""));

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
    }
}
