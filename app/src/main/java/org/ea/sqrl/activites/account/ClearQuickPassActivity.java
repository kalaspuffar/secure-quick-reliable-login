package org.ea.sqrl.activites.account;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;

public class ClearQuickPassActivity extends BaseActivity {
    public static final String ACTION_CLEAR_QUICK_PASS = "org.ea.sqrl.activites.account.ClearQuickpassActivity.CLEAR_QUICK_PASS";
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
            Toast.makeText(this, "Quickpass was not and is not active", Toast.LENGTH_LONG).show();
        }

        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SQRLStorage storage = SQRLStorage.getInstance(ClearQuickPassActivity.this.getApplicationContext());
        if (storage.hasQuickPass()) {
            Toast.makeText(this, getResources().getString(R.string.clear_identity_fail), Toast.LENGTH_LONG).show();
            Log.v(TAG, "mToasted has quick toasted: " + mToasted);
        } else {
            if (!mToasted) {
                Toast.makeText(this, getResources().getString(R.string.clear_identity_success), Toast.LENGTH_LONG).show();
            }
            Log.v(TAG, "mToasted no quick toasted: " + mToasted);
        }

    }
}
