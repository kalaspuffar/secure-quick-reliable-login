package org.ea.sqrl.services;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import org.ea.sqrl.utils.EncryptionUtils;

import java.security.KeyStore;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;

public class FingerprintHandler {
    private static final String TAG = "FingerprintHandler";

    private static final String KEY_NAME = "SQRLFingerprintKey";
    private static final String CHARSET_NAME = "UTF-8";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String TRANSFORMATION =
            KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7;

    private static final int AUTHENTICATION_DURATION_SECONDS = 30;

    private KeyguardManager keyguardManager;
    private FingerprintManager fingerprintManager;
    private Cipher cipher;
    private static final int SAVE_CREDENTIALS_REQUEST_CODE = 1;

    private byte[] encryptionIv = null;
    private byte[] encryptedPasswordBytes = null;

    private Activity activity;

    public FingerprintHandler(Activity a) {
        activity = a;
    }

    public boolean init() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            keyguardManager = (KeyguardManager) activity.getSystemService(KEYGUARD_SERVICE);
            fingerprintManager = (FingerprintManager) activity.getSystemService(FINGERPRINT_SERVICE);

            if (!keyguardManager.isKeyguardSecure()) {
                Toast.makeText(activity, "Lock screen security not enabled in Settings", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Lock screen security not enabled in Settings");
                return false;
            }

            if (ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.USE_FINGERPRINT) !=
                    PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "Fingerprint authentication permission not enabled", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Fingerprint authentication permission not enabled");

                return false;
            }

            if (!fingerprintManager.hasEnrolledFingerprints()) {

                // This happens when no fingerprints are registered.
                Toast.makeText(activity, "Register at least one fingerprint in Settings", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Register at least one fingerprint in Settings");
                return false;
            }
        }
        return true;
    }

    public boolean hasKey() {
        return encryptedPasswordBytes != null;
    }

    public void encryptKey(byte[] pass) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            try {
                CancellationSignal mCancellationSignal = new CancellationSignal();

                SecretKey secretKey = createKey();
                cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);

                fingerprintManager.authenticate(
                    new FingerprintManager.CryptoObject(cipher), mCancellationSignal,
                    0 /* flags */,
                    new FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            System.out.println("Error "+errorCode+" = "+errString);
                            super.onAuthenticationError(errorCode, errString);
                        }

                        @Override
                        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                            System.out.println("Help "+helpCode+" = "+helpString);
                            super.onAuthenticationHelp(helpCode, helpString);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            System.out.println("FAIL!");
                            super.onAuthenticationFailed();
                        }

                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                            System.out.println("FIXED!");
                            System.out.println(EncryptionUtils.byte2hex(pass));
                            try {
                                encryptionIv = cipher.getIV();
                                encryptedPasswordBytes = cipher.doFinal(pass);
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }
                    }, null);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    private SecretKey createKey() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            try {
                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.MINUTE, 15);

                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setKeyValidityEnd(c.getTime())
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build());
                return keyGenerator.generateKey();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return null;
    }

    public byte[] decryptKey() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            try {
                CancellationSignal mCancellationSignal = new CancellationSignal();

                Cipher cipher = Cipher.getInstance(TRANSFORMATION);

                fingerprintManager.authenticate(
                        new FingerprintManager.CryptoObject(cipher), mCancellationSignal,
                        0 /* flags */,
                        new FingerprintManager.AuthenticationCallback() {
                            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                                try {
                                    KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
                                    keyStore.load(null);

                                    Cipher cipher = result.getCryptoObject().getCipher();

                                    SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);
                                    cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(encryptionIv));
                                    byte[] passwordBytes = cipher.doFinal(encryptedPasswordBytes);
                                    System.out.println("-------------------------------------------------");
                                    System.out.println("FIXED!");
                                    System.out.println(EncryptionUtils.byte2hex(passwordBytes));
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }

                            }
                        }, null);

                return encryptedPasswordBytes;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return new byte[0];
    }

    private void showAuthenticationScreen(Activity a, int requestCode) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
            if (intent != null) {
                a.startActivityForResult(intent, requestCode);
            }
        }
    }
}
