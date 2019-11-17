package org.ea.sqrl.activites.identity;

import android.os.Bundle;
import android.widget.Button;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.CommonBaseActivity;

public class SupersededIdentityActivity extends CommonBaseActivity {

    private static final String TAG = "SupersededIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_superseded_identity);

        final Button btnOk = findViewById(R.id.btnSupersededIdentityOk);
        btnOk.setOnClickListener(v -> this.finishAffinity());
    }
}
