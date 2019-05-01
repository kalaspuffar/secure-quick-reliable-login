package org.ea.sqrl.activites.create;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.ViewGroup;
import android.widget.Button;

import org.ea.sqrl.R;
import org.ea.sqrl.utils.RescueCodeInputHelper;
import org.ea.sqrl.utils.Utils;

/**
 *
 * @author Daniel Persson
 */
public class RescueCodeEnterActivity extends AppCompatActivity {
    private static final String TAG = "RescueCodeEnterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescuecode_enter);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);

        Utils.setLanguage(this);

        ViewGroup rootLayout = findViewById(R.id.rescueCodeEntryActivityView);
        Button btnRescueCodeEnterNext = findViewById(R.id.btnRescueCodeEnterNext);

        RescueCodeInputHelper rescueCodeInputHelper = new RescueCodeInputHelper(this);
        rescueCodeInputHelper.setDisplayErrors(true);
        rescueCodeInputHelper.setStatusChangedListener(successfullyCompleted -> {
            btnRescueCodeEnterNext.setEnabled(successfullyCompleted);
        });
        rescueCodeInputHelper.register(rootLayout, btnRescueCodeEnterNext);

        btnRescueCodeEnterNext.setEnabled(false);
        btnRescueCodeEnterNext.setOnClickListener(v -> {
            this.finish();
            startActivity(new Intent(this, SaveIdentityActivity.class));
        });

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;
    }
}
