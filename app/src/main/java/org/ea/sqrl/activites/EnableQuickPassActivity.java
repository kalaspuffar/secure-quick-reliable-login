package org.ea.sqrl.activites;

import android.os.Bundle;
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
    }

    public void doingLogin() {
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
                String identityName = IdentityDBHelper.getInstance(this).getIdentityName(SqrlApplication.getCurrentId(this));
                if ("".equals(identityName)) {
                    Toast.makeText(this, R.string.identity_required, Toast.LENGTH_LONG).show(); // unexpected
                    finishAffinity();
                } else if (!storage.hasQuickPass()) {
                    final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
                    setupLoginPopupWindow(getLayoutInflater());
                    PopupWindow loginOnlyPopupWindow = alterDialogOfLoginPopupWindow(loginPopupWindow);
                    loginOnlyPopupWindow.showAtLocation(viewGroup, Gravity.CENTER, 0, 0);
                    mPopupShown = true;
                } else {
                    Toast.makeText(this, R.string.quickpass_already_active, Toast.LENGTH_LONG).show(); // unexpected
                    finishAffinity();
                }
            }  else {
                if (!mDoingLogin) {
                    Toast.makeText(this, R.string.quickpass_cancelled, Toast.LENGTH_LONG).show();
                    finishAffinity();
                } else {
                    Toast.makeText(this, R.string.quickpass_enable_heading, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private PopupWindow alterDialogOfLoginPopupWindow(PopupWindow loginPopupWindow) {
        View view = loginPopupWindow.getContentView();

        String identityName = IdentityDBHelper.getInstance(this).getIdentityName(SqrlApplication.getCurrentId(this));
        TextView instructions = view.findViewById(R.id.textView4);
        instructions.setText(getString(R.string.quickpass_enable_prompt, identityName));

        TextView heading = view.findViewById(R.id.textView15);
        heading.setText(R.string.quickpass_enable_heading);

        TextView domainPrompt = view.findViewById(R.id.textView3);
        domainPrompt.setText("");

        TextView domainText = view.findViewById(R.id.txtSite);
        domainText.setText("");
        domainText.setVisibility(GONE);

        Button buttonOptions = view.findViewById(R.id.btnLoginOptions);
        buttonOptions.setVisibility(GONE);

        Button buttonLogin = view.findViewById(R.id.btnLogin);
        buttonLogin.setText(R.string.enable_text);

        return loginPopupWindow;
    }

}
