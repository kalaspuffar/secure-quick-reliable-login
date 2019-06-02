package org.ea.sqrl.activites.account;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.database.IdentityDBHelper;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.SqrlApplication;

import static android.view.View.GONE;

public class EnableQuickPassActivity extends LoginBaseActivity {
    String TAG = "EnableQuickPassActivity";
    boolean mPopupShown = false;
    boolean mDoingLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
    }

    public void isDoingLogin() {
        mDoingLogin = true;
    }

    public void failedLogin() {
        mDoingLogin = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (!mPopupShown) {
                SQRLStorage storage = SQRLStorage.getInstance(EnableQuickPassActivity.this.getApplicationContext());
                if (!storage.hasQuickPass()) {
                    final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
                    setupLoginPopupWindow(getLayoutInflater());
                    PopupWindow loginOnlyPopupWindow = alterDialogOfLoginPopupWindow(loginPopupWindow);
                    loginOnlyPopupWindow.showAtLocation(viewGroup, Gravity.CENTER, 0, 0);
                    mPopupShown = true;
                } else {
                    Toast.makeText(this, "QuickPass was already active, and still is", Toast.LENGTH_LONG).show(); // unexpected
                    finishAffinity();
                }
            }  else {
                Log.v(TAG, "popup already shown <<<<<<<<<<<<<<<<<<<<<< ");
                if (!mDoingLogin) {
                    Toast.makeText(this, "Cancelled QuickPass Initiation", Toast.LENGTH_LONG).show();
                    Log.v(TAG, "Cancelled toast should show.");
                    finishAffinity();
                } else {
                    Toast.makeText(this, "Enabling QuickPass", Toast.LENGTH_LONG).show();
                    Log.v(TAG, "Enabling toast should show.");

                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.v(TAG, "onBackPressed() NOT CALLED?? ");
    }

    private PopupWindow alterDialogOfLoginPopupWindow(PopupWindow loginPopupWindow) {
        View view = loginPopupWindow.getContentView();

        String identityName = IdentityDBHelper.getInstance(this).getIdentityName(SqrlApplication.getCurrentId(this));
        String quotedIdentityNamePhrase = (identityName == null?"":" for \"" + identityName + "\".");
        TextView instructions = view.findViewById(R.id.textView4);
        instructions.setText("Type your SQRL password here to enable QuickPass" + quotedIdentityNamePhrase);

        TextView heading = view.findViewById(R.id.textView15);
        heading.setText("Enable QuickPass");

        TextView domainPrompt = view.findViewById(R.id.textView3);
        domainPrompt.setText("");

        Button buttonOptions = view.findViewById(R.id.btnLoginOptions);
        buttonOptions.setVisibility(GONE);

        Button buttonLogin = view.findViewById(R.id.btnLogin);
        buttonLogin.setText("Enable");

        return loginPopupWindow;
    }

}
