package org.ea.sqrl;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Small class to handle the progression of a decryption process. We show the time and
 * progress bar to the user so they know the application are working on their identity.
 *
 * @author Daniel Persson
 */
public class ProgressionUpdater {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private Handler handler;
    private ProgressBar progressBar;
    private TextView progressText;
    private int max;
    private long startTime;
    private long endTime;
    private boolean dummy = false;

    public ProgressionUpdater() {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        dummy = true;
    }


    public ProgressionUpdater(Handler handler, ProgressBar progressBar, TextView progressText) {
        this.handler = handler;
        this.progressBar = progressBar;
        this.progressText = progressText;
    }

    public String getTimeLeft() {
        long timeLeftInMilliSeconds = (endTime - startTime) * (max - progressBar.getProgress());
        return sdf.format(new Date(timeLeftInMilliSeconds));
    }

    public void setTimeLeft(long timeLeftInMilliSeconds) {
        handler.post(() -> {
            progressText.setTextColor(Color.GRAY);
            progressText.setText("Time left: " + sdf.format(new Date(timeLeftInMilliSeconds)));
        });
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
