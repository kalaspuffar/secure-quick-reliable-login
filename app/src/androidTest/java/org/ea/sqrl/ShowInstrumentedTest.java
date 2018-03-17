package org.ea.sqrl;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.ea.sqrl.activites.SettingsActivity;
import org.ea.sqrl.activites.ShowIdentityActivity;
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
public class ShowInstrumentedTest {
    @Rule
    public ActivityTestRule<ShowIdentityActivity> activityRule =
            new ActivityTestRule<>(ShowIdentityActivity.class);
/*
    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }
*/
    @Test
    public void testAccessability() throws Exception {
        activityRule.getActivity();
        onView(withId(R.id.txtIdentityText)).perform(click());
    }
}
