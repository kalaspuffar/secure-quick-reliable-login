package org.ea.sqrl.activites.identity;

import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

/**
 *
 * @author Daniel Persson
 */
public class ClearIdentityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_identity);

        final ImageView imgClearIdentity = findViewById(R.id.imgClearIdentity);
        final TextView txtClearIdentity = findViewById(R.id.txtClearIdentity);

        final Drawable failureImage = getResources().getDrawable(R.drawable.ic_clean_identity_fail);
        final Drawable successImage = getResources().getDrawable(R.drawable.ic_clean_identity_success);

        imgClearIdentity.setImageDrawable(failureImage);
        txtClearIdentity.setText(getString(R.string.clear_identity_fail));

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.clearQuickPass(this);
        if(!storage.hasQuickPass()) {
            imgClearIdentity.setImageDrawable(successImage);
            txtClearIdentity.setText(getString(R.string.clear_identity_success));
        }
    }
}
