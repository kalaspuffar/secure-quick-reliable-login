package org.ea.sqrl.activites;

import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.ea.sqrl.R;
import org.ea.sqrl.database.IdentityDBHelper;
import org.ea.sqrl.utils.SqrlApplication;

import static android.view.View.GONE;

public class EnableQuickPassActivity extends LoginActivity {
    String TAG = "EnableQuickPassActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String identityName = IdentityDBHelper.getInstance(this).getIdentityName(SqrlApplication.getCurrentId(this));
        TextView instructions = rootView.findViewById(R.id.txtLoginDescription);
        instructions.setText(getString(R.string.quickpass_enable_prompt, identityName));

        TextView domainPrompt = rootView.findViewById(R.id.txtLoginHeadline);
        domainPrompt.setText("");

        TextView domainText = rootView.findViewById(R.id.txtSite);
        domainText.setText("");
        domainText.setVisibility(GONE);

        Button buttonLogin = rootView.findViewById(R.id.btnLogin);
        buttonLogin.setText(R.string.enable_text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void closeActivity() {
        Toast.makeText(this, R.string.quickpass_enable_message, Toast.LENGTH_LONG).show();
        super.closeActivity();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, R.string.quickpass_cancelled, Toast.LENGTH_LONG).show();
        super.onBackPressed();
    }
}
