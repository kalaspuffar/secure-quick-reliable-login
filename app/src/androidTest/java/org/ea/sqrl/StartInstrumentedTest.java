package org.ea.sqrl;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.ea.sqrl.activites.ShowIdentityActivity;
import org.ea.sqrl.activites.StartActivity;
import org.junit.BeforeClass;
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
public class StartInstrumentedTest {
    @Rule
    public ActivityTestRule<StartActivity> activityRule =
            new ActivityTestRule<>(StartActivity.class);

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessabilitySingelton.getInstance();
    }

    @Test
    public void testAccessability() throws Exception {
        activityRule.getActivity();
        onView(withId(R.id.btnScanSecret)).perform(click());
    }
}
