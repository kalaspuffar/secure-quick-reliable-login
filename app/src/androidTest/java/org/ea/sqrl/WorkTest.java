package org.ea.sqrl;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.ea.sqrl.processors.CommunicationHandler;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.EncryptionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;

@RunWith(AndroidJUnit4.class)
public class WorkTest {
    private final String TAG = "WorkTest";


    @Test
    public void testCommunication() throws Exception {
        //String rawQRCodeData = "4ce7371726c646174617d0001002d0031d32536a67c4661faef7631d4a7854da64297af4bda01438eca39a40921000000f10104010f0085de0eea7b76134eee4e0b2d638955ad5fd253d857c86177151d30a956182abc55b80316da5c22fcc9b92c4f5a4850f5a4ecb6b948f05b297de13b1a7698cbee461c5e7ef0f8759e659a7ad853555be64900020079d1d5da2d4b046212f43da2f7b0a39709ca000000d5dd50893d516c8175291f7d905b5bf5636d26fee5d3f8801375f7824b09a2a824de7fc41451ca13e610d5591d568db60ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11";
        //byte[] bytesArray = EncryptionUtils.readSQRLQRCode(EncryptionUtils.hex2Byte(rawQRCodeData));
/*
        byte[] bytesArray = EncryptionUtils.hex2Byte("7371726c646174617d0001002d00b51fd99559b887d106a8d877c70133bb20a12fa1a7c829b194db94f309c5000000f30104050f000d174cc6e7b70baa158aa4ce75e2f2b99a02a40e4beb2e5d16c2f03442bd3e932035419a63885a663125a600e5486c42b38f708c1094ced1ab0b0050137f6df449caf78581fec678408a804caf74f91c490002005528fc85e3e36866a85574146fe7776d09cf0000004a4e12277dd48366fc1f335dd37188bbcba02bc32a12aef0188f5e83593665518483d638b80051c2b4b013491eb06835");

        SQRLStorage storage = SQRLStorage.getInstance();
        storage.setProgressionUpdater(new ProgressionUpdater());
        storage.read(bytesArray);
        storage.decryptIdentityKey("Testing1234");
        boolean didIt = storage.decryptUnlockKey("7276-0587-2230-1119-8559-3839");

        Log.d(TAG, "Well I didit it! " + didIt);

        CommunicationHandler commHandler = CommunicationHandler.getInstance();
        String sqrlLink = "sqrl://www.grc.com/sqrl?nut=Vnnsn47mSIcYVTgnAFrCGw";
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
        } else {
            String postData2 = commHandler.createPostParams(commHandler.createClientEnable(), serverData, true);
            commHandler.postRequest(queryLink, postData2);
        }
        commHandler.printParams();
*/
    }

}
