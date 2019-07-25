package org.ea.sqrl.activites.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.ea.sqrl.processors.CommunicationFlowHandler;

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
            communicationFlowHandler.closeCPSServer();
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
