package org.ea.sqrl.activites;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.EntropyHarvester;

import java.io.IOException;

/**
 *
 * @author Daniel Persson
 */
public class EntropyGatherActivity extends AppCompatActivity {
    private static final String TAG = "EntropyGatherActivity";

    private final int REQUEST_PERMISSION_CAMERA=1;

    private Camera mCamera;
    private EntropyHarvester entropyHarvester;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entropy_gather);

        showPhoneStatePermission();

        final Button btnEntropyGatherNext = findViewById(R.id.btnEntropyGatherNext);
        btnEntropyGatherNext.setOnClickListener(v -> {
            if(mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
            }
            entropyHarvester.digestEntropy();
            this.finish();
            startActivity(new Intent(this, RescueCodeShowActivity.class));
        });

        progressBar = findViewById(R.id.pbEntropy);

        try {
            entropyHarvester = EntropyHarvester.getInstance();
            entropyHarvester.startGather();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void initCameraUsage() {
        mCamera = getCameraInstance();
        final CameraPreview mPreview = new CameraPreview(this, mCamera, entropyHarvester);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
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
                    mCamera.startPreview();
                }
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {}

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if (mCamera == null || mHolder.getSurface() == null) {
                return;
            }

            try {
                mCamera.stopPreview();
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setPreviewCallback((data, camera) -> {
                    mEntropyHarvester.addEntropy(progressBar, data);
                });
                mCamera.startPreview();
            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }
}
