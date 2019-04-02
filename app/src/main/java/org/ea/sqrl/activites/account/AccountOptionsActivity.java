package org.ea.sqrl.activites.account;

import android.content.Intent;
import android.os.Bundle;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;

public class AccountOptionsActivity extends BaseActivity {

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

        findViewById(R.id.btnAlternativeIdentity).setOnClickListener(v -> {
            AccountOptionsActivity.this.finish();
            startActivity(new Intent(this, AlternativeLoginActivity.class));
        });
    }
}
