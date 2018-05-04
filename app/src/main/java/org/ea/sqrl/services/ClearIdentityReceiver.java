package org.ea.sqrl.services;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.ea.sqrl.processors.SQRLStorage;

/**
 *
 * @author Daniel Persson
 */
public class ClearIdentityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SQRLStorage.getInstance().clearQuickPass(context);
    }
}
