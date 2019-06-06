package org.ea.sqrl.activites.identity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.utils.SqrlApplication;

public class RenameActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);

        final EditText txtIdentityName = findViewById(R.id.txtIdentityName);

        final long currentId = SqrlApplication.getCurrentId(this.getApplication());

        if(currentId != 0) {
            txtIdentityName.setText(mDbHelper.getIdentityName(currentId));
        }

        findViewById(R.id.btnRename).setOnClickListener(v -> {
            if(currentId != 0) {
                mDbHelper.updateIdentityName(currentId, txtIdentityName.getText().toString());
            }
            txtIdentityName.setText("");

            RenameActivity.this.finish();
        });
    }
}
