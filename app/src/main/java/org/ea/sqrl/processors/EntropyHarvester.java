package org.ea.sqrl.processors;

import android.os.Build;
import android.util.Log;
import android.widget.ProgressBar;

import org.ea.sqrl.utils.EncryptionUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
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
public class EntropyHarvester {
    private static final String TAG = "EntropyHarvester";

    private final SecureRandom sr;
    private final MessageDigest md;
    private static EntropyHarvester instance;
    private BigInteger numberOfBytesGathered;
    private ProgressBar progressBar;

    public static EntropyHarvester getInstance() throws Exception {
        if(instance == null) {
            instance = new EntropyHarvester();
        }
        return instance;
    }

    private EntropyHarvester() throws Exception {
        if(Build.DEVICE != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sr = SecureRandom.getInstanceStrong();
        } else {
            sr = SecureRandom.getInstance("SHA1PRNG");
        }

        md = MessageDigest.getInstance("SHA-512");
        this.numberOfBytesGathered = BigInteger.ZERO;
    }

    public void fetchRandom(byte[] buffer) {
        sr.nextBytes(buffer);
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.numberOfBytesGathered = BigInteger.ZERO;
        this.progressBar = progressBar;
    }

    public void addEntropy(byte[] bytes) {
        md.update(bytes);
        numberOfBytesGathered = numberOfBytesGathered.add(BigInteger.valueOf(bytes.length));
        if(progressBar != null) {
            progressBar.setProgress(Math.round(numberOfBytesGathered.longValue() / 1_000_000));
        }
    }

    public void digestEntropy() {
        byte[] entropyBytes = md.digest();
        sr.setSeed(entropyBytes);
    }
}
