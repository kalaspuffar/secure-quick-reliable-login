package org.ea.sqrl.activites;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.EntropyHarvester;
import org.ea.sqrl.processors.SQRLStorage;

import java.util.List;

public class RescueCodeShowActivity extends AppCompatActivity {
    private static final String TAG = "RescueCodeShowActivity";

    private EntropyHarvester entropyHarvester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescuecode_show);

        try {
            entropyHarvester = EntropyHarvester.getInstance();
            SQRLStorage storage = SQRLStorage.getInstance();
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

        final Button btnEntropyGatherNext = findViewById(R.id.btnRescueCodeShowNext);
        btnEntropyGatherNext.setOnClickListener(v -> {
            this.finish();
            startActivity(new Intent(this, RescueCodeEnterActivity.class));
        });
    }
}
