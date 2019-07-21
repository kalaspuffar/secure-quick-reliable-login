package org.ea.sqrl.processors;

import android.util.Log;

import org.ea.sqrl.utils.EncryptionUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a minimalistic "web server" on port 25519 to handle
 * SQRL's "Client Provided Session (CPS)" authentication protection.
 *
 */
public class CPSServer {
    private static final String TAG = "CPSServer";
    private static final int CPS_SERVER_PORT = 25519;

    private static CPSServer mInstance = null;
    private static CommunicationFlowHandler mCommFlowHandler = null;
    private ServerSocket mServerSocket;
    private boolean mSentImage = false;
    private Thread mCpsThread;
    private boolean mCancelCPS = false;

    private CPSServer() {}

    public static CPSServer getInstance(CommunicationFlowHandler communicationFlowHandler) {
        mCommFlowHandler = communicationFlowHandler;

        if(mInstance == null) {
            mInstance = new CPSServer();
        }

        return mInstance;
    }

    public void close() {
        if (mCpsThread != null) mCpsThread.interrupt();

        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setCancelCPS(boolean cancelCPS) {
        mCancelCPS = cancelCPS;
    }

    public boolean start(Runnable doneAction) {
        mCpsThread = new Thread(() -> {

            mSentImage = false;
            boolean done = false;

            try {
                mServerSocket = new ServerSocket(CPS_SERVER_PORT);

                while (!mServerSocket.isClosed() && !done) {
                    Socket socket = mServerSocket.accept();
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
                        mSentImage = true;
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
                        if(mCancelCPS) {
                            if(params.containsKey("can")) {
                                out.append("Location: ").append(params.get("can")).append("\r\n\r\n");
                            } else {
                                out.append("Location: ").append("https://www.google.com").append("\r\n\r\n");
                            }
                        } else {
                            out.append("Location: ").append(mCommFlowHandler.getCommHandler()
                                    .getCPSUrl()).append("\r\n\r\n");
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
            } catch (InterruptedException e) {
                mSentImage = false;
                return;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }

            if(done) {
                mSentImage = false;
                doneAction.run();
            }
        });
        mCpsThread.start();

        waitForCPS(false);

        if(mCpsThread.isAlive() && !mSentImage) {
            return false;
        }
        return true;
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

    public void interruptServerThread() {
        mCpsThread.interrupt();
    }

    public void waitForTransactionDone() {
        while (mCpsThread.isAlive() && !mCommFlowHandler.getCommHandler().hasCPSUrl()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }
    }

    public void waitForCPS(boolean afterConversation) {
        int time = 0;
        while (mCpsThread.isAlive() && time < 10 && (!mSentImage || afterConversation)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
            time++;
        }
    }
}
