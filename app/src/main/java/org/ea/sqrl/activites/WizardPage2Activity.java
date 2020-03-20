package org.ea.sqrl.activites;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.utils.Utils;

import java.util.List;

/**
 * Start wizard.....
 *
 * @author Daniel Persson
 */
public class WizardPage2Activity extends Activity {
    private static final String TAG = "WizardPage1Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setLanguage(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.startup_wizard_page_2);

        TextView wizardText = findViewById(R.id.wizard_text);
        String s = (String)wizardText.getText();
        Spannable textSpan = Utils.getSpanWithHighlight(s);
        wizardText.setText(textSpan);

        Button next = findViewById(R.id.wizard_next);
        next.setOnClickListener((a) -> {
            startActivity(new Intent(this, WizardPage3Activity.class));
        });
        Button skip = findViewById(R.id.wizard_skip);
        skip.setOnClickListener((a) -> {
            startActivity(new Intent(this, StartActivity.class));
        });
    }
}
