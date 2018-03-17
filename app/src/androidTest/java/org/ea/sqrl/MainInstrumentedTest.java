package org.ea.sqrl;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.ea.sqrl.activites.MainActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 *
 * @author Daniel Persson
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainInstrumentedTest {
    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);
/*
    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }
*/
    @Test
    public void testAccessability() throws Exception {
        activityRule.getActivity();
        onView(withId(R.id.btnImportIdentity)).perform(click());
    }
}
