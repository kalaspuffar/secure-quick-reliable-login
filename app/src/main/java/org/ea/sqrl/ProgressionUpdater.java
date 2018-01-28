package org.ea.sqrl;

import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ProgressionUpdater {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private Handler handler;
    private ProgressBar progressBar;
    private TextView progressText;
    private int max;
    private long startTime;
    private long endTime;

    public ProgressionUpdater(Handler handler, ProgressBar progressBar, TextView progressText) {
        this.handler = handler;
        this.progressBar = progressBar;
        this.progressText = progressText;
    }

    public String getTimeLeft() {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        long timeLeftInMilliSeconds = (endTime - startTime) * (max - progressBar.getProgress());
        return sdf.format(new Date(timeLeftInMilliSeconds));
    }

    public void incrementProgress() {
        handler.post(() -> progressBar.incrementProgressBy(1));
        this.progressText.setText("Time left: " + getTimeLeft());
    }

    public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    public void endTimer() {
        this.endTime = System.currentTimeMillis();
    }

    public void setMax(int max) {
        this.max = max;
        this.progressBar.setMax(max);
        this.progressText.setText("Time left: " + getTimeLeft());
    }
}
