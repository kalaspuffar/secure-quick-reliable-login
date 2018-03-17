package org.ea.sqrl;

import android.support.test.espresso.accessibility.AccessibilityChecks;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.ea.sqrl.activites.AdvancedActivity;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Daniel Persson
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdvancedInstrumentedTest {
    @Rule
    public ActivityTestRule<AdvancedActivity> activityRule =
            new ActivityTestRule<>(AdvancedActivity.class);

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @Test
    public void testAccessability() throws Exception {
        activityRule.getActivity();
//        onView(withId(R.id.imageButton)).perform(click());
    }
}
