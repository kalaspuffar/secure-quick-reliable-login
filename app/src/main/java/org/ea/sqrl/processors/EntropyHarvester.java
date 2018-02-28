package org.ea.sqrl.processors;

import android.os.Build;
import android.util.Log;

import java.security.SecureRandom;

/**
 * The point of this class is to be the one and only place to fetch your entropy from. It will
 * probably keep up a good amount of random sources and fetch data in order to give high entropy
 * random bits back when asked.
 *
 * TODO: Implement actual harvester, it will use secure random in order to get some data to start
 * with.
 *
 * @author Daniel Persson
 */
public class EntropyHarvester implements Runnable {
    private static final String TAG = "EntropyHarvester";

    private final SecureRandom sr;

    public EntropyHarvester() throws Exception {
        if(Build.VERSION.BASE_OS != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sr = SecureRandom.getInstanceStrong();
        } else {
            sr = SecureRandom.getInstance("SHA1PRNG");
        }
    }

    public void fetchRandom(byte[] buffer) {
        sr.nextBytes(buffer);
    }

    @Override
    public void run() {
        Log.d(TAG, "NOT IMPLEMENTED YET!");
    }
}
