package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

public class AccountOptionsActivity extends BaseActivity {
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_options);

        findViewById(R.id.btnRemoveAccount).setOnClickListener(v -> {
            startActivity(new Intent(this, RemoveAccountActivity.class));
        });

        findViewById(R.id.btnLockAccount).setOnClickListener(v -> {
            startActivity(new Intent(this, DisableAccountActivity.class));
        });

        findViewById(R.id.btnUnlockAccount).setOnClickListener(v -> {
            startActivity(new Intent(this, EnableAccountActivity.class));
        });
    }
}
