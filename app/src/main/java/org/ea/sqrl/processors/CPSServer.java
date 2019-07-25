package org.ea.sqrl.processors;

import android.content.Context;
import android.util.Log;

import org.ea.sqrl.R;
import org.ea.sqrl.utils.EncryptionUtils;
import org.ea.sqrl.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a minimalistic "web server" on port 25519 to support
 * SQRL's "Client Provided Session (CPS)" authentication protection mechanism.
 *
 */
public class CPSServer {
    private static final String TAG = "CPSServer";
    private static final int CPS_SERVER_PORT = 25519;

    private static CPSServer mInstance = null;
    private static CommunicationFlowHandler mCommFlowHandler = null;
    private static Context mContext;
    private ServerSocket mServerSocket;
    private boolean mSentImage = false;
    private Thread mCpsThread;
    private boolean mCancelCPS = false;

    private CPSServer() {}

    public static CPSServer getInstance(Context context, CommunicationFlowHandler communicationFlowHandler) {
        mContext = context;
        mCommFlowHandler = communicationFlowHandler;

        if(mInstance == null) {
            mInstance = new CPSServer();
        }

        return mInstance;
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

                    String requestLine = in.readLine();
                    Log.i(TAG, requestLine);

                    Map<String, String> headers = parseHeaders(in);
                    if (headers.containsKey("origin")) {
                        sendConnectionAbortedPage(socket);
                        done = true;
                        break;
                    }

                    if(requestLine.contains("gif HTTP/1.1")) {
                        sendDummyGifImage(socket);
                        mSentImage = true;
                    } else {
                        String[] requestTokens = requestLine.split(" ");
                        if (requestTokens.length < 2 || requestTokens[1].length() < 2) break;
                        String data = requestTokens[1].substring(1);
                        Map<String, String> params = getQueryParams(data);

                        waitForTransactionDone();

                        if (mCancelCPS) {
                            if (params.containsKey("can")) {
                                send302Redirect(socket, params.get("can"));
                            } else {
                                sendConnectionAbortedPage(socket);
                            }
                        } else {
                            send302Redirect(socket, mCommFlowHandler.getCommHandler().getCPSUrl());
                        }
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

    public void setCancelCPS(boolean cancelCPS) {
        mCancelCPS = cancelCPS;
    }

    private Map<String, String> parseHeaders(BufferedReader reader) {
        Map<String, String> headers = new HashMap<>();
        String line;
        int idx;

        try {
            line = reader.readLine();
            while (line != null && !line.equals("")) {
                idx = line.indexOf(':');
                if (idx < 0) {
                    break;
                }
                else {
                    headers.put(line.substring(0, idx).toLowerCase(), line.substring(idx+1).trim());
                }
                line = reader.readLine();
            }
        } catch (IOException e) { }

        return headers;
    }

    private void sendDummyGifImage(Socket socket) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendConnectionAbortedPage(Socket socket) {
        try {
            byte[] htmlBytes = Utils.getAssetContent(mContext, "cps_cancelled.html");
            String html = new String(htmlBytes);
            html = html.replace("{{0}}", mContext.getResources().getString(R.string.cps_auth_aborted_headline));
            html = html.replace("{{1}}", mContext.getResources().getString(R.string.cps_auth_aborted_description));
            html = html.replace("{{2}}", mContext.getResources().getString(R.string.cps_auth_aborted_go_back_now));
            htmlBytes = html.getBytes();
            OutputStream os = socket.getOutputStream();
            StringBuilder out = new StringBuilder();

            out.append("HTTP/1.0 200 OK\r\n");
            out.append("Content-Type: text/html\r\n");
            out.append("Content-Length: ").append(htmlBytes.length).append("\r\n\r\n");
            Log.i(TAG, out.toString());
            os.write(out.toString().getBytes("UTF-8"));
            os.write(htmlBytes);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send302Redirect(Socket socket, String redirectUrl) {
        try {
            OutputStream os = socket.getOutputStream();
            StringBuilder out = new StringBuilder();

            out.append("HTTP/1.0 302 Found\r\n");
            out.append("Location: ").append(redirectUrl).append("\r\n\r\n");
            Log.i(TAG, out.toString());
            os.write(out.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
