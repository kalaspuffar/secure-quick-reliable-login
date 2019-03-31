package org.ea.sqrl.activites.create;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.EntropyHarvester;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.services.RescueCodePrintDocumentAdapter;
import org.ea.sqrl.utils.Utils;

import java.util.List;

/**
 *
 * @author Daniel Persson
 */
public class RescueCodeShowActivity extends AppCompatActivity {
    private static final String TAG = "RescueCodeShowActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescuecode_show);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);

        Utils.setLanguage(this);

        try {
            final EntropyHarvester entropyHarvester = EntropyHarvester.getInstance();
            SQRLStorage storage = SQRLStorage.getInstance(RescueCodeShowActivity.this.getApplicationContext());
            storage.newRescueCode(entropyHarvester);

            List<String> rescueArr = storage.getTempShowableRescueCode();

            final TextView txtRecoverCode1 = findViewById(R.id.txtRecoverCode1);
            final TextView txtRecoverCode2 = findViewById(R.id.txtRecoverCode2);
            final TextView txtRecoverCode3 = findViewById(R.id.txtRecoverCode3);
            final TextView txtRecoverCode4 = findViewById(R.id.txtRecoverCode4);
            final TextView txtRecoverCode5 = findViewById(R.id.txtRecoverCode5);
            final TextView txtRecoverCode6 = findViewById(R.id.txtRecoverCode6);

            txtRecoverCode1.setText(rescueArr.get(0));
            txtRecoverCode2.setText(rescueArr.get(1));
            txtRecoverCode3.setText(rescueArr.get(2));
            txtRecoverCode4.setText(rescueArr.get(3));
            txtRecoverCode5.setText(rescueArr.get(4));
            txtRecoverCode6.setText(rescueArr.get(5));

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
