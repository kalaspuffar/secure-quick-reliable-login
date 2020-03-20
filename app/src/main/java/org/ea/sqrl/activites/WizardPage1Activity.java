package org.ea.sqrl.activites;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.activites.create.CreateIdentityActivity;
import org.ea.sqrl.activites.identity.ImportActivity;
import org.ea.sqrl.activites.identity.ImportOptionsActivity;
import org.ea.sqrl.utils.Utils;

import java.util.Objects;

/**
 * Start wizard.....
 *
 * @author Daniel Persson
 */
public class WizardPage1Activity extends Activity {
    private static final String TAG = "WizardPage1Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setLanguage(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.startup_wizard_page_1);

        TextView secureText = findViewById(R.id.wizard_secure);
        Spannable secureSpan = new SpannableString("Secure");
        secureSpan.setSpan(new ForegroundColorSpan(Color.YELLOW), 1, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        secureText.setText(secureSpan);

        TextView quickText = findViewById(R.id.wizard_quick);
        Spannable quickSpan = new SpannableString("Quick");
        quickSpan.setSpan(new ForegroundColorSpan(Color.YELLOW), 1, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        quickText.setText(quickSpan);

        TextView reliableText = findViewById(R.id.wizard_reliable);
        Spannable reliableSpan = new SpannableString("Reliable");
        reliableSpan.setSpan(new ForegroundColorSpan(Color.YELLOW), 1, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        reliableText.setText(reliableSpan);

        TextView loginText = findViewById(R.id.wizard_login);
        Spannable loginSpan = new SpannableString("Login");
        loginSpan.setSpan(new ForegroundColorSpan(Color.YELLOW), 1, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginText.setText(loginSpan);

        Button next = findViewById(R.id.wizard_next);
        next.setOnClickListener((a) -> {
            startActivity(new Intent(this, WizardPage2Activity.class));
        });
        Button skip = findViewById(R.id.wizard_skip);
        skip.setOnClickListener((a) -> {
            startActivity(new Intent(this, StartActivity.class));
        });
    }
}
