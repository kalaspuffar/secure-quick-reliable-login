package org.ea.sqrl.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

/**
 *
 * @author Daniel Persson
 */
public class MyBitSet {
    boolean[] bits;

    private MyBitSet(boolean[] bits) {
        this.bits = bits;
    }

    public MyBitSet(byte[] bytes) {
        bits = new boolean[bytes.length * 8];

        int bitCount = 0;
        for(byte b : bytes) {
            for(int i = 0; i < 8; i++) {
                bits[bitCount] = getBit(b, i);
                bitCount++;
            }
        }
    }

    public MyBitSet get(int from, int to) {
        return new MyBitSet(Arrays.copyOfRange(bits, from, to));
    }

    public boolean getBit(byte b, int position) {
        return ((b >> position) & 1) == 1;
    }

    public int toInt() {
        int val = 0;
        int index = 0;
        for(boolean b : bits) {
            if(index > 32) break;
            if(index > 0) val = val << 1;
            val = val | (b ? 1 : 0);
            index++;
        }
        return val;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(boolean b : bits) {
            sb.append(b ? 1 : 0);
        }
        return sb.toString();
    }

    private ServerSocket server;
    public void startCPSServer() {
        new Thread(() -> {
            try {
                server = new ServerSocket(25519);

                System.out.println("Started CPS server");

                while (true) {
                    Socket socket = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String line = in.readLine();
                    System.out.println(line);

                    if(line.contains("gif HTTP/1.1")) {
                        System.out.println("Respond");
                        byte[] content = Base64.getDecoder().decode(
                                "R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=="
                        );
                        OutputStream os = socket.getOutputStream();
                        StringBuilder out = new StringBuilder();
                        out.append("HTTP/1.0 200 OK\r\n");
                        out.append("Content-Type: image/gif\r\n");
                        out.append("Content-Length: " + content.length + "\r\n\r\n");
                        System.out.println(out.toString());
                        os.write(out.toString().getBytes("UTF-8"));
                        os.write(content);
                        os.close();
                    }

                    in.close();
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        MyBitSet m = new MyBitSet(new byte[] {});
        m.startCPSServer();
    }

}
