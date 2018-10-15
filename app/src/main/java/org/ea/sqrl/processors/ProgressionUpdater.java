package org.ea.sqrl.processors;

import android.graphics.Color;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ea.sqrl.R;

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
    private int counter;
    private int state;
    private long startTime;
    private long endTime;

    public ProgressionUpdater() {}

    public String getTimeLeft() {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        long timeLeftInMilliSeconds = (endTime - startTime) * (max - progressBar.getProgress());
        return sdf.format(new Date(timeLeftInMilliSeconds));
    }

    public String getString(int res, String s) {
        return progressText.getContext().getString(res, s);
    }

    public void setTimeDone(long timeInMilliSeconds) {
        if(handler == null) return;

        int timeInSeconds = Math.round(timeInMilliSeconds / 1000f);
        handler.post(() -> {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            progressText.setTextColor(Color.GRAY);
            progressText.setText(getString(R.string.progress_time_elapsed, sdf.format(new Date(timeInMilliSeconds))));
            progressBar.setProgress(timeInSeconds);
        });
    }

    public void setState(int state) {
        this.state = state;
        if (progressTitle != null) {
            handler.post(() -> progressTitle.setText(state));
        }
    }

    public void incrementProgress() {
        if(handler == null) return;

        this.counter++;

        handler.post(() -> {
            progressBar.incrementProgressBy(1);
            progressText.setTextColor(Color.GRAY);
            progressText.setText(getString(R.string.progress_time_left, getTimeLeft()));
        });
    }

    public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    public void endTimer() {
        this.endTime = System.currentTimeMillis();
    }

    public void setMax(int max) {
        if(handler == null) return;
        this.max = max;
        this.counter = 0;

        handler.post(() -> {
            progressBar.setMax(max);
            progressBar.setProgress(0);
            progressText.setTextColor(Color.GRAY);
            progressText.setText(getString(R.string.progress_time_left, getTimeLeft()));
        });
    }

    public void clear() {
        if(handler == null) return;

        this.max = 1;
        this.counter = 0;

        handler.post(() -> {
            progressBar.setMax(1);
            progressBar.setProgress(0);
            progressText.setTextColor(Color.GRAY);
            progressText.setText("");
        });
    }

    public void reset() {
        handler.post(() -> {
            progressTitle.setText(this.state);
            progressBar.setMax(this.max);
            progressBar.setProgress(this.counter);
            progressText.setTextColor(Color.GRAY);
            progressText.setText(getString(R.string.progress_time_left, getTimeLeft()));
        });
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setProgressTitle(TextView progressTitle) {
        this.progressTitle = progressTitle;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void setProgressText(TextView progressText) {
        this.progressText = progressText;
    }
}
