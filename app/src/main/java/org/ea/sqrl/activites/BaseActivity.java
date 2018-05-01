package org.ea.sqrl.activites;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ea.sqrl.BuildConfig;
import org.ea.sqrl.R;
import org.ea.sqrl.database.IdentityDBHelper;
import org.ea.sqrl.processors.EntropyHarvester;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;

import java.util.function.Function;

/**
 * This base activity is inherited by all other activities. We place logic used for menus,
 * background processes and other things that are untied to the current context of the application.
 *
 * @author Daniel Persson
 */
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected static final String CURRENT_ID = "current_id";
    protected static final String APPS_PREFERENCES = "org.ea.sqrl.preferences";
    protected static final String EXPORT_WITHOUT_PASSWORD = "export_without_password";

    private final int REQUEST_PERMISSION_CAMERA = 1;

    private PopupWindow cameraAccessPopupWindow;

    protected static final String START_USER_MODE = "org.ea.sqrl.START_MODE";
    protected static final int START_USER_MODE_NEW_USER = 1;
    protected static final int START_USER_MODE_RETURNING_USER = 2;
    public static final int NOTIFICATION_IDENTITY_UNLOCKED = 1;

    protected final IdentityDBHelper mDbHelper;
    protected EntropyHarvester entropyHarvester;

    public BaseActivity() {
        mDbHelper = new IdentityDBHelper(this);
        try {
            entropyHarvester = EntropyHarvester.getInstance();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_default, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                Intent intent = new Intent(this, IntroductionActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_about:
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(BaseActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(BaseActivity.this);
                }
                builder.setTitle(R.string.about_message_title)
                        .setMessage(getString(R.string.about_message_text, BuildConfig.VERSION_NAME))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setupProgressPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_camera_permission, null);

        cameraAccessPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);

        final Button btnCameraPermissionOk = popupView.findViewById(R.id.btnCameraPermissionOk);
        btnCameraPermissionOk.setOnClickListener(v -> {
            requestPermission();
            cameraAccessPopupWindow.dismiss();
        });
    }

    protected void permissionOkCallback() {

    }

    protected void showPhoneStatePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                cameraAccessPopupWindow.showAtLocation(cameraAccessPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
            } else {
                requestPermission();
            }
        } else {
            permissionOkCallback();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionOkCallback();
                }
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                BaseActivity.this,
                new String[] {Manifest.permission.CAMERA},
                REQUEST_PERMISSION_CAMERA
        );
    }


}
