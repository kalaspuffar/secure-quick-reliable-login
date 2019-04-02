package org.ea.sqrl.processors;


import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;

@TargetApi(value=28)
public class BioAuthenticationCallback extends BiometricPrompt.AuthenticationCallback {
    private final Runnable doneCallback;
    private final Context context;

    public BioAuthenticationCallback(Context context, Runnable doneCallback) {
        this.context = context;
        this.doneCallback = doneCallback;
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        super.onAuthenticationHelp(helpCode, helpString);
    }

    @Override
    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        try {
            BiometricPrompt.CryptoObject co = result.getCryptoObject();
            SQRLStorage.getInstance(context).decryptIdentityKeyBiometric(co.getCipher());

            new Thread(doneCallback).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
    }
}
