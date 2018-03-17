package org.ea.sqrl;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.accessibility.AccessibilityChecks;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExampleInstrumentedTest {
    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable()
                .setRunChecksFromRootView(true);
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("org.ea.sqrl", appContext.getPackageName());
    }
}
