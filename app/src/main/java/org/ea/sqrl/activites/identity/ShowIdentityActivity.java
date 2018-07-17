package org.ea.sqrl.activites.identity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.BaseActivity;
import org.ea.sqrl.processors.SQRLStorage;

import io.nayuki.qrcodegen.QrCode;

/**
 * This activity shows an identity. Both the QRCode you can scan to export the identity to another
 * device and it also shows the rescue data used by the rescue code in order to restore identity.
 *
 * @author Daniel Persson
 */
public class ShowIdentityActivity extends BaseActivity {
    private static final String TAG = "ShowIdentityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_identity);

        SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                APPS_PREFERENCES,
                Context.MODE_PRIVATE
        );
        long currentId = sharedPref.getLong(CURRENT_ID, 0);

        if(currentId == 0) return;

        final byte[] qrCodeData = mDbHelper.getIdentityData(currentId);
        if (qrCodeData.length == 0) {
            return;
        }


        final TextView txtIdentityText = findViewById(R.id.txtIdentityText);
        SQRLStorage storage = SQRLStorage.getInstance();
        try {
            storage.read(qrCodeData);
            txtIdentityText.setText(storage.getVerifyingRecoveryBlock());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        final Button btnCloseIdentity = findViewById(R.id.btnCloseIdentity);
        btnCloseIdentity.setOnClickListener(
                v -> {
                    ShowIdentityActivity.this.finish();
                }
        );

        final boolean exportWithoutPassword = getIntent().getBooleanExtra(EXPORT_WITHOUT_PASSWORD, false);

        byte[] saveData;
        if(exportWithoutPassword) {
            saveData = SQRLStorage.getInstance().createSaveDataWithoutPassword();
        } else {
            saveData = SQRLStorage.getInstance().createSaveData();
        }

        ImageView imageView = findViewById(R.id.imgQRCode);

        QrCode qrCode = QrCode.encodeBinary(saveData, QrCode.Ecc.MEDIUM);
        imageView.setImageBitmap(qrCode.toImage(3, 0));
    }
}
