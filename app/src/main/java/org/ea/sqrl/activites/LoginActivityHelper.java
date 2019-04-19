package org.ea.sqrl.activites;

import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.LoginBaseActivity;
import org.ea.sqrl.processors.BioAuthenticationCallback;
import org.ea.sqrl.processors.CommunicationFlowHandler;
import org.ea.sqrl.processors.SQRLStorage;

import java.security.KeyStore;

import javax.crypto.Cipher;

public class LoginActivityHelper {

    public static void doLoginBiometric (LoginBaseActivity activity, String domain, SQRLStorage storage, CommunicationFlowHandler communicationFlowHandler) {
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.P )  return;

        BioAuthenticationCallback biometricCallback =
                new BioAuthenticationCallback(activity.getApplicationContext(), () -> {
                    communicationFlowHandler.getHandler().post(() -> {
                        activity.hideLoginPopup();
                        activity.showProgressPopup();
                    });
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.QUERY_WITHOUT_SUK_QRCODE);
                    communicationFlowHandler.addAction(CommunicationFlowHandler.Action.LOGIN);

                    communicationFlowHandler.setDoneAction(() -> {
                        storage.clear();
                        communicationFlowHandler.getHandler().post(() -> {
                            activity.hideProgressPopup();
                            activity.closeActivity();
                        });
                    });

                    communicationFlowHandler.setErrorAction(() -> {
                        storage.clear();
                        storage.clearQuickPass();
                        communicationFlowHandler.getHandler().post(() -> activity.hideProgressPopup());
                    });

                    communicationFlowHandler.handleNextAction();
                });

        BiometricPrompt bioPrompt = new BiometricPrompt.Builder(activity)
                .setTitle(activity.getString(R.string.login_title))
                .setSubtitle(domain)
                .setDescription(activity.getString(R.string.login_verify_domain_text))
                .setNegativeButton(
                        activity.getString(R.string.button_cps_cancel),
                        activity.getMainExecutor(),
                        (dialogInterface, i) -> {}
                ).build();

        CancellationSignal cancelSign = new CancellationSignal();
        cancelSign.setOnCancelListener(() -> {});

        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.Entry entry = keyStore.getEntry("quickPass", null);
            Cipher decCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING"); //or try with "RSA"
            decCipher.init(Cipher.DECRYPT_MODE, ((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
            bioPrompt.authenticate(new BiometricPrompt.CryptoObject(decCipher), cancelSign, activity.getMainExecutor(), biometricCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
