package org.ea.sqrl.activites.identity;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.utils.SqrlApplication;

public class RenameActivity extends BaseActivity {
    private EditText txtIdentityName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);

        txtIdentityName = findViewById(R.id.txtIdentityName);

        final long currentId = SqrlApplication.getCurrentId(this.getApplication());

        if(currentId != 0) {
            txtIdentityName.setText(mDbHelper.getIdentityName(currentId));
            txtIdentityName.setSelectAllOnFocus(true);
            txtIdentityName.requestFocus();
        }

        txtIdentityName.setOnEditorActionListener((v, actionId, event) -> {
            switch (actionId) {
                case EditorInfo.IME_ACTION_DONE:
                    doRename();
                    return true;
                default:
                    return false;
            }
        });

        findViewById(R.id.btnRename).setOnClickListener(v -> doRename());
    }

    private void doRename() {
        final long currentId = SqrlApplication.getCurrentId(this.getApplication());

        if(currentId != 0) {
            mDbHelper.updateIdentityName(currentId, txtIdentityName.getText().toString());
        }
        txtIdentityName.setText("");

        RenameActivity.this.finish();
    }
}
