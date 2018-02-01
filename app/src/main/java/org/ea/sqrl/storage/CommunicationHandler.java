package org.ea.sqrl.storage;

import android.os.Build;
import android.util.Base64;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import org.ea.sqrl.ProgressionUpdater;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class CommunicationHandler {

    public static String encodeUrlSafe(byte[] data) throws Exception {
        if(Build.VERSION.BASE_OS != null) {
            return Base64.encodeToString(data, Base64.NO_PADDING + Base64.URL_SAFE + Base64.NO_WRAP);
        } else {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data);
        }
    }

    public static byte[] decodeUrlSafe(String data) throws Exception {
        if(Build.VERSION.BASE_OS != null) {
            return Base64.decode(data, Base64.NO_PADDING + Base64.URL_SAFE + Base64.NO_WRAP);
        } else {
            return java.util.Base64.getUrlDecoder().decode(data);
        }
    }

    public String createClientQueryData(String domain) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=query\r\n");
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(storage.getPrivateKey(domain), spec);
        sb.append("idk=" + encodeUrlSafe(privKey.getA().toByteArray()));

        return sb.toString();
    }

    public String createClientIdentData(String domain) throws Exception {
        SQRLStorage storage = SQRLStorage.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=query\r\n");
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(storage.getPrivateKey(domain), spec);
        sb.append("idk=" + encodeUrlSafe(privKey.getA().toByteArray()));

        return sb.toString();
    }

    public String createPostParams(String client, String server, String domain) throws Exception {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(SQRLStorage.getInstance().getPrivateKey(domain), spec);

        StringBuilder sb = new StringBuilder();
        sb.append("client=");
        sb.append(encodeUrlSafe(client.getBytes()));

        sb.append("&server=");
        sb.append(encodeUrlSafe(server.getBytes()));

        PrivateKey sKey = new EdDSAPrivateKey(privKey);
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
        sgr.initSign(sKey);
        sgr.update(encodeUrlSafe(client.getBytes()).getBytes());
        sgr.update(encodeUrlSafe(server.getBytes()).getBytes());
        sb.append("&ids=");
        sb.append(encodeUrlSafe(sgr.sign()));
        return sb.toString();
    }

    public String postRequest(String link, String data) throws Exception {
        StringBuilder result = new StringBuilder();

        String httpsURL = "https://" + link;
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
        return result.toString();
    }

    public static void debugPostData(String data) throws Exception{
        String[] variables = data.split("&");
        for(String s : variables) {
            System.out.println(s);
            byte[] bytes = decodeUrlSafe(s.split("=")[1]);
            System.out.println(Arrays.toString(bytes));
            System.out.println(new String(bytes));
        }
    }

    public static void main(String[] args) {
        try {
            File file = new File("Testing3.sqrl");
            byte[] bytesArray = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray);
            fis.close();

            SQRLStorage storage = SQRLStorage.getInstance();
            storage.setProgressionUpdater(new ProgressionUpdater());
            storage.read(bytesArray, true);
            storage.decryptIdentityKey("Testing1234");

            CommunicationHandler commHandler = new CommunicationHandler();
            String sqrlLink = "sqrl://www.grc.com/sqrl?nut=Goq4xz6i70frU7xu1-RDTQ";
            String domain = sqrlLink.split("/")[2];
            String serverData = sqrlLink.substring(sqrlLink.indexOf("://")+3);

            String postData = commHandler.createPostParams(commHandler.createClientQueryData(domain), sqrlLink, domain);

            System.out.println(commHandler.postRequest(serverData, postData));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
