package org.ea.sqrl.processors;

import android.graphics.Color;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Small class to handle the progression of a decryption process. We show the time and
 * progress bar to the user so they know the application are working on their identity.
 *
 * @author Daniel Persson
 */
public class ProgressionUpdater {
    private static final String TAG = "ProgressionUpdater";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private Handler handler;
    private ProgressBar progressBar;
    private TextView progressTitle;
    private TextView progressText;
    private int max;
    private long startTime;
    private long endTime;
    private boolean dummy = false;

    public ProgressionUpdater() {
        dummy = true;
    }


    public ProgressionUpdater(Handler handler, TextView progressTitle, ProgressBar progressBar, TextView progressText) {
        this.handler = handler;
        this.progressTitle = progressTitle;
        this.progressBar = progressBar;
        this.progressText = progressText;
    }

    public String getTimeLeft() {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        long timeLeftInMilliSeconds = (endTime - startTime) * (max - progressBar.getProgress());
        return sdf.format(new Date(timeLeftInMilliSeconds));
    }

    public void setTimeDone(long timeInMilliSeconds) {
        if(dummy) return;

        int timeInSeconds = Math.round(timeInMilliSeconds / 1000f);
        handler.post(() -> {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            progressText.setTextColor(Color.GRAY);
            progressText.setText("Time elapsed: " + sdf.format(new Date(timeInMilliSeconds)));
            progressBar.setProgress(timeInSeconds);
        });
    }

    public void setState(String state) {
        if(progressTitle != null) progressTitle.setText(state);
    }

    public void incrementProgress() {
        if(dummy) return;
        handler.post(() -> {
            progressBar.incrementProgressBy(1);
            progressText.setTextColor(Color.GRAY);
            progressText.setText("Time left: " + getTimeLeft());
        });
    }

    public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    public void endTimer() {
        this.endTime = System.currentTimeMillis();
    }

    public void setMax(int max) {
        if(dummy) return;
        this.max = max;

        handler.post(() -> {
            progressBar.setMax(max);
            progressBar.setProgress(0);
            progressText.setTextColor(Color.GRAY);
            progressText.setText("Time left: " + getTimeLeft());
        });
    }

    public void clear() {
        if(dummy) return;

        handler.post(() -> {
            progressBar.setMax(1);
            progressBar.setProgress(0);
            progressText.setTextColor(Color.GRAY);
            progressText.setText("");
        });
    }
}
