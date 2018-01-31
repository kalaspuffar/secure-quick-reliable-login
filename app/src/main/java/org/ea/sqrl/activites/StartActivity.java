package org.ea.sqrl.activites;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.ea.sqrl.R;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final Button btnScanSecret = findViewById(R.id.btnScanSecret);
        btnScanSecret.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        Intent intent = new Intent(StartActivity.this, ScanActivity.class);
                        intent.putExtra(ScanActivity.SCAN_MODE_MESSAGE, ScanActivity.SCAN_MODE_SECRET);
                        startActivity(intent);
                    }
                }).start();
            }
        });
    }
}
