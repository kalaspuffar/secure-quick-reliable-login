package org.ea.sqrl.activites.create;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;

import android.support.v7.app.AppCompatDelegate;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.CommonBaseActivity;
import org.ea.sqrl.processors.EntropyHarvester;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.services.RescueCodePrintDocumentAdapter;
import org.ea.sqrl.utils.RescueCodeInputHelper;

import java.util.List;

/**
 *
 * @author Daniel Persson
 */
public class RescueCodeShowActivity extends CommonBaseActivity {
    private static final String TAG = "RescueCodeShowActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescuecode_show);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);

        try {
            final EntropyHarvester entropyHarvester = EntropyHarvester.getInstance();
            SQRLStorage storage = SQRLStorage.getInstance(RescueCodeShowActivity.this.getApplicationContext());
            storage.newRescueCode(entropyHarvester);

            List<String> rescueArr = storage.getTempShowableRescueCode();

            final TextView txtRescueCodeShowDescription = findViewById(R.id.txtRescueCodeShowDescription);
            final ViewGroup rootView = findViewById(R.id.rescueCodeShowActivityView);
            final Button btnRescueCodeShowNext = findViewById(R.id.btnRescueCodeShowNext);

            txtRescueCodeShowDescription.setMovementMethod(LinkMovementMethod.getInstance());

            RescueCodeInputHelper rescueCodeInputHelper = new RescueCodeInputHelper(
                    this, rootView, btnRescueCodeShowNext, false);
            rescueCodeInputHelper.setInputEnabled(false);
            rescueCodeInputHelper.setRescueCodeInput(rescueArr);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        findViewById(R.id.btnRescueCodeShowNext).setOnClickListener(v -> {
            this.finish();
            startActivity(new Intent(this, RescueCodeEnterActivity.class));
        });

        findViewById(R.id.btnPrintRescueCode).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                String jobName = getString(R.string.app_name) + " Document";

                PrintAttributes printAttributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .build();

                printManager.print(jobName, new RescueCodePrintDocumentAdapter(this), printAttributes);
            } else {
                showPrintingNotAvailableDialog();
            }
        });
    }

    public void showPrintingNotAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RescueCodeShowActivity.this);
        builder.setTitle(R.string.print_not_available_title)
                .setMessage(getString(R.string.print_not_available_text))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
}
