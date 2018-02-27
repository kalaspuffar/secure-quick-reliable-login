package org.ea.sqrl.activites;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

import io.nayuki.qrcodegen.QrCode;

/**
 * This activity shows an identity. Both the QRCode you can scan to export the identity to another
 * device and it also shows the rescue data used by the rescue code in order to restore identity.
 *
 * @author Daniel Persson
 */
public class ShowIdentityActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_identity);

        SharedPreferences sharedPref = this.getApplication().getSharedPreferences(
                getString(R.string.preferences),
                Context.MODE_PRIVATE
        );
        //int defaultValue = getResources().getInteger(R.integer.saved_high_score_default);
        long currentId = sharedPref.getLong(getString(R.string.current_id), 0);

        byte[] qrCodeData = mDbHelper.getIdentityData(currentId);
        if (qrCodeData.length == 0) {
            ShowIdentityActivity.this.finish();
        }


        SQRLStorage storage = SQRLStorage.getInstance();
        try {
            storage.read(qrCodeData, true);
        } catch (Exception e) {
            e.printStackTrace();
            ShowIdentityActivity.this.finish();
        }

        final TextView txtIdentityText = findViewById(R.id.txtIdentityText);
        txtIdentityText.setText(storage.getVerifyingRecoveryBlock());

        final Button btnCloseIdentity = findViewById(R.id.btnCloseIdentity);
        btnCloseIdentity.setOnClickListener(v -> ShowIdentityActivity.this.finish());

        ImageView imageView = findViewById(R.id.imgQRCode);
        Bitmap bitmap = QrCode.encodeBinary(storage.createSaveData(), QrCode.Ecc.MEDIUM).toImage(10, 0);
        imageView.setImageBitmap(bitmap);
    }
}
