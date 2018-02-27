package org.ea.sqrl.processors;

import android.app.Activity;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;

import org.ea.sqrl.R;
import org.ea.sqrl.utils.EncryptionUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
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
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(storage.getPrivateKey(domain), spec);
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(privKey.getA().toByteArray()));

        return sb.toString();
    }

    public String createClientCreateAccount() throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=ident\r\n");
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(storage.getPrivateKey(domain), spec);
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(privKey.getA().toByteArray()));
        return sb.toString();
    }

    public String createClientLogin() throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=ident\r\n");
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(storage.getPrivateKey(domain), spec);
        sb.append("idk=" + EncryptionUtils.encodeUrlSafe(privKey.getA().toByteArray()));

        return sb.toString();
    }

    public String createPostParams(String client, String server) throws Exception {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(SQRLStorage.getInstance().getPrivateKey(domain), spec);

        StringBuilder sb = new StringBuilder();
        sb.append("client=");
        sb.append(EncryptionUtils.encodeUrlSafe(client.getBytes()));

        sb.append("&server=");
        sb.append(EncryptionUtils.encodeUrlSafe(server.getBytes()));

        PrivateKey sKey = new EdDSAPrivateKey(privKey);
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
        sgr.initSign(sKey);
        sgr.update(EncryptionUtils.encodeUrlSafe(client.getBytes()).getBytes());
        sgr.update(EncryptionUtils.encodeUrlSafe(server.getBytes()).getBytes());
        sb.append("&ids=");
        sb.append(EncryptionUtils.encodeUrlSafe(sgr.sign()));
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

        System.out.println("Resp Code:" + con.getResponseCode());

        setResponseData(result.toString());
    }

    public static void debugPostData(String data) throws Exception{
        String[] variables = data.split("&");
        for(String s : variables) {
            System.out.println(s);
            byte[] bytes = EncryptionUtils.decodeUrlSafe(s.split("=")[1]);
            System.out.println(Arrays.toString(bytes));
            System.out.println(new String(bytes));
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
            System.out.println(entry.getKey() + "=" + entry.getValue());
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
            File file = new File("Testing2.sqrl");
            byte[] bytesArray = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray);
            fis.close();

            SQRLStorage storage = SQRLStorage.getInstance();
            storage.setProgressionUpdater(new ProgressionUpdater());
            storage.read(bytesArray, true);
            storage.decryptIdentityKey("Testing1234");

            CommunicationHandler commHandler = CommunicationHandler.getInstance();
            String sqrlLink = "sqrl://www.grc.com/sqrl?nut=Goq4xz6i70frU7xu1-RDTQ";
            String domain = sqrlLink.split("/")[2];

            int indexOfQuery = sqrlLink.indexOf("/", sqrlLink.indexOf("://")+3);
            String queryLink = sqrlLink.substring(indexOfQuery);

            System.out.println(queryLink);

            commHandler.setDomain(domain);
            String postData = commHandler.createPostParams(commHandler.createClientQuery(), sqrlLink);
            commHandler.postRequest(queryLink, postData);
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
}
