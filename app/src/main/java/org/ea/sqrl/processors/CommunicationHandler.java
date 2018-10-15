package org.ea.sqrl.processors;

import android.app.Activity;
import android.util.Log;

import org.ea.sqrl.R;
import org.ea.sqrl.services.AskDialogService;
import org.ea.sqrl.utils.EncryptionUtils;
import org.libsodium.jni.Sodium;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This handler creates different queries to servers and parses the response so we can talk over
 * the SQRL protocol seamlessly.
 *
 * @author Daniel Persson
 */
public class CommunicationHandler {
    private static final String TAG = "CommunicationHandler";
    public static final Pattern sqrlPattern = Pattern.compile("^s*qrl://([^?/]+)(.*)$");

    private static CommunicationHandler instance = null;
    private String communicationDomain;
    private String cryptDomain;
    private Map<String, String> lastResponse = new HashMap<>();
    private String askButton;
    private AskDialogService askDialogService;
    private String response;
    private boolean useSSL;
    private boolean urlBasedLogin = false;

    public static final int TIF_CURRENT_ID_MATCH = 0;
    public static final int TIF_PREVIOUS_ID_MATCH = 1;
    public static final int TIF_IP_MATCHED = 2;
    public static final int TIF_SQRL_DISABLED = 3;
    public static final int TIF_FUNCTION_NOT_SUPPORTED = 4;
    public static final int TIF_TRANSIENT_ERROR = 5;
    public static final int TIF_COMMAND_FAILED = 6;
    public static final int TIF_CLIENT_FAILURE = 7;
    public static final int TIF_BAD_ID_ASSOCIATION = 8;

    private CommunicationHandler() {}

    public static CommunicationHandler getInstance() {
        if(instance == null) {
            instance = new CommunicationHandler();
        }
        return instance;
    }

    public void setUrlBasedLogin(boolean urlBasedLogin) {
        this.urlBasedLogin = urlBasedLogin;
    }

    public boolean isUrlBasedLogin() {
        return this.urlBasedLogin;
    }

