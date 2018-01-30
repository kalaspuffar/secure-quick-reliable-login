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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

public class CommunicationHandler {

    public static String encodeUrlSafe(byte[] data) throws Exception {
        if(Build.VERSION.BASE_OS != null) {
            return Base64.encodeToString(data, Base64.CRLF + Base64.NO_PADDING + Base64.URL_SAFE);
        } else {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data);
        }
    }

    public static byte[] decodeUrlSafe(String data) throws Exception {
        if(Build.VERSION.BASE_OS != null) {
            return Base64.decode(data, Base64.CRLF + Base64.NO_PADDING + Base64.URL_SAFE);
        } else {
            return java.util.Base64.getUrlDecoder().decode(data);
        }
    }

    public byte[] getPrivateKey(byte[] masterKey, String domain) throws Exception {
        final Mac HMacSha256 = Mac.getInstance("HmacSHA256");
        final SecretKeySpec key = new SecretKeySpec(masterKey, "HmacSHA256");
        HMacSha256.init(key);
        return HMacSha256.doFinal(domain.getBytes());
    }

    public String createClientQueryData() {
        StringBuilder sb = new StringBuilder();
        sb.append("ver=1\r\n");
        sb.append("cmd=ident\r\n");
        return sb.toString();
    }

    public String createPostParams(String client, String server, byte[] privateKey) throws Exception {
        StringBuilder sb = new StringBuilder();
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(privateKey, spec);
        client += "idk=" + encodeUrlSafe(privKey.getA().toByteArray());

        sb.append("client=");
        sb.append(encodeUrlSafe(client.getBytes()));

        sb.append("&server=");
        sb.append(encodeUrlSafe(server.getBytes()));

        sb.append("&idk=");
        sb.append(encodeUrlSafe(privKey.getA().toByteArray()));

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
        String httpsURL = "https://" + link;

        URL myurl = new URL(httpsURL);
        HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
        con.setRequestMethod("POST");

        con.setRequestProperty("Content-length", String.valueOf(data.length()));
        con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        //con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)");
        con.setDoOutput(true);
        con.setDoInput(true);

        DataOutputStream output = new DataOutputStream(con.getOutputStream());
        output.writeBytes(data);
        output.close();



        DataInputStream input = new DataInputStream( con.getInputStream() );

        String newLine = System.getProperty("line.separator");
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder result = new StringBuilder();
        String line; boolean flag = false;
        while ((line = reader.readLine()) != null) {
            result.append(flag ? newLine : "").append(line);
            flag = true;
        }

        /*
        for( int c = input.read(); c != -1; c = input.read() )
            System.out.print( (char)c );
        */
        input.close();

        System.out.println("Resp Code:"+con.getResponseCode());
        return result.toString();
    }

    private static void debugPostData(String data) throws Exception{
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
            File file = new File("Testing2.sqrl");
            byte[] bytesArray = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray);
            fis.close();

            SQRLStorage storage = SQRLStorage.getInstance();
            storage.setProgressionUpdater(new ProgressionUpdater());
            storage.read(bytesArray, true);
            storage.decryptIdentityKey("Testing1234");

            CommunicationHandler commHandler = new CommunicationHandler();
            String sqrlLink = "sqrl://www.grc.com/sqrl?nut=zXPUSRX-0DcmezCtdb-4-Q";
            String domain = sqrlLink.split("/")[2];
            String serverData = sqrlLink.substring(sqrlLink.indexOf("://")+3);

            byte[] privateKey = commHandler.getPrivateKey(storage.getMasterKey(), domain);
            String postData = commHandler.createPostParams(commHandler.createClientQueryData(), sqrlLink, privateKey);

            //System.out.println(postData);
            //debugPostData(postData);

            System.out.println(commHandler.postRequest(serverData, postData));


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
