package org.ea.sqrl.activites.create;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.CommonBaseActivity;
import org.ea.sqrl.processors.EntropyHarvester;

import java.io.IOException;

/**
 *
 * @author Daniel Persson
 */
public class EntropyGatherActivity extends CommonBaseActivity {
    private static final String TAG = "EntropyGatherActivity";

    private final int REQUEST_PERMISSION_CAMERA = 1;

    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout mPreviewLayout;
    private EntropyHarvester entropyHarvester;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entropy_gather);
        mPreviewLayout = findViewById(R.id.camera_preview);
    }

    @Override
    protected void onResume() {
        super.onResume();

        showPhoneStatePermission();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }

        if (mPreview != null) {
            mPreviewLayout.removeView(mPreview);
            mPreview = null;
        }
    }

    private void initCameraUsage() {
        final Button btnEntropyGatherNext = findViewById(R.id.btnEntropyGatherNext);
        btnEntropyGatherNext.setOnClickListener(v -> {
            entropyHarvester.digestEntropy();
            startActivity(new Intent(this, RescueCodeShowActivity.class));
        });

        progressBar = findViewById(R.id.pbEntropy);

        try {
            entropyHarvester = EntropyHarvester.getInstance();
            entropyHarvester.startGather();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        if (mCamera == null) {
            mCamera = getCameraInstance();
        }

        if (mPreview == null) {
            mPreview = new CameraPreview(this, mCamera, entropyHarvester);
            mPreviewLayout.addView(mPreview);
        }

        mCamera.startPreview();
    }

    private void showPhoneStatePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                showExplanation(
                        getString(R.string.camera_permission_request_title),
                        getString(R.string.camera_permission_request_desc)
                );
            } else {
                requestPermission();
            }
        } else {
            initCameraUsage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initCameraUsage();
                } else {
                    this.finish();
                }
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                EntropyGatherActivity.this,
                new String[] {Manifest.permission.CAMERA},
                REQUEST_PERMISSION_CAMERA
        );
    }

    private void showExplanation(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> requestPermission());
        builder.create().show();
    }


    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){}
        return c;
    }

    /** A basic Camera preview class */
    private class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private EntropyHarvester mEntropyHarvester;

        public CameraPreview(Context context, Camera camera, EntropyHarvester entropyHarvester) {
            super(context);
            mCamera = camera;
            mEntropyHarvester = entropyHarvester;
            mHolder = getHolder();
            mHolder.addCallback(this);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if(mCamera != null) {
                    mCamera.setPreviewDisplay(holder);
                    mCamera.setPreviewCallback((data, camera) -> {
                        mEntropyHarvester.addEntropy(progressBar, data);
                    });
                }
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            holder.removeCallback(this);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) { }
    }
}
