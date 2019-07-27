package org.ea.sqrl;

import org.ea.sqrl.processors.CommunicationHandler;
import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.*;

/**
 * This testcase will check that all rules of a SQRL URL are handled correctly.
 */
public class SQRLURLUnitTest {

    private String testMatch(String url) throws Exception {
        CommunicationHandler communicationHandler = CommunicationHandler.getInstance(null);
        Matcher mSqrlMatcher = CommunicationHandler.sqrlPattern.matcher(url);
        if(!mSqrlMatcher.matches()) {
            throw(new Exception("Incorrect URL"));
        }

        communicationHandler.setDomain(mSqrlMatcher.group(1), mSqrlMatcher.group(2));

        return new String(communicationHandler.getDomain());
    }

    @Test
    public void domainLowercased() throws Exception {
        String cryptDomain = testMatch("sqrl://ExAmPlE.cOm/?nut=oOB4QOFJux5Z");
        assertEquals("example.com", cryptDomain);
    }

    @Test
    public void ignorePort() throws Exception {
        String cryptDomain = testMatch("sqrl://example.com:44344/?nut=oOB4QOFJux5Z");
        assertEquals("example.com", cryptDomain);
    }

    @Test
    public void ignoreUsername() throws Exception {
        String cryptDomain = testMatch("sqrl://jonny@example.com/?nut=oOB4QOFJux5Z");
        assertEquals("example.com", cryptDomain);
    }

    @Test
    public void ignoreUsernameAndPassword() throws Exception {
        String cryptDomain = testMatch("sqrl://Jonny:Secret@example.com/?nut=oOB4QOFJux5Z");
        assertEquals("example.com", cryptDomain);
    }

    @Test
    public void extendAuthDomain() throws Exception {
        String cryptDomain = testMatch("sqrl://example.com/jimbo/?x=6&nut=oOB4QOFJux5Z");
        assertEquals("example.com/jimbo", cryptDomain);
    }

    @Test
    public void extensionCaseAndLimitByQuestionMark() throws Exception {
        String cryptDomain = testMatch("sqrl://EXAMPLE.COM/JIMBO?x=16&nut=oOB4QOFJux5Z");
        assertEquals("example.com/JIMBO", cryptDomain);
    }
}