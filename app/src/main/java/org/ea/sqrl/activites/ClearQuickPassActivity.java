package org.ea.sqrl.activites;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;

public class ClearQuickPassActivity extends BaseActivity {
    public static final String ACTION_CLEAR_QUICK_PASS = "org.ea.sqrl.activites.ClearQuickpassActivity.CLEAR_QUICK_PASS";
    public static final String TAG = "ClearQuickPassActivity";

    boolean mToasted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        SQRLStorage storage = SQRLStorage.getInstance(ClearQuickPassActivity.this.getApplicationContext());

        if (storage.hasQuickPass()) {
            storage.clearQuickPass();
            storage.clear();
        } else {
            mToasted = true;
            Toast.makeText(this, R.string.quickpass_not_active, Toast.LENGTH_LONG).show();
        }

        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SQRLStorage storage = SQRLStorage.getInstance(ClearQuickPassActivity.this.getApplicationContext());
        if (storage.hasQuickPass()) {
            Toast.makeText(this, getResources().getString(R.string.clear_identity_fail), Toast.LENGTH_LONG).show();
        } else {
            if (!mToasted) {
                Toast.makeText(this, getResources().getString(R.string.clear_identity_success), Toast.LENGTH_LONG).show();
            }
        }
        finishAffinity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
    }
}
