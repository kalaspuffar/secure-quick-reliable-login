package org.ea.sqrl.activites.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;
import java.util.Map;

/**
 *
 * @author Daniel Persson
 */
public class LoginBaseActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "LoginBaseActivity";
    protected ConstraintLayout rootView;

    protected Handler handler = new Handler();
    protected Spinner cboxIdentity;
    protected Map<Long, String> identities;
    protected Button btnUseIdentity;

    protected PopupWindow progressPopupWindow;
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
        for(Long l : identities.keySet()) {
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
        cboxIdentity.setSelection(getPosition(currentId));
    }
}
