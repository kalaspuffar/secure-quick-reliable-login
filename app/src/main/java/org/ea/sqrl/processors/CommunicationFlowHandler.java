package org.ea.sqrl.processors;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
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
import org.ea.sqrl.services.AskDialogService;

import java.util.ArrayDeque;
import java.util.Deque;

public class CommunicationFlowHandler {
    private static final String TAG = "CommFlowHandler";
    private int lastTIF = 0;

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
    private Runnable doneAction;
    private Runnable errorAction;
    private Handler handler;
    private boolean hasRetried = false;

    private static CommunicationFlowHandler instance = null;
    private CPSServer cpsServer = null;
    private EntropyHarvester entropyHarvester;
    private final CommunicationHandler commHandler;
    private String serverData = null;
    private String queryLink = null;
    private boolean shouldRunServer = false;
    private boolean cpsServerStarted = false;
    private Activity currentActivity;

    private CommunicationFlowHandler(Activity currentActivity, Handler handler) {
        try {
            this.entropyHarvester = EntropyHarvester.getInstance();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        this.commHandler = CommunicationHandler.getInstance(currentActivity);
        this.cpsServer = CPSServer.getInstance(currentActivity, this);
        this.currentActivity = currentActivity;
        this.handler = handler;
        this.lastTIF = 0;
    }

    public static CommunicationFlowHandler getInstance(Activity currentActivity, Handler handler) {
        if(instance == null) {
            instance = new CommunicationFlowHandler(currentActivity, handler);
        }

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

    public void setDomain(String domain, String queryLink) throws Exception {
        this.commHandler.setDomain(domain, queryLink);
    }

    public void setAlternativeId(String alternativeId) {
        this.commHandler.setAlternativeId(alternativeId);
    }

    public CommunicationHandler getCommHandler() {
        return this.commHandler;
    }

    public void handleNextAction() {
        if(commHandler.hasErrorMessage(shouldRunServer)) {
            txtErrorMessage.setText(commHandler.getErrorMessage(this.currentActivity, shouldRunServer));
            error();
            return;
        }
        try {
            if (shouldRunServer && !cpsServerStarted) {
                cpsServerStarted = cpsServer.start(() -> done());
                if(!cpsServerStarted) {
                    cpsServer.interruptServerThread();
                    currentActivity.startActivity(new Intent(currentActivity, CPSMissingActivity.class));
                    return;
                }
            }
            if (!actionStack.isEmpty()) {
                runAction(actionStack.pop());
            } else {
                if (shouldRunServer) {
                    cpsServer.waitForCPS(true);
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
                if(!commHandler.isIdentityKnown(false))
                    throw new Exception(currentActivity.getString(R.string.account_missing));
                break;
            case UNLOCK_ACCOUNT:
            case UNLOCK_ACCOUNT_CPS:
                if(!commHandler.isIdentityKnown(true))
                    throw new Exception(currentActivity.getString(R.string.account_missing));
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
                    postLogin(commHandler, true, false);
                } else if (!commHandler.isIdentityKnown(false)) {
                    postCreateAccount(commHandler, true, false);
                }
                break;
            case LOGIN_CPS:
                if (commHandler.isIdentityKnown(false)) {
                    postLogin(commHandler, false, true);
                } else if (!commHandler.isIdentityKnown(false)) {
                    postCreateAccount(commHandler, false,true);
                }
                break;
            case REMOVE_ACCOUNT:
                postRemoveAccount(commHandler, true, false);
                break;
            case REMOVE_ACCOUNT_CPS:
                postRemoveAccount(commHandler, false, true);
                break;
            case LOCK_ACCOUNT:
                postDisableAccount(commHandler, true, false);
                break;
            case LOCK_ACCOUNT_CPS:
                postDisableAccount(commHandler, false, true);
                break;
            case UNLOCK_ACCOUNT:
                postEnableAccount(commHandler, true, false);
                break;
            case UNLOCK_ACCOUNT_CPS:
                postEnableAccount(commHandler, false, true);
                break;
        }

        /*
         * If an error occurs try again if we have a different error code than last time.
         */
        if(
            (
                commHandler.isTIFBitSet(CommunicationHandler.TIF_TRANSIENT_ERROR) ||
                commHandler.isTIFBitSet(CommunicationHandler.TIF_CLIENT_FAILURE) ||
                commHandler.isTIFBitSet(CommunicationHandler.TIF_COMMAND_FAILED) ||
                commHandler.isTIFBitSet(CommunicationHandler.TIF_BAD_ID_ASSOCIATION)
            ) && commHandler.getTif() != lastTIF
        ) {
            actionStack.push(a);
            lastTIF = commHandler.getTif();
            commHandler.clearLastResponse();
        } else if(commHandler.isPreviousKeyValid()) {
            commHandler.loginWithPreviousKey();
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
        new Thread(doneAction).start();
    }

    private void error() {
        if(shouldRunServer && cpsServerStarted) {
            cpsServer.setCancelCPS(true);
        }
        shouldRunServer = false;
        hasRetried = false;
        cpsServerStarted = false;
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
        SQRLStorage storage = SQRLStorage.getInstance(currentActivity);

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

    protected void postCreateAccount(CommunicationHandler commHandler, boolean noiptest, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(
                commHandler.createClientCreateAccount(entropyHarvester, noiptest, clientProvidedSession),
                serverData
        );
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postLogin(CommunicationHandler commHandler, boolean noiptest, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(
                commHandler.createClientLogin(entropyHarvester, noiptest, clientProvidedSession),
                serverData,
                commHandler.isPreviousKeyValid()
        );
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postDisableAccount(CommunicationHandler commHandler, boolean noiptest, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientDisable(noiptest, clientProvidedSession), serverData, true);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postEnableAccount(CommunicationHandler commHandler, boolean noiptest, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientEnable(noiptest, clientProvidedSession), serverData, true);
        commHandler.postRequest(queryLink, postData);
        serverData = commHandler.getResponse();
        queryLink = commHandler.getQueryLink();
        commHandler.printParams();
    }

    protected void postRemoveAccount(CommunicationHandler commHandler, boolean noiptest, boolean clientProvidedSession) throws Exception {
        String postData = commHandler.createPostParams(commHandler.createClientRemove(noiptest, clientProvidedSession), serverData, true);
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

    public void closeCPSServer() { cpsServer.close(); }

    public byte[] getDomain() {
        return commHandler.getDomain();
    }

    public void setNoCPSServer() {
        this.cpsServerStarted = true;
    }
}
