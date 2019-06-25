package org.ea.sqrl.activites.create;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.ViewGroup;
import android.widget.Button;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.CommonBaseActivity;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.RescueCodeInputHelper;

/**
 *
 * @author Daniel Persson
 */
public class RescueCodeEnterActivity extends CommonBaseActivity {
    private static final String TAG = "RescueCodeEnterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescuecode_enter);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);

        ViewGroup rootLayout = findViewById(R.id.rescueCodeEntryActivityView);
        Button btnRescueCodeEnterNext = findViewById(R.id.btnRescueCodeEnterNext);

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        RescueCodeInputHelper rescueCodeInputHelper = new RescueCodeInputHelper(
                this, rootLayout, btnRescueCodeEnterNext, true);
        rescueCodeInputHelper.setStatusChangedListener(successfullyCompleted -> {
            btnRescueCodeEnterNext.setEnabled(successfullyCompleted);
        });

        btnRescueCodeEnterNext.setEnabled(false);
        btnRescueCodeEnterNext.setOnClickListener(v -> {
            this.finish();
            startActivity(new Intent(this, SaveIdentityActivity.class));
        });
    }
}
