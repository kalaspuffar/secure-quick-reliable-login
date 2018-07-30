package org.ea.sqrl.processors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.CPSMissingActivity;
import org.ea.sqrl.activites.MainActivity;
import org.ea.sqrl.activites.StartActivity;
import org.ea.sqrl.activites.identity.ExportOptionsActivity;
import org.ea.sqrl.services.AskDialogService;
import org.ea.sqrl.utils.EncryptionUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class CommunicationFlowHandler {
    private static final String TAG = "CommFlowHandler";

    public enum Action {
        QUERY_WITH_SUK,
        QUERY_WITHOUT_SUK,
        QUERY_WITH_SUK_QRCODE,
        QUERY_WITHOUT_SUK_QRCODE,
        LOGIN,
        LOGIN_CPS,
        CREATE_ACCOUNT,
        CREATE_ACCOUNT_CPS,
        REMOVE_ACCOUNT,
        REMOVE_ACCOUNT_CPS,
        LOCK_ACCOUNT,
        LOCK_ACCOUNT_CPS,
        UNLOCK_ACCOUNT,
        UNLOCK_ACCOUNT_CPS
    }

    private PopupWindow askPopupWindow;
    private PopupWindow errorPopupWindow;
    private TextView txtErrorMessage;

    private Deque<Action> actionStack = new ArrayDeque<>();
    private ServerSocket server;
    private Runnable doneAction;
    private Runnable errorAction;
    private Handler handler;
    private boolean hasRetried = false;
    private boolean sentImage = false;
    private Thread cpsThread;

    private static CommunicationFlowHandler instance = null;
    private EntropyHarvester entropyHarvester;
    private final CommunicationHandler commHandler = CommunicationHandler.getInstance();
    private String serverData = null;
    private String queryLink = null;
    private boolean shouldRunServer = false;
    private boolean cpsServerStarted = false;
    private boolean cancelCPS = false;
    private Activity currentActivity;

    private CommunicationFlowHandler() {}

    public static CommunicationFlowHandler getInstance(Activity currentActivity, Handler handler) {
        if(instance == null) {
            instance = new CommunicationFlowHandler();
        }
        try {
            instance.entropyHarvester = EntropyHarvester.getInstance();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        instance.currentActivity = currentActivity;
        instance.handler = handler;
        return instance;
    }

    public void setUrlBasedLogin(boolean urlBasedLogin) {
        commHandler.setUrlBasedLogin(urlBasedLogin);
    }

    public boolean isUrlBasedLogin() {
        return commHandler.isUrlBasedLogin();
    }

    public void setServerData(String serverData) {
        this.serverData = serverData;
    }

    public void setUseSSL(boolean useSSL) {
        this.commHandler.setUseSSL(useSSL);
    }

    public void setQueryLink(String queryLink) {
        this.queryLink = queryLink;
    }

    public void setDomain(String domain) throws Exception {
        this.commHandler.setDomain(domain);
    }

    public void waitForCPS(boolean afterConversation) {
        int time = 0;
        while (cpsThread.isAlive() && time < 10 && (!sentImage || afterConversation)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
            time++;
        }
    }

    public void waitForTransactionDone() {
        while (cpsThread.isAlive() && !this.commHandler.hasCPSUrl()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }
    }

    public void handleNextAction() {
        if(commHandler.hasErrorMessage(shouldRunServer)) {
            txtErrorMessage.setText(commHandler.getErrorMessage(this.currentActivity, shouldRunServer));
            error();
            return;
        }
        try {
            if (shouldRunServer && !cpsServerStarted) {
                cpsServerStarted = startCPSServer(() -> done());
                if(!cpsServerStarted) {
                    cpsThread.interrupt();
                    currentActivity.startActivity(new Intent(currentActivity, CPSMissingActivity.class));
                    return;
                }
            }
            if (!actionStack.isEmpty()) {
                runAction(actionStack.pop());
            } else {
                if (shouldRunServer) {
                    waitForCPS(true);
                }
                done();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if(e.getMessage() != null) {
                if("CONN_ERROR".equalsIgnoreCase(e.getMessage())) {
                    txtErrorMessage.setText(currentActivity.getString(R.string.connection_error));
                } else {
                    txtErrorMessage.setText(e.getMessage());
                }
            }
            error();
        }
    }

    private void runAction(Action a) throws Exception {
        switch (a) {
            case LOGIN:
            case LOGIN_CPS:
                if(commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED))
                    throw new Exception(currentActivity.getString(R.string.communication_sqrl_disabled));
                break;
            case LOCK_ACCOUNT:
            case LOCK_ACCOUNT_CPS:
                if(commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED))
                    throw new Exception(currentActivity.getString(R.string.communication_sqrl_disabled));
                if(!commHandler.isIdentityKnown(false)) return;
                break;
            case UNLOCK_ACCOUNT:
            case UNLOCK_ACCOUNT_CPS:
                if(!commHandler.isIdentityKnown(true)) return;
                break;
        }

        switch (a) {
            case QUERY_WITH_SUK:
                postQuery(commHandler, false, true);
                break;
            case QUERY_WITHOUT_SUK:
                postQuery(commHandler, false, false);
                break;
            case QUERY_WITH_SUK_QRCODE:
                postQuery(commHandler, true, true);
                break;
            case QUERY_WITHOUT_SUK_QRCODE:
                postQuery(commHandler, true, false);
                break;
            case LOGIN:
                if (commHandler.isIdentityKnown(false)) {
                    postLogin(commHandler, false);
                } else if (!commHandler.isIdentityKnown(false)) {
                    postCreateAccount(commHandler, false);
                }
                break;
            case LOGIN_CPS:
                if (commHandler.isIdentityKnown(false)) {
                    postLogin(commHandler, true);
                } else if (!commHandler.isIdentityKnown(false)) {
                    postCreateAccount(commHandler, true);
                }
                break;
            case REMOVE_ACCOUNT:
                postRemoveAccount(commHandler, false);
                break;
            case REMOVE_ACCOUNT_CPS:
                postRemoveAccount(commHandler, true);
                break;
            case LOCK_ACCOUNT:
                if(commHandler.isIdentityKnown(false)) {
                    postDisableAccount(commHandler, false);
                }
                break;
            case LOCK_ACCOUNT_CPS:
                if(commHandler.isIdentityKnown(false)) {
                    postDisableAccount(commHandler, true);
                }
                break;
            case UNLOCK_ACCOUNT:
                postEnableAccount(commHandler, false);
                break;
            case UNLOCK_ACCOUNT_CPS:
                postEnableAccount(commHandler, true);
                break;
        }

        /*
         * Remove the option of the server returning transient error when the request is not accepted
         * due to IP restriction or reusing of code. Retry to with a fresh nut and hope the error
         * disappears.
         */
        switch (a) {
            case QUERY_WITH_SUK:
            case QUERY_WITHOUT_SUK:
            case QUERY_WITH_SUK_QRCODE:
            case QUERY_WITHOUT_SUK_QRCODE:
                if(!hasRetried && commHandler.isTIFBitSet(CommunicationHandler.TIF_TRANSIENT_ERROR)) {
                    actionStack.push(a);
                    commHandler.clearLastResponse();
                    hasRetried = true;
                }
                break;
        }

        if(commHandler.hasAskQuestion() && this.actionStack.isEmpty()) {
            this.actionStack.add(Action.QUERY_WITHOUT_SUK);
        }

        commHandler.setAskAction(this::handleNextAction);
        commHandler.showAskDialog();
    }

    private void done() {
        shouldRunServer = false;
        hasRetried = false;
        cpsServerStarted = false;
        sentImage = false;
        new Thread(doneAction).start();
    }

    private void error() {
        if(shouldRunServer && cpsServerStarted) {
            cancelCPS = true;
        }
        shouldRunServer = false;
        hasRetried = false;
        cpsServerStarted = false;
        sentImage = false;
        commHandler.clearLastResponse();
        handler.post(() ->
            errorPopupWindow.showAtLocation(errorPopupWindow.getContentView(), Gravity.CENTER, 0, 0)
        );
        new Thread(errorAction).start();
    }

    public void addAction(Action a) {
        switch (a) {
            case CREATE_ACCOUNT_CPS:
            case LOGIN_CPS:
            case LOCK_ACCOUNT_CPS:
            case REMOVE_ACCOUNT_CPS:
            case UNLOCK_ACCOUNT_CPS:
                shouldRunServer = true;
                break;
        }

        commHandler.clearLastResponse();
        this.actionStack.add(a);
    }

    protected void postQuery(CommunicationHandler commHandler, boolean noiptest, boolean requestServerUnlockKey) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();

        if (!storage.hasMorePreviousKeys()) {
            postQueryInternal(commHandler, noiptest, requestServerUnlockKey);
            return;
        }

        while (storage.hasMorePreviousKeys()) {
            storage.increasePreviousKeyIndex();
            if(commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH)) break;
            if(commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) break;
            if(commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)) break;
            postQueryInternal(commHandler, noiptest, requestServerUnlockKey);
            noiptest = false;
        }
    }


    private void postQueryInternal(CommunicationHandler commHandler, boolean noiptest, boolean requestServerUnlockKey) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientQuery(noiptest, requestServerUnlockKey), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postCreateAccount(CommunicationHandler commHandler, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(
                commHandler.createClientCreateAccount(entropyHarvester, clientProvidedSession),
                serverData
        );
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postLogin(CommunicationHandler commHandler, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientLogin(clientProvidedSession), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postDisableAccount(CommunicationHandler commHandler, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientDisable(clientProvidedSession), serverData);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postEnableAccount(CommunicationHandler commHandler, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientEnable(clientProvidedSession), serverData, true);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postRemoveAccount(CommunicationHandler commHandler, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientRemove(clientProvidedSession), serverData, true);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    public void setDoneAction(Runnable doneAction) {
        this.doneAction = doneAction;
    }

    public void setErrorAction(Runnable errorAction) {
        this.errorAction = errorAction;
    }

    public void setupAskPopupWindow(LayoutInflater layoutInflater, Handler handler) {
        View popupView = layoutInflater.inflate(R.layout.fragment_ask_dialog, null);

        askPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);


        final TextView txtAskQuestion = popupView.findViewById(R.id.txtAskQuestion);
        final Button btnAskFirstButton = popupView.findViewById(R.id.btnAskFirstButton);
        final Button btnAskSecondButton = popupView.findViewById(R.id.btnAskSecondButton);
        final ImageButton btnCloseAsk = popupView.findViewById(R.id.btnCloseAsk);

        btnAskFirstButton.setOnClickListener(v -> {
            askPopupWindow.dismiss();
            commHandler.setAskButton("1");
        });
        btnAskSecondButton.setOnClickListener(v -> {
            askPopupWindow.dismiss();
            commHandler.setAskButton("2");
        });
        btnCloseAsk.setOnClickListener(v -> {
            askPopupWindow.dismiss();
            commHandler.setAskButton("3");
        });

        commHandler.setAskDialogService(new AskDialogService(
                handler,
                askPopupWindow,
                txtAskQuestion,
                btnAskFirstButton,
                btnAskSecondButton
        ));
    }


    public void setupErrorPopupWindow(LayoutInflater layoutInflater) {
        View popupView = layoutInflater.inflate(R.layout.fragment_error_dialog, null);

        errorPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                false);


        txtErrorMessage = popupView.findViewById(R.id.txtErrorMessage);
        final Button btnErrorOk = popupView.findViewById(R.id.btnErrorOk);
        btnErrorOk.setOnClickListener(v -> {
            errorPopupWindow.dismiss();
        });
    }

    private Map<String, String> getQueryParams(String data) throws Exception {
        Map<String, String> params = new HashMap<>();
        String url = EncryptionUtils.decodeUrlSafeString(data);
        String query = url.split("\\?")[1];
        String[] paramArr = query.split("&");
        for(String s : paramArr) {
            String[] param = s.split("=");
            if(param[0].equals("can")) {
                params.put(param[0], EncryptionUtils.decodeUrlSafeString(param[1]));
            } else {
                params.put(param[0], param[1]);
            }
        }
        return params;
    }

    public void closeServer() {
        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean startCPSServer(Runnable closeScreen) throws Exception {
        cpsThread = new Thread(() -> {
            boolean done = false;
            try {
                server = new ServerSocket(25519);

                while (!server.isClosed() && !done) {
                    Socket socket = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String line = in.readLine();
                    Log.i(TAG, line);

                    if(line.contains("gif HTTP/1.1")) {
                        byte[] content = EncryptionUtils.decodeUrlSafe(
                                "R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=="
                        );
                        OutputStream os = socket.getOutputStream();
                        StringBuilder out = new StringBuilder();
                        out.append("HTTP/1.0 200 OK\r\n");
                        out.append("Content-Type: image/gif\r\n");
                        out.append("Content-Length: ").append(content.length).append("\r\n\r\n");
                        Log.i(TAG, out.toString());
                        os.write(out.toString().getBytes("UTF-8"));
                        os.write(content);
                        os.flush();
                        os.close();
                        sentImage = true;
                    } else {
                        String[] linearg = line.split(" ");
                        String data = linearg[1].substring(1);

                        Map<String, String> params = getQueryParams(data);
                        if(params.containsKey("can")) {
                            Log.i(TAG, params.get("can"));
                        }

                        OutputStream os = socket.getOutputStream();
                        StringBuilder out = new StringBuilder();

                        waitForTransactionDone();

                        out.append("HTTP/1.0 302 Found\r\n");
                        if(cancelCPS) {
                            if(params.containsKey("can")) {
                                out.append("Location: ").append(params.get("can")).append("\r\n\r\n");
                            } else {
                                out.append("Location: ").append("https://www.google.com").append("\r\n\r\n");
                            }
                        } else {
                            out.append("Location: ").append(commHandler.getCPSUrl()).append("\r\n\r\n");
                        }
                        Log.i(TAG, out.toString());
                        os.write(out.toString().getBytes("UTF-8"));
                        os.flush();
                        os.close();
                        done = true;
                    }

                    in.close();
                    socket.close();
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }

            if(done) {
                closeScreen.run();
            }
        });
        cpsThread.start();

        waitForCPS(false);

        if(cpsThread.isAlive() && !sentImage) {
            return false;
        }
        return true;
    }

    public String getDomain() {
        return commHandler.getDomain();
    }

    public void setNoCPSServer() {
        this.cpsServerStarted = true;
    }
}
