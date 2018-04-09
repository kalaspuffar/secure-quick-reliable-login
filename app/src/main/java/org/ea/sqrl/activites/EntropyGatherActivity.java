package org.ea.sqrl.activites;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
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

public class EntropyGatherActivity extends AppCompatActivity {
    private static final String TAG = "EntropyGatherActivity";

    private Camera mCamera;
    private EntropyHarvester entropyHarvester;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entropy_gather);

        final Button btnEntropyGatherNext = findViewById(R.id.btnEntropyGatherNext);
        btnEntropyGatherNext.setOnClickListener(v -> {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            entropyHarvester.digestEntropy();
            this.finish();
            startActivity(new Intent(this, RescueCodeShowActivity.class));
        });

        mCamera = getCameraInstance();
        progressBar = findViewById(R.id.pbEntropy);

        try {
            entropyHarvester = EntropyHarvester.getInstance();
            entropyHarvester.startGather();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        final CameraPreview mPreview = new CameraPreview(this, mCamera, entropyHarvester);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
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
