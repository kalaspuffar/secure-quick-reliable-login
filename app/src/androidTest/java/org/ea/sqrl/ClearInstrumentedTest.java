package org.ea.sqrl;

import android.support.test.espresso.accessibility.AccessibilityChecks;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import org.ea.sqrl.activites.AdvancedActivity;
import org.ea.sqrl.activites.ClearIdentityActivity;
import org.hamcrest.Matcher;
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
public class ClearInstrumentedTest {
    @Rule
    public ActivityTestRule<ClearIdentityActivity> activityRule =
            new ActivityTestRule<>(ClearIdentityActivity.class);

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessabilitySingelton.getInstance();
    }

    @Test
    public void testAccessability() throws Exception {
        activityRule.getActivity();
        onView(withId(R.id.txtClearIdentity)).perform(click());
    }
}
