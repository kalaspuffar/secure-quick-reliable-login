package org.ea.sqrl.processors;

import android.app.Activity;
import android.util.Log;

import org.ea.sqrl.R;
import org.ea.sqrl.utils.EncryptionUtils;
import org.libsodium.jni.Sodium;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * This handler creates different queries to servers and parses the response so we can talk over
 * the SQRL protocol seamlessly.
 *
 * @author Daniel Persson
 */
public class CommunicationHandler {
    private static final String TAG = "CommunicationHandler";

    private static CommunicationHandler instance = null;
    private String domain;
    private Map<String, String> lastResponse = new HashMap<>();
    private String response;

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

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String createClientQuery() throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=query\r\n");
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(domain)));
        return sb.toString();
    }

    public String createClientDisable() throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=disable\r\n");
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(domain)));
        return sb.toString();
    }

    public String createClientEnable() throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=enable\r\n");
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(domain)));
        return sb.toString();
    }

    public String createClientRemove() throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=remove\r\n");
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(domain)));
        return sb.toString();
    }


    public String createClientCreateAccount(EntropyHarvester entropyHarvester) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=ident\r\n");
        sb.append(storage.getServerUnlockKey(entropyHarvester));
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(domain)));
        return sb.toString();
    }

    public String createClientLogin() throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=ident\r\n");
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(storage.getPublicKey(domain)));
        return sb.toString();
    }

    public String createPostParams(String client, String server) throws Exception {
        return createPostParams(client, server, false);
    }

    public String createPostParams(String client, String server, boolean unlockServerKey) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("client=");
        sb.append(EncryptionUtils.encodeUrlSafe(client.getBytes()));

        sb.append("&server=");
        sb.append(EncryptionUtils.encodeUrlSafe(server.getBytes()));

        SQRLStorage storage = SQRLStorage.getInstance();
        byte[] message = EncryptionUtils.combine(
                EncryptionUtils.encodeUrlSafe(client.getBytes()).getBytes(),
                EncryptionUtils.encodeUrlSafe(server.getBytes()).getBytes()
                );

        byte[] signed_message = new byte[Sodium.crypto_sign_bytes() + message.length];
        int[] signed_message_len = new int[1];

        if(unlockServerKey) {
            Sodium.crypto_sign(
                    signed_message,
                    signed_message_len,
                    message,
                    message.length,
                    storage.getUnlockRequestSigningKey(getServerUnlockKey())
            );
            sb.append("&urs=");
            sb.append(EncryptionUtils.encodeUrlSafe(Arrays.copyOfRange(signed_message, 0, Sodium.crypto_sign_bytes())));
        }

        Sodium.crypto_sign(
                signed_message,
                signed_message_len,
                message,
                message.length,
                storage.getPrivateKey(domain)
        );
        sb.append("&ids=");
        sb.append(EncryptionUtils.encodeUrlSafe(Arrays.copyOfRange(signed_message, 0, Sodium.crypto_sign_bytes())));

        return sb.toString();
    }

    public void postRequest(String link, String data) throws Exception {
        StringBuilder result = new StringBuilder();

        String httpsURL = "https://" + domain + link;

        URL myurl = new URL(httpsURL);
        HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
        con.setRequestMethod("POST");

        con.setRequestProperty("Content-Length", String.valueOf(data.length()));
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        con.setDoInput(true);

        DataOutputStream output = new DataOutputStream(con.getOutputStream());
        output.writeBytes(data);
        output.close();

        DataInputStream input = new DataInputStream(con.getInputStream());

        String newLine = System.getProperty("line.separator");
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;
        boolean flag = false;
        while ((line = reader.readLine()) != null) {
            result.append(flag ? newLine : "").append(line);
            flag = true;
        }
        input.close();

        //Log.d(TAG, "Resp Code:" + con.getResponseCode());

        setResponseData(result.toString());
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
            Log.d(TAG, entry.getKey() + "=" + entry.getValue());
        }
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

    public boolean hasErrorMessage() {
        return !lastResponse.containsKey("tif") ||
            isTIFBitSet(CommunicationHandler.TIF_BAD_ID_ASSOCIATION) ||
            isTIFBitSet(CommunicationHandler.TIF_CLIENT_FAILURE) ||
            isTIFBitSet(CommunicationHandler.TIF_COMMAND_FAILED) ||
            isTIFBitSet(CommunicationHandler.TIF_FUNCTION_NOT_SUPPORTED) ||
            isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED) ||
            isTIFBitSet(CommunicationHandler.TIF_TRANSIENT_ERROR);
    }

    public String getErrorMessage(Activity a) {
        StringBuilder sb = new StringBuilder();
        if(!lastResponse.containsKey("tif")) {
            return a.getString(R.string.communication_incorrect_response);
        }

        if(isTIFBitSet(CommunicationHandler.TIF_BAD_ID_ASSOCIATION)) {
            sb.append(a.getString(R.string.communication_bad_id_association));
            sb.append("\n\n");
        }

        if(isTIFBitSet(CommunicationHandler.TIF_CLIENT_FAILURE)) {
            sb.append(a.getString(R.string.communication_client_failure));
            sb.append("\n\n");
        }

        if(isTIFBitSet(CommunicationHandler.TIF_COMMAND_FAILED)) {
            sb.append(a.getString(R.string.communication_command_failed));
            sb.append("\n\n");
        }

        if(isTIFBitSet(CommunicationHandler.TIF_FUNCTION_NOT_SUPPORTED)) {
            sb.append(a.getString(R.string.communication_function_not_supported));
            sb.append("\n\n");
        }

        if(isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)) {
            sb.append(a.getString(R.string.communication_sqrl_disabled));
            sb.append("\n\n");
        }

        if(isTIFBitSet(CommunicationHandler.TIF_TRANSIENT_ERROR)) {
            sb.append(a.getString(R.string.communication_transient_error));
            sb.append("\n\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {

        try {

            byte[] bytesArray = EncryptionUtils.hex2Byte("7371726c646174617d0001002d00b51fd99559b887d106a8d877c70133bb20a12fa1a7c829b194db94f309c5000000f30104050f000d174cc6e7b70baa158aa4ce75e2f2b99a02a40e4beb2e5d16c2f03442bd3e932035419a63885a663125a600e5486c42b38f708c1094ced1ab0b0050137f6df449caf78581fec678408a804caf74f91c490002005528fc85e3e36866a85574146fe7776d09cf0000004a4e12277dd48366fc1f335dd37188bbcba02bc32a12aef0188f5e83593665518483d638b80051c2b4b013491eb06835");

            SQRLStorage storage = SQRLStorage.getInstance();
            storage.setProgressionUpdater(new ProgressionUpdater());
            storage.read(bytesArray);
            storage.decryptIdentityKey("Testing1234");
            boolean didIt = storage.decryptUnlockKey("7276-0587-2230-1119-8559-3839");
            System.out.println(didIt);

            CommunicationHandler commHandler = CommunicationHandler.getInstance();
            String sqrlLink = "sqrl://www.grc.com/sqrl?nut=Na2MOglf7NyyupQ8-dtj1g";
            String domain = sqrlLink.split("/")[2];

            int indexOfQuery = sqrlLink.indexOf("/", sqrlLink.indexOf("://")+3);
            String queryLink = sqrlLink.substring(indexOfQuery);

            commHandler.setDomain(domain);
            String postData = commHandler.createPostParams(commHandler.createClientQuery(), sqrlLink);
            commHandler.postRequest(queryLink, postData);

            String serverData = commHandler.getResponse();
            queryLink = commHandler.getQueryLink();

            if(
                (commHandler.isTIFBitSet(CommunicationHandler.TIF_CURRENT_ID_MATCH) ||
                        commHandler.isTIFBitSet(CommunicationHandler.TIF_PREVIOUS_ID_MATCH)) &&
                        !commHandler.isTIFBitSet(CommunicationHandler.TIF_SQRL_DISABLED)
                ) {
                String postData2 = commHandler.createPostParams(commHandler.createClientDisable(), serverData);
                commHandler.postRequest(queryLink, postData2);

                serverData = commHandler.getResponse();
                queryLink = commHandler.getQueryLink();

                String postData3 = commHandler.createPostParams(commHandler.createClientRemove(), serverData, true);
                commHandler.postRequest(queryLink, postData3);

            } else {
                String postData2 = commHandler.createPostParams(commHandler.createClientEnable(), serverData, true);
                commHandler.postRequest(queryLink, postData2);
            }
            commHandler.printParams();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        try {
            File file = new File("Testing2.sqrl");
            byte[] bytesArray = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray);
            fis.close();

            System.out.println(EncryptionUtils.byte2hex(bytesArray));
            if(true) System.exit(0);


            SQRLStorage storage = SQRLStorage.getInstance();
            storage.setProgressionUpdater(new ProgressionUpdater());
            storage.read(bytesArray);
            storage.decryptIdentityKey("Testing1234");

            CommunicationHandler commHandler = CommunicationHandler.getInstance();
            String sqrlLink = "sqrl://www.grc.com/sqrl?nut=WJ1LHhkhd-8O6oMiO1RuQw";
            String domain = sqrlLink.split("/")[2];

            int indexOfQuery = sqrlLink.indexOf("/", sqrlLink.indexOf("://")+3);
            String queryLink = sqrlLink.substring(indexOfQuery);

            commHandler.setDomain(domain);
            String postData = commHandler.createPostParams(commHandler.createClientQuery(), sqrlLink);
            commHandler.postRequest(queryLink, postData);
            commHandler.printParams();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
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
}
