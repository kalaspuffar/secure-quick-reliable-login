package org.ea.sqrl.services;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.ea.sqrl.utils.EncryptionUtils;

/**
 *
 * @author Daniel Persson
 */
public class AskDialogService {
    private static final String TAG = "AskDialogService";
    private Handler handler;
    private PopupWindow askPopupWindow;
    private TextView txtAskQuestion;
    private Button btnAskFirstButton;
    private Button btnAskSecondButton;
    private Runnable askAction;

    public AskDialogService(Handler handler, PopupWindow askPopupWindow, TextView txtAskQuestion, Button btnAskFirstButton, Button btnAskSecondButton) {
        this.handler = handler;
        this.askPopupWindow = askPopupWindow;
        this.txtAskQuestion = txtAskQuestion;
        this.btnAskFirstButton = btnAskFirstButton;
        this.btnAskSecondButton = btnAskSecondButton;
    }

    public void showDialog(String askString) {
        String[] askArray = askString.split("~");
        handler.post(() -> {
            try {
                btnAskSecondButton.setVisibility(View.GONE);
                txtAskQuestion.setText(new String(EncryptionUtils.decodeUrlSafe(askArray[0]), "UTF-8"));
                if(askArray.length > 1) {
                    String buttonString = new String(EncryptionUtils.decodeUrlSafe(askArray[1]), "UTF-8");
                    String[] buttonStringArr = buttonString.split(";");
                    btnAskFirstButton.setText(buttonStringArr[0]);
                }
                if(askArray.length > 2) {
                    String buttonString = new String(EncryptionUtils.decodeUrlSafe(askArray[2]), "UTF-8");
                    String[] buttonStringArr = buttonString.split(";");
                    btnAskSecondButton.setText(buttonStringArr[0]);
                    btnAskSecondButton.setVisibility(View.VISIBLE);
                }

                askPopupWindow.showAtLocation(askPopupWindow.getContentView(), Gravity.CENTER, 0, 0);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    public void activateAskButton() {
        handler.post(() -> {
            askPopupWindow.dismiss();
        });
        new Thread(askAction).start();
    }

    public void setAskAction(Runnable askAction) {
        this.askAction = askAction;
    }
}