    public void clearLastResponse() {
        this.lastResponse = new HashMap<>();
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public void setDomain(String domain) throws Exception {
        this.communicationDomain = domain;
        int atSignIndex = domain.indexOf("@");
        int portColon = domain.indexOf(":");
        if (atSignIndex != -1) {
            domain = domain.substring(atSignIndex + 1);
        }
        if (portColon != -1) {
            domain = domain.substring(0, portColon);
        }

        atSignIndex = domain.indexOf("@");
        portColon = domain.indexOf(":");
        if (atSignIndex != -1 || portColon != -1) {
            throw new Exception("Incorrect cryptDomain " + domain);
        }
        this.cryptDomain = domain;
    }

    private String getAskButtonAnswer() {
        if(askButton == null) return "";
        String askResponse = "btn=" + askButton + "\r\n";
        askButton = null;
        return askResponse;
    }

    public String createClientQuery(boolean noiptest, boolean requestServerUnlockKey) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=query\r\n");
        sb.append(getAskButtonAnswer());
        sb.append(storage.getOptions(noiptest, requestServerUnlockKey, false));
        sb.append(storage.getSecretIndex(cryptDomain, lastResponse.get("sin")));
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(cryptDomain)));
        if(storage.hasPreviousKeys()) {
            sb.append("\r\npidk=" + EncryptionUtils.encodeUrlSafe(storage.getPreviousPublicKey(cryptDomain)));
        }
        return sb.toString();
    }

    public String createClientDisable(boolean clientProvidedSession) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=disable\r\n");
        sb.append(getAskButtonAnswer());
        sb.append(storage.getOptions(false, false, clientProvidedSession));
        sb.append(storage.getSecretIndex(cryptDomain, lastResponse.get("sin")));
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(cryptDomain)));
        if(storage.hasPreviousKeys()) {
            sb.append("\r\npidk=" + EncryptionUtils.encodeUrlSafe(storage.getPreviousPublicKey(cryptDomain)));
        }
        return sb.toString();
    }

    public String createClientEnable(boolean clientProvidedSession) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=enable\r\n");
        sb.append(getAskButtonAnswer());
        sb.append(storage.getOptions(false, false, clientProvidedSession));
        sb.append(storage.getSecretIndex(cryptDomain, lastResponse.get("sin")));
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(cryptDomain)));
        if(storage.hasPreviousKeys()) {
            sb.append("\r\npidk=" + EncryptionUtils.encodeUrlSafe(storage.getPreviousPublicKey(cryptDomain)));
        }
        return sb.toString();
    }

    public String createClientRemove(boolean clientProvidedSession) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=remove\r\n");
        sb.append(getAskButtonAnswer());
        sb.append(storage.getOptions(false, false, clientProvidedSession));
        sb.append(storage.getSecretIndex(cryptDomain, lastResponse.get("sin")));
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(cryptDomain)));
        if(storage.hasPreviousKeys()) {
            sb.append("\r\npidk=" + EncryptionUtils.encodeUrlSafe(storage.getPreviousPublicKey(cryptDomain)));
        }
        return sb.toString();
    }


    public String createClientCreateAccount(EntropyHarvester entropyHarvester, boolean clientProvidedSession) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=ident\r\n");
        sb.append(getAskButtonAnswer());
        sb.append(storage.getOptions(false, false, clientProvidedSession));
        sb.append(storage.getSecretIndex(cryptDomain, lastResponse.get("sin")));
        sb.append(storage.getServerUnlockKey(entropyHarvester));
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(cryptDomain)));
        if(storage.hasPreviousKeys()) {
            sb.append("\r\npidk=" + EncryptionUtils.encodeUrlSafe(storage.getPreviousPublicKey(cryptDomain)));
        }
        return sb.toString();
    }

    public String createClientLogin(boolean clientProvidedSession) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=ident\r\n");
        sb.append(getAskButtonAnswer());
        sb.append(storage.getOptions(false, false, clientProvidedSession));
        sb.append(storage.getSecretIndex(cryptDomain, lastResponse.get("sin")));
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(cryptDomain)));
        if(storage.hasPreviousKeys()) {
            sb.append("\r\npidk=" + EncryptionUtils.encodeUrlSafe(storage.getPreviousPublicKey(cryptDomain)));
        }
        return sb.toString();
    }

    public String createPostParams(String client, String server) throws Exception {
        return createPostParams(client, server, false);
    }

    public String createPostParams(String client, String server, boolean unlockServerKey) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressState(R.string.progress_state_prepare_query);

        StringBuilder sb = new StringBuilder();
        sb.append("client=");
        sb.append(EncryptionUtils.encodeUrlSafe(client.getBytes()));

        sb.append("&server=");
        sb.append(EncryptionUtils.encodeUrlSafe(server.getBytes()));

        byte[] message = EncryptionUtils.combine(
                EncryptionUtils.encodeUrlSafe(client.getBytes()).getBytes(),
                EncryptionUtils.encodeUrlSafe(server.getBytes()).getBytes()
                );

        byte[] signed_message = new byte[Sodium.crypto_sign_bytes() + message.length];
        int[] signed_message_len = new int[1];

        Sodium.crypto_sign(
                signed_message,
                signed_message_len,
                message,
                message.length,
                storage.getPrivateKey(cryptDomain)
        );
        sb.append("&ids=");
        sb.append(EncryptionUtils.encodeUrlSafe(Arrays.copyOfRange(signed_message, 0, Sodium.crypto_sign_bytes())));

        if(storage.hasPreviousKeys()) {
            Sodium.crypto_sign(
                    signed_message,
                    signed_message_len,
                    message,
                    message.length,
                    storage.getPreviousPrivateKey(cryptDomain)
            );
            sb.append("&pids=");
            sb.append(EncryptionUtils.encodeUrlSafe(Arrays.copyOfRange(signed_message, 0, Sodium.crypto_sign_bytes())));
        }

        if(unlockServerKey) {
            Sodium.crypto_sign(
                    signed_message,
                    signed_message_len,
                    message,
                    message.length,
                    storage.getUnlockRequestSigningKey(getServerUnlockKey(), this.isPreviousKeyValid())
            );
            sb.append("&urs=");
            sb.append(EncryptionUtils.encodeUrlSafe(Arrays.copyOfRange(signed_message, 0, Sodium.crypto_sign_bytes())));
        }
        return sb.toString();
    }

    public void postRequest(String link, String data) throws Exception {
        StringBuilder result = new StringBuilder();

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressState(R.string.progresstate_contact_server);

        String loginURL = (useSSL ? "https://" : "http://") + communicationDomain + link;

        HttpURLConnection con = null;
        DataOutputStream output = null;
        DataInputStream input = null;
        try {
            URL myurl = new URL(loginURL);
            con = (HttpURLConnection) myurl.openConnection();
            con.setRequestMethod("POST");

            con.setRequestProperty("Content-Length", String.valueOf(data.length()));
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setDoOutput(true);
            con.setDoInput(true);

            output = new DataOutputStream(con.getOutputStream());
            output.writeBytes(data);
            output.close();

            if(con.getResponseCode() != 200) {
                throw new Exception("CONN_ERROR");
            }

            input = new DataInputStream(con.getInputStream());

            String newLine = System.getProperty("line.separator");
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            boolean flag = false;
            while ((line = reader.readLine()) != null) {
                result.append(flag ? newLine : "").append(line);
                flag = true;
            }
            input.close();

            setResponseData(result.toString());

            if(!lastResponse.containsKey("tif")) {
                throw new Exception("CONN_ERROR");
            }

        } catch (Exception e) {
            throw e;
        } finally {
            closeQuietly(output);
            closeQuietly(input);
            if(con != null) con.disconnect();
        }
    }

    private void closeQuietly(Closeable c) {
        try {
            if(c != null) {
                c.close();
            }
        } catch (IOException ioe) {}
    }

    public static void debugPostData(String data) throws Exception{
        String[] variables = data.split("&");
        for(String s : variables) {
            Log.d(TAG,s);
            byte[] bytes = EncryptionUtils.decodeUrlSafe(s.split("=")[1]);
            Log.d(TAG,Arrays.toString(bytes));
            Log.d(TAG,new String(bytes));
        }
    }


    private void setResponseData(String responseData) throws Exception {
        this.response = new String(EncryptionUtils.decodeUrlSafe(responseData));
        for(String param : response.split("\r\n")) {
            int firstEqualSign = param.indexOf("=");
            if(firstEqualSign == -1) continue;
            lastResponse.put(param.substring(0, firstEqualSign), param.substring(firstEqualSign+1));
        }
    }

    public void printParams() {
        for(Map.Entry<String, String> entry : lastResponse.entrySet()) {
            Log.i(TAG, entry.getKey() + "=" + entry.getValue());
        }
    }

    public boolean isIdentityKnown(boolean disabled) {
        return (this.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                this.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                this.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED) == disabled;
    }

    public String getResponse() {
        return response;
    }

    public boolean isTIFBitSet(int k) {
        if(!lastResponse.containsKey("tif")) return false;
        int tif = Integer.parseInt(lastResponse.get("tif"), 16);
        return (tif & 1 << k) != 0;
    }

    public boolean isTIFZero() {
        if(!lastResponse.containsKey("tif")) return false;
        int tif = Integer.parseInt(lastResponse.get("tif"));
        return tif == 0;
    }

    public boolean hasErrorMessage(boolean shouldUseCPSServer) {
        return lastResponse.containsKey("tif") &&
            (
                shouldUseCPSServer &&
                !isTIFBitSet(CommunicationHandler.TIF_IP_MATCHED)
            ) ||
            (
                isTIFBitSet(CommunicationHandler.TIF_BAD_ID_ASSOCIATION) ||
                isTIFBitSet(CommunicationHandler.TIF_CLIENT_FAILURE) ||
                //isTIFBitSet(CommunicationHandler.TIF_COMMAND_FAILED) ||
                isTIFBitSet(CommunicationHandler.TIF_FUNCTION_NOT_SUPPORTED) ||
                isTIFBitSet(CommunicationHandler.TIF_TRANSIENT_ERROR)
            );
    }

    public String getErrorMessage(Activity a, boolean shouldUseCPSServer) {
        StringBuilder sb = new StringBuilder();
        if(!lastResponse.containsKey("tif")) {
            return a.getString(R.string.communication_incorrect_response);
        }

        if(shouldUseCPSServer && !isTIFBitSet(CommunicationHandler.TIF_IP_MATCHED)) {
            sb.append(a.getString(R.string.communication_ip_mismatch));
            sb.append("\n\n");
        }

        if(isTIFBitSet(CommunicationHandler.TIF_BAD_ID_ASSOCIATION)) {
            sb.append(a.getString(R.string.communication_bad_id_association));
            sb.append("\n\n");
        }

        if(isTIFBitSet(CommunicationHandler.TIF_CLIENT_FAILURE)) {
            sb.append(a.getString(R.string.communication_client_failure));
            sb.append("\n\n");
        }
/*
        if(isTIFBitSet(CommunicationHandler.TIF_COMMAND_FAILED)) {
            sb.append(a.getString(R.string.communication_command_failed));
            sb.append("\n\n");
        }
*/
        if(isTIFBitSet(CommunicationHandler.TIF_FUNCTION_NOT_SUPPORTED)) {
            sb.append(a.getString(R.string.communication_function_not_supported));
            sb.append("\n\n");
        }

        if(isTIFBitSet(CommunicationHandler.TIF_TRANSIENT_ERROR)) {
            sb.append(a.getString(R.string.communication_transient_error));
            sb.append("\n\n");
        }
        return sb.toString();
    }

    public boolean hasStateChangeMessage() {
        return lastResponse.containsKey("tif") &&
            isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED);
    }

    public String getStageChangeMessage(Activity a) {
        StringBuilder sb = new StringBuilder();
        if(!lastResponse.containsKey("tif")) {
            return a.getString(R.string.communication_incorrect_response);
        }

        if(isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)) {
            sb.append(a.getString(R.string.communication_sqrl_disabled));
            sb.append("\n\n");
        }

        return sb.toString();
    }


    public static void main(String[] args) {

        try {

            byte[] bytesArray = EncryptionUtils.hex2Byte("7371726c646174617d0001002d00b51fd99559b887d106a8d877c70133bb20a12fa1a7c829b194db94f309c5000000f30104050f000d174cc6e7b70baa158aa4ce75e2f2b99a02a40e4beb2e5d16c2f03442bd3e932035419a63885a663125a600e5486c42b38f708c1094ced1ab0b0050137f6df449caf78581fec678408a804caf74f91c490002005528fc85e3e36866a85574146fe7776d09cf0000004a4e12277dd48366fc1f335dd37188bbcba02bc32a12aef0188f5e83593665518483d638b80051c2b4b013491eb06835");

            SQRLStorage storage = SQRLStorage.getInstance();
            storage.createProgressionUpdater(null, null, null, null);
            storage.read(bytesArray);
            storage.decryptIdentityKey("Testing1234", EntropyHarvester.getInstance(), false);
            boolean didIt = storage.decryptUnlockKey("7276-0587-2230-1119-8559-3839");
            System.out.println(didIt);

            CommunicationHandler commHandler = CommunicationHandler.getInstance();
            String sqrlLink = "sqrl://www.grc.com/sqrl?nut=Na2MOglf7NyyupQ8-dtj1g";

            Matcher sqrlMatcher = CommunicationHandler.sqrlPattern.matcher(sqrlLink);

            final String domain = sqrlMatcher.group(1);
            String queryLink = sqrlMatcher.group(2);

            commHandler.setDomain(domain);
            String postData = commHandler.createPostParams(commHandler.createClientQuery(true, true), sqrlLink);
            commHandler.postRequest(queryLink, postData);

            String serverData = commHandler.getResponse();
            queryLink = commHandler.getQueryLink();

            if(
                (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                        commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                        !commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                ) {
                String postData2 = commHandler.createPostParams(commHandler.createClientDisable(false), serverData);
                commHandler.postRequest(queryLink, postData2);

                serverData = commHandler.getResponse();
                queryLink = commHandler.getQueryLink();

                String postData3 = commHandler.createPostParams(commHandler.createClientRemove(false), serverData, true);
                commHandler.postRequest(queryLink, postData3);

            } else {
                String postData2 = commHandler.createPostParams(commHandler.createClientEnable(false), serverData, true);
                commHandler.postRequest(queryLink, postData2);
            }
            commHandler.printParams();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getQueryLink() {
        if(!lastResponse.containsKey("qry")) {
            return "";
        }
        return lastResponse.get("qry");
    }

    public byte[] getServerUnlockKey() throws Exception{
        if(!lastResponse.containsKey("suk")) {
            return new byte[32];
        }
        return EncryptionUtils.decodeUrlSafe(lastResponse.get("suk"));
    }

    public void setAskButton(String askButton) {
        this.askButton = askButton;
        this.askDialogService.activateAskButton();
    }

    public void setAskDialogService(AskDialogService askDialogService) {
        this.askDialogService = askDialogService;
    }

    public void setAskAction(Runnable askAction) {
        this.askDialogService.setAskAction(askAction);
    }

    public boolean hasAskQuestion() {
        return this.lastResponse.containsKey("ask");
    }

    public void showAskDialog() {
        if(hasAskQuestion()) {
            this.askDialogService.showDialog(this.lastResponse.get("ask"));
        } else {
            this.askDialogService.activateAskButton();
        }
    }

    public boolean isPreviousKeyValid() {
        return isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH);
    }

    public boolean hasCPSUrl() {
        return this.lastResponse.containsKey("url");
    }

    public String getCPSUrl() {
        return this.lastResponse.get("url");
    }

    public String getDomain() {
        return this.cryptDomain;
    }
}
