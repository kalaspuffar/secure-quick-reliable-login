package org.ea.sqrl.activites.base;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.account.AccountOptionsActivity;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.ProgressionUpdater;
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
    protected Map<Long, String> identities;
    protected Button btnUseIdentity;

    protected PopupWindow loginPopupWindow;
    protected CommunicationFlowHandler communicationFlowHandler = null;


    protected void setupBasePopups(LayoutInflater layoutInflater, boolean urlBasedLogin) {
        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        if(cboxIdentity != null) {
            identities = mDbHelper.getIdentitys();

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    R.layout.simple_spinner_item,
                    identities.values().toArray(new String[identities.size()])
            );
            adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            cboxIdentity.setAdapter(adapter);
            cboxIdentity.setOnItemSelectedListener(this);
        }

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

        SQRLStorage storage = SQRLStorage.getInstance();

        byte[] identityData = mDbHelper.getIdentityData(keyArray[pos]);

        if(storage.needsReload(identityData)) {
            storage.clearQuickPass(this);
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
    }

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
        identities = mDbHelper.getIdentitys();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.simple_spinner_item,
                identities.values().toArray(new String[identities.size()])
        );
        cboxIdentity.setAdapter(adapter);
        cboxIdentity.setSelection(getPosition(currentId), false);
    }

    public void showLoginPopup() {
        loginPopupWindow.showAtLocation(loginPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
        lockRotation();
    }

    public void hideLoginPopup() {
        loginPopupWindow.dismiss();
        unlockRotation();
    }

    public void setupLoginPopupWindow(LayoutInflater layoutInflater, Context quickPassContext) {
        View popupView = layoutInflater.inflate(R.layout.fragment_login, null);

        loginPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        final SQRLStorage storage = SQRLStorage.getInstance();

        loginPopupWindow.setTouchable(true);
        final EditText txtLoginPassword = popupView.findViewById(R.id.txtLoginPassword);

        txtLoginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence password, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence password, int start, int before, int count) {
                if (!storage.hasQuickPass()) return;
                if ((start + count) >= storage.getHintLength()) {
                    hideLoginPopup();
                    showProgressPopup();
                    closeKeyboard();

                    new Thread(() -> {
                        boolean decryptionOk = storage.decryptIdentityKey(password.toString(), entropyHarvester, true);
                        if(!decryptionOk) {
                            showErrorMessage(R.string.decrypt_identity_fail);
                            handler.post(() -> {
                                txtLoginPassword.setHint(R.string.login_identity_password);
                                txtLoginPassword.setText("");
                                hideProgressPopup();
                                showLoginPopup();
                            });
                            storage.clear();
                            storage.clearQuickPass(quickPassContext);
                            return;
                        }
                        showClearNotification();

                        handler.post(() -> txtLoginPassword.setText(""));

                        communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                        communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);

                        communicationFlowHandler.setDoneAction(() -> {
                            storage.clear();
                            handler.post(() -> {
                                hideProgressPopup();
                                closeActivity();
                            });
                        });

                        communicationFlowHandler.setErrorAction(() -> {
                            storage.clear();
                            storage.clearQuickPass(quickPassContext);
                            handler.post(() -> hideProgressPopup());
                        });

                        communicationFlowHandler.handleNextAction();

                    }).start();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        txtLoginPassword.setOnLongClickListener(view -> {
            StringBuilder longClickResultMessage = new StringBuilder("Password paste failed.");
            ClipboardManager cm = (ClipboardManager)getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
            Log.v(TAG, "long click detected with " + cm.getPrimaryClip().getItemCount() + " items.");
            if (cm.getPrimaryClip().getItemCount() > 0) {
                ClipData.Item clipItem  = cm.getPrimaryClip().getItemAt(0);
                CharSequence clipItemText = clipItem.getText();
                if (clipItemText.length() < 3) {
                    longClickResultMessage.append(" The clipboard item was too short to be a password.");
                } else {
                    txtLoginPassword.setText(clipItem.getText());
                    Log.v(TAG, "Successfully pasted password.");
                    Toast.makeText(quickPassContext, "Pasted clipboard contents", Toast.LENGTH_SHORT).show();
                    return true;
                }
            } else {
                longClickResultMessage.append(" The clipboard was empty.");
            }
            Log.v(TAG, longClickResultMessage.toString());
            return false;
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
                hideLoginPopup();
                showProgressPopup();
                closeKeyboard();

                new Thread(() -> {
                    boolean decryptionOk = storage.decryptIdentityKey(txtLoginPassword.getText().toString(), entropyHarvester, false);
                    if(!decryptionOk) {
                        showErrorMessage(R.string.decrypt_identity_fail);
                        handler.post(() -> {
                            txtLoginPassword.setText("");
                            hideProgressPopup();
                        });
                        storage.clearQuickPass(quickPassContext);
                        storage.clear();
                        return;
                    }
                    showClearNotification();

                    handler.post(() -> txtLoginPassword.setText(""));

                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);

                    communicationFlowHandler.setDoneAction(() -> {
                        storage.clear();
                        handler.post(() -> {
                            hideProgressPopup();
                            closeActivity();
                        });
                    });

                    communicationFlowHandler.setErrorAction(() -> {
                        storage.clear();
                        storage.clearQuickPass(quickPassContext);
                        handler.post(() -> hideProgressPopup());
                    });

                    communicationFlowHandler.handleNextAction();

                }).start();
            }
        });
    }
}
