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

    protected CommunicationFlowHandler communicationFlowHandler = null;


    protected void setupBasePopups(LayoutInflater layoutInflater) {
        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        communicationFlowHandler.setupAskPopupWindow(layoutInflater, handler);
        communicationFlowHandler.setupErrorPopupWindow(layoutInflater);
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
}
