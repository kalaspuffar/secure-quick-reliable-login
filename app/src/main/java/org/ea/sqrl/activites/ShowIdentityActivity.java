package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

import java.io.File;
import java.io.FileOutputStream;

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
            ShowIdentityActivity.this.finish();
        }


        final TextView txtIdentityText = findViewById(R.id.txtIdentityText);
        SQRLStorage storage = SQRLStorage.getInstance();
        try {
            storage.read(qrCodeData);
            txtIdentityText.setText(storage.getVerifyingRecoveryBlock());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ShowIdentityActivity.this.finish();
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
        Bitmap bitmap = QrCode.encodeBinary(saveData, QrCode.Ecc.MEDIUM).toImage(10, 0);
        imageView.setImageBitmap(bitmap);
    }
}
