package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupWindow;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

public class RenameActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);

        final EditText txtIdentityName = findViewById(R.id.txtIdentityName);

        SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                APPS_PREFERENCES,
                Context.MODE_PRIVATE
        );
        final long currentId = sharedPref.getLong(CURRENT_ID, 0);
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
